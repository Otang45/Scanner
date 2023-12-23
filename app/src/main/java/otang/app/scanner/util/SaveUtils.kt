package otang.app.scanner.util

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import otang.app.scanner.R

class SaveUtils(private val activity: Activity) {
    fun saveToStorage(bitmap: Bitmap) {
        val time = AppUtils.getTimeFormat("ddMMyyHHmmss")
        val displayName = "Scanner-$time.jpg"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/*")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }
        }
        var uri: Uri? = null
        if (checkPremission()) {
            runCatching {
                with(activity.contentResolver) {
                    insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)?.also {
                        uri = it
                        openOutputStream(it)?.use { stream ->
                            try {
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                                Toast.makeText(
                                    activity,
                                    activity.getString(R.string.image_saved),
                                    Toast.LENGTH_SHORT
                                ).show()
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(activity, e.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    } ?: Toast.makeText(activity, "Save failed", Toast.LENGTH_SHORT).show()
                }
            }.getOrElse {
                it.printStackTrace()
                uri?.let { orphanUri ->
                    activity.contentResolver.delete(orphanUri, null, null)
                }
                Toast.makeText(activity, it.message, Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(
                activity,
                activity.getString(R.string.permission_required),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun checkPremission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            true
        } else {
            val result = ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            if (result == PackageManager.PERMISSION_GRANTED) {
                true
            } else {
                ActivityCompat.requestPermissions(
                    activity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    REQ_STORAGE_CODE
                )
                false
            }
        }
    }

    companion object {
        const val REQ_STORAGE_CODE = 21
    }
}