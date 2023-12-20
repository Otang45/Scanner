package otang.app.scanner.dialog

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.text.TextPaint
import android.util.Base64
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import otang.app.scanner.R
import otang.app.scanner.databinding.CreateDialogBinding
import otang.app.scanner.dialog.base.BaseBottomSheetDialogFragment
import otang.app.scanner.util.AppUtils
import otang.app.scanner.util.CacheUtils
import otang.app.scanner.util.PreferenceUtils
import otang.app.scanner.util.SaveUtils
import java.util.EnumMap
import java.util.concurrent.atomic.AtomicBoolean

class CreateDialog(private val type: CreateType) :
    BaseBottomSheetDialogFragment<CreateDialogBinding>() {

    private lateinit var hints: MutableMap<EncodeHintType, Any>
    private lateinit var saveUtils: SaveUtils
    private lateinit var cacheUtils: CacheUtils
    private lateinit var prefUtils: PreferenceUtils
    private var multiFormatWriter: MultiFormatWriter = MultiFormatWriter()
    private var hasNewImage = AtomicBoolean(false)
    private var barcode: Bitmap? = null

    override fun onViewCreated() {
        prefUtils = PreferenceUtils(requireActivity())
        hints = EnumMap(EncodeHintType::class.java)
        hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
        hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.H
        hints[EncodeHintType.MARGIN] = prefUtils.qrMargin
        saveUtils = SaveUtils(requireActivity())
        cacheUtils = CacheUtils(requireActivity())
        binding.tiet.setOnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_DONE) {
                val content = binding.tiet.text.toString()
                if (content.isBlank()) {
                    Toast.makeText(
                        requireActivity(),
                        getString(R.string.content_cannot_empty), Toast.LENGTH_SHORT
                    )
                        .show()
                } else {
                    val bitmap = createBarcode(content)
                    bitmap?.let {
                        cacheUtils.saveBitmapToCache(it)
                        binding.iv.setImageBitmap(it)
                        binding.tvContent.text = content
                        barcode = it
                        hasNewImage.set(true)
                    }
                }
            }
            binding.tiet.text?.clear()
            binding.til.clearFocus()
            false
        }
        binding.mbSave.setOnClickListener {
            barcode?.let {
                saveUtils.saveToStorage(it)
            }
        }
        binding.mbShare.setOnClickListener {
            if (hasNewImage.get()) {
                val uri = cacheUtils.getImageCacheUri()
                AppUtils.shareImage(requireActivity(), uri)
            }
        }
    }

    @Suppress("SameParameterValue")
    private fun createBarcode(content: String): Bitmap? {
        val width: Int
        val height: Int
        val format: BarcodeFormat
        if (type == CreateType.QR_CODE) {
            width = 1024
            height = 1024
            format = BarcodeFormat.QR_CODE
        } else {
            width = 1024
            height = 350
            format = BarcodeFormat.CODE_128
        }
        try {
            val bitMatrix = multiFormatWriter.encode(content, format, width, height, hints)
            bitMatrix.let {
                val pixels = IntArray(it.width * it.height)
                for (y in 0 until it.height) {
                    val offset = y * it.width
                    for (x in 0 until it.width) {
                        pixels[offset + x] =
                            if (it.get(x, y)) prefUtils.contentColor else prefUtils.bgColor
                    }
                }
                val bitmap = Bitmap.createBitmap(it.width, it.height, Bitmap.Config.ARGB_8888)
                bitmap.setPixels(pixels, 0, it.width, 0, 0, it.width, it.height)
                if (type == CreateType.BARCODE && prefUtils.addText) {
                    val res = Bitmap.createBitmap(
                        bitmap.width,
                        bitmap.height + 90 * 2,
                        Bitmap.Config.ARGB_8888
                    )
                    val canvas = Canvas(res)
                    canvas.drawColor(prefUtils.bgColor)
                    canvas.drawBitmap(bitmap, 0f, 30f, null)
                    val txtPaint = TextPaint()
                    txtPaint.textSize = 60f
                    txtPaint.color = prefUtils.contentColor
                    txtPaint.textAlign = Paint.Align.CENTER
                    canvas.drawText(
                        content,
                        bitmap.width / 2f,
                        bitmap.height + 60 * 2f,
                        txtPaint
                    )
                    canvas.save()
                    canvas.restore()
                    return res
                } else if (type == CreateType.BARCODE) {
                    val res = Bitmap.createBitmap(
                        bitmap.width + 40,
                        bitmap.height + 60,
                        Bitmap.Config.ARGB_8888
                    )
                    val canvas = Canvas(res)
                    canvas.drawColor(prefUtils.bgColor)
                    canvas.drawBitmap(bitmap, 20f, 30f, null)
                    canvas.save()
                    canvas.restore()
                    return res
                }
                if (type == CreateType.QR_CODE && prefUtils.addLogo) {
                    val uri = prefUtils.customLogo
                    val logo = if (uri.isNotBlank() && prefUtils.useCustomLogo) {
                        val byte = Base64.decode(uri, Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(byte, 0, byte.size)
                    } else {
                        AppUtils.getBitmapFromVectorDrawable(
                            requireActivity(),
                            R.drawable.ic_custom_logo
                        )
                    }
                    val scale = (bitmap.width * 0.1 / logo.width).toFloat()
                    val cPaint = Paint()
                    cPaint.color = prefUtils.bgColor
                    val res =
                        Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(res)
                    canvas.drawBitmap(bitmap, 0f, 0f, null)
                    canvas.scale(scale, scale, bitmap.width / 2f, bitmap.height / 2f)
                    val shape = prefUtils.logoShape
                    if (shape == "0") {
                        canvas.drawCircle(
                            (bitmap.width) / 2f,
                            (bitmap.height) / 2f,
                            (logo.width / 3 * 2) + 50f,
                            cPaint
                        )
                    } else {
                        canvas.drawRect(
                            ((bitmap.width - logo.width) / 2f) - 250,
                            ((bitmap.height - logo.height) / 2f) - 250,
                            ((bitmap.width + logo.width) / 2f) + 250,
                            ((bitmap.height + logo.height) / 2f) + 250,
                            cPaint
                        )
                    }
                    canvas.drawBitmap(
                        logo,
                        (bitmap.width - logo.width) / 2f,
                        (bitmap.height - logo.height) / 2f,
                        null
                    )
                    canvas.save()
                    canvas.restore()
                    return res
                }
                return bitmap
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    override val viewBinding: CreateDialogBinding
        get() = CreateDialogBinding.inflate(layoutInflater)

    companion object {
        const val TAG = "CreateDialog"
    }

    enum class CreateType {
        QR_CODE, BARCODE
    }
}