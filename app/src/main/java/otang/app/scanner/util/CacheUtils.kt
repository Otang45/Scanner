package otang.app.scanner.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

class CacheUtils(private val context: Context) {
    fun saveBitmapToCache(bitmap: Bitmap) {
        val file = File(context.cacheDir, "images")
        file.mkdirs()
        val stream = FileOutputStream(file.path + "/" + "image.jpg")
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        stream.flush()
        stream.close()
    }

    fun getImageCacheUri(): Uri {
        val newFile = File(context.cacheDir, "/images/image.jpg")
        return FileProvider.getUriForFile(context, context.packageName + ".provider", newFile)
    }
}