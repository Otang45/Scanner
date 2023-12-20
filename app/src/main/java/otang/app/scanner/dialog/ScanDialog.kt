package otang.app.scanner.dialog

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageFormat
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.Result
import com.google.zxing.ResultPoint
import com.google.zxing.ResultPointCallback
import com.google.zxing.common.HybridBinarizer
import otang.app.scanner.databinding.ScanDialogBinding
import otang.app.scanner.dialog.base.BaseBottomSheetDialogFragment
import otang.app.scanner.util.AppUtils
import java.util.EnumMap
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class ScanDialog(val callback: (result: Result, bitmap: Bitmap) -> Unit) :
    BaseBottomSheetDialogFragment<ScanDialogBinding>() {
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null
    private val fromatReader: MultiFormatReader = MultiFormatReader()
    private val isScaning = AtomicBoolean(false)
    private lateinit var hints: MutableMap<DecodeHintType, Any>

    override fun onViewCreated() {
        val params = binding.root.layoutParams
        params.height = AppUtils.getScreenHeight(requireActivity()) / 5 * 3
        binding.root.layoutParams = params
        hints = EnumMap(DecodeHintType::class.java)
        hints[DecodeHintType.POSSIBLE_FORMATS] = AppUtils.getDefaultFormat()
        hints[DecodeHintType.TRY_HARDER] = true
        hints[DecodeHintType.CHARACTER_SET] = "UTF-8"
        hints[DecodeHintType.NEED_RESULT_POINT_CALLBACK] = PoinCallback()
        fromatReader.setHints(hints)
        initCamera()
    }

    private fun initCamera() {
        if (isPermissionGranted(requireActivity())) {
            openCamera()
        } else {
            requestPermission(requireActivity())
            dismiss()
        }
    }

    private fun openCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireActivity())
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                cameraProvider?.let {
                    bindPreview(it)
                }
            } catch (ee: ExecutionException) {
                ee.printStackTrace()
            } catch (ie: InterruptedException) {
                ie.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(requireActivity()))
    }

    private fun bindPreview(cameraProvider: ProcessCameraProvider) {
        try {
            cameraProvider.unbindAll()
            val selector = CameraSelector.DEFAULT_BACK_CAMERA
            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(binding.preview.surfaceProvider)
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
            val analyzer = ImageAnalyzer { result, bitmap ->
                requireActivity().runOnUiThread {
                    analysis.clearAnalyzer()
                    cameraProvider.unbindAll()
                    callback(result, bitmap)
                    dismiss()
                }
            }
            analysis.setAnalyzer(cameraExecutor, analyzer)
            cameraProvider.bindToLifecycle(requireActivity(), selector, preview, analysis)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isYuv(image: ImageProxy): Boolean {
        return image.format == ImageFormat.YUV_420_888 || image.format == ImageFormat.YUV_422_888 || image.format == ImageFormat.YUV_444_888
    }

    private fun getLuminancePlaneData(image: ImageProxy): ByteArray {
        val plane = image.planes[0]
        val buf = plane.buffer
        val data = ByteArray(buf.remaining())
        buf[data]
        buf.rewind()
        val width = image.width
        val height = image.height
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride
        val cleanData = ByteArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                cleanData[y * width + x] = data[y * rowStride + x * pixelStride]
            }
        }
        return cleanData
    }

    private fun rotateImageArray(imageToRotate: RotatedImage, rotationDegrees: Int) {
        if (rotationDegrees == 0) return // no rotation
        if (rotationDegrees % 90 != 0) return // only 90 degree times rotations
        val width = imageToRotate.width
        val height = imageToRotate.height
        val rotatedData = ByteArray(imageToRotate.data.size)
        for (y in 0 until height) { // we scan the array by rows
            for (x in 0 until width) {
                when (rotationDegrees) {
                    90 -> rotatedData[x * height + height - y - 1] =
                        imageToRotate.data[x + y * width] // Fill from top-right toward left (CW)
                    180 -> rotatedData[width * (height - y - 1) + width - x - 1] =
                        imageToRotate.data[x + y * width] // Fill from bottom-right toward up (CW)
                    270 -> rotatedData[y + x * height] =
                        imageToRotate.data[y * width + width - x - 1] // The opposite (CCW) of 90 degrees
                }
            }
        }
        imageToRotate.data = rotatedData
        if (rotationDegrees != 180) {
            imageToRotate.height = width
            imageToRotate.width = height
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        cameraExecutor.shutdown()
    }

    override val viewBinding: ScanDialogBinding
        get() = ScanDialogBinding.inflate(layoutInflater)

    companion object {
        const val TAG = "ScanDialog"
        const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

        fun isPermissionGranted(context: Context): Boolean = ContextCompat.checkSelfPermission(
            context,
            REQUIRED_PERMISSIONS[0]
        ) == PackageManager.PERMISSION_GRANTED

        fun requestPermission(activity: Activity) {
            ActivityCompat.requestPermissions(
                activity,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }
    }

    inner class RotatedImage(var data: ByteArray, var width: Int, var height: Int)

    inner class PoinCallback : ResultPointCallback {
        override fun foundPossibleResultPoint(point: ResultPoint) {
            binding.viewfinder.addPossibleResultPoint(point)
        }
    }

    inner class ImageAnalyzer(val onAnalyzed: (result: Result, bitmap: Bitmap) -> Unit) :
        ImageAnalysis.Analyzer {
        override fun analyze(image: ImageProxy) {
            if (isScaning.get()) {
                image.close()
                return
            }
            isScaning.set(true)
            if (isYuv(image) && image.planes.size == 3) {
                val rotatedImage =
                    RotatedImage(getLuminancePlaneData(image), image.width, image.height)
                val rotationDegrees = image.imageInfo.rotationDegrees
                rotateImageArray(rotatedImage, rotationDegrees)
                val source = PlanarYUVLuminanceSource(
                    rotatedImage.data, rotatedImage.width, rotatedImage.height, 0, 0,
                    rotatedImage.width, rotatedImage.height, false
                )
                val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
                try {
                    val result = fromatReader.decodeWithState(binaryBitmap)
                    onAnalyzed(result, generateBarcode(source))
                } catch (nfe: NotFoundException) {
                    nfe.printStackTrace()
                } finally {
                    fromatReader.reset()
                    image.close()
                }
                isScaning.set(false)
            }
        }

        private fun generateBarcode(source: PlanarYUVLuminanceSource): Bitmap {
            val pixels = source.renderThumbnail()
            val width = source.thumbnailWidth
            val height = source.thumbnailHeight
            var bitmap =
                Bitmap.createBitmap(pixels, 0, width, width, height, Bitmap.Config.ARGB_8888)
            bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            return bitmap
        }
    }
}