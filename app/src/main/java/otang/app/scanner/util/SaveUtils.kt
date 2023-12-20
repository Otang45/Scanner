package otang.app.scanner.util

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContextCompat
import otang.app.scanner.R
import java.io.File
import java.io.FileOutputStream

class SaveUtils(private val activity: Activity) {
    fun saveToStorage(bitmap: Bitmap) {
        if (!checkPremission()) {
            Toast.makeText(
                activity,
                activity.getString(R.string.permission_required),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            try {
                val dir = getDownloadDir()
                val time = AppUtils.getTimeFormat("ddMMyyHHmmss")
                val stream = FileOutputStream(dir.path + "/Scanner-" + time + ".jpg")
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                stream.flush()
                stream.close()
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
    }

    private fun getDownloadDir(): File {
        return File(
            Environment.getExternalStorageDirectory().absolutePath,
            Environment.DIRECTORY_DOWNLOADS
        )
    }

    private fun checkPremission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                true
            } else {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.addCategory("android.intent.category.DEFAULT")
                    intent.setData(
                        Uri.parse(
                            String.format(
                                "package:%s",
                                activity.packageName
                            )
                        )
                    )
                    startActivityForResult(activity, intent, REQ_STORAGE_CODE, null)
                } catch (e: Exception) {
                    val intent = Intent()
                    intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivityForResult(activity, intent, REQ_STORAGE_CODE, null)
                }
                false
            }
        } else {
            val result = ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            val result1 = ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            if (result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED) {
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