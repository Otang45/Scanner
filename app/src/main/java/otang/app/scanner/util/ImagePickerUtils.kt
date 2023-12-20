package otang.app.scanner.util

import android.graphics.Bitmap
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.Result
import com.google.zxing.common.HybridBinarizer
import java.util.EnumMap

class ImagePickerUtils(
    private val activity: AppCompatActivity,
    private val callback: ((result: Result, barcode: Bitmap) -> Unit)?
) {
    private val fromatReader: MultiFormatReader = MultiFormatReader()
    private var hints: MutableMap<DecodeHintType, Any> = EnumMap(DecodeHintType::class.java)
    private val pickMedia =
        activity.registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                val bitmap = AppUtils.getBitmapFromUri(activity, uri)
                val width = bitmap.width
                val height = bitmap.height
                val pixels = IntArray(width * height)
                bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
                val source = RGBLuminanceSource(width, height, pixels)
                val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
                try {
                    val result = fromatReader.decodeWithState(binaryBitmap)
                    callback?.invoke(result, bitmap)
                } catch (nfe: NotFoundException) {
                    nfe.printStackTrace()
                } finally {
                    fromatReader.reset()
                }
            }
        }

    init {
        hints[DecodeHintType.POSSIBLE_FORMATS] = AppUtils.getDefaultFormat()
        hints[DecodeHintType.TRY_HARDER] = true
        hints[DecodeHintType.CHARACTER_SET] = "UTF-8"
        fromatReader.setHints(hints)
    }

    fun pickImage() {
        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
}