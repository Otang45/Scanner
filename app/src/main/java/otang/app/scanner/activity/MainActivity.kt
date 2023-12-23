package otang.app.scanner.activity

import android.content.Intent
import android.content.pm.PackageManager
import android.view.View
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import otang.app.scanner.R
import otang.app.scanner.databinding.MainActivityBinding
import otang.app.scanner.dialog.AboutDialog
import otang.app.scanner.dialog.CreateDialog
import otang.app.scanner.dialog.ResultDialog
import otang.app.scanner.dialog.ScanDialog
import otang.app.scanner.util.ImagePickerUtils
import otang.app.scanner.util.SaveUtils

class MainActivity : BaseActivity<MainActivityBinding>() {
    private val imagePicker = ImagePickerUtils(this) { result, barcode ->
        ResultDialog(result, barcode).show(supportFragmentManager, ResultDialog.TAG)
    }

    @Suppress("UNUSED_PARAMETER")
    fun createQR(view: View) {
        MaterialAlertDialogBuilder(this).setTitle(getString(R.string.create))
            .setMessage(getString(R.string.create_dialog_info))
            .setPositiveButton(getString(R.string.qrcode)) { dialog, _ ->
                dialog.dismiss()
                create(CreateDialog.CreateType.QR_CODE)
            }
            .setNegativeButton(getString(R.string.barcode)) { dialog, _ ->
                dialog.dismiss()
                create(CreateDialog.CreateType.BARCODE)
            }.show()
    }

    private fun create(type: CreateDialog.CreateType) {
        CreateDialog(type).show(supportFragmentManager, CreateDialog.TAG)
    }

    @Suppress("UNUSED_PARAMETER")
    fun scanQR(view: View) {
        ScanDialog { result, bitmap ->
            ResultDialog(result, bitmap).show(supportFragmentManager, ResultDialog.TAG)
        }.show(supportFragmentManager, ScanDialog.TAG)
    }

    @Suppress("UNUSED_PARAMETER")
    fun scanLocal(view: View) {
        imagePicker.pickImage()
    }

    @Suppress("UNUSED_PARAMETER")

    fun openSettings(view: View) {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    @Suppress("UNUSED_PARAMETER")
    fun aboutDialog(view: View) {
        AboutDialog().show(supportFragmentManager, AboutDialog.TAG)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == ScanDialog.REQUEST_CODE_PERMISSIONS) {
            if (!ScanDialog.isPermissionGranted(this)) {
                MaterialAlertDialogBuilder(this).setTitle(getString(R.string.info))
                    .setMessage(getString(R.string.need_permission_dialog_info))
                    .setPositiveButton(getString(R.string.oke)) { dialog, _ ->
                        ScanDialog.requestPermission(this)
                        dialog.dismiss()
                    }.show()
            } else {
                scanQR(binding.mbScan)
            }
        }
        if (requestCode == SaveUtils.REQ_STORAGE_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, getString(R.string.save_again), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, getString(R.string.permission_required), Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    override val viewBinding: MainActivityBinding
        get() = MainActivityBinding.inflate(layoutInflater)
}