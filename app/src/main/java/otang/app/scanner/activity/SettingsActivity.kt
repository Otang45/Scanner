package otang.app.scanner.activity

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import otang.app.scanner.R
import otang.app.scanner.databinding.SettingsActivityBinding
import otang.app.scanner.util.AppUtils
import otang.app.scanner.util.PreferenceUtils
import java.io.ByteArrayOutputStream

class SettingsActivity : BaseActivity<SettingsActivityBinding>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager.beginTransaction().replace(R.id.container, SettingsFragment())
            .commit()
    }

    override val viewBinding: SettingsActivityBinding
        get() = SettingsActivityBinding.inflate(layoutInflater)

    class SettingsFragment : PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener,
        Preference.OnPreferenceClickListener {
        private lateinit var prefUtils: PreferenceUtils
        private val logoLauncher =
            registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                if (uri != null) {
                    val bitmap = AppUtils.getBitmapFromUri(requireActivity(), uri)
                    val baos = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
                    val encodedLogo = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
                    val pref = PreferenceUtils(requireActivity())
                    pref.setCustomLogo(encodedLogo)
                    Toast.makeText(activity,
                        getString(R.string.custom_logo_saved), Toast.LENGTH_SHORT).show()
                }
            }
        private var addLogo: SwitchPreferenceCompat? = null
        private var logoGroup: PreferenceCategory? = null
        private var customsLogo: SwitchPreferenceCompat? = null
        private var customLogo: Preference? = null

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            addPreferencesFromResource(R.xml.preferences)
            prefUtils = PreferenceUtils(requireActivity())
            addLogo = findPreference("pref_add_logo")
            logoGroup = findPreference("pref_logo")
            customsLogo = findPreference("pref_customs_logo")
            customLogo = findPreference("pref_custom_logo")
            val addLogos = prefUtils.addLogo
            val customsLogos = prefUtils.useCustomLogo
            updateLogoPref(addLogos)
            updateCustomLogoPref(customsLogos)
            addLogo?.onPreferenceChangeListener = this
            customsLogo?.onPreferenceChangeListener = this
            customLogo?.onPreferenceClickListener = this
        }

        private fun updateLogoPref(show: Boolean) {
            logoGroup?.isEnabled = show
        }

        private fun updateCustomLogoPref(show: Boolean) {
            customLogo?.isEnabled = show
        }

        override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
            when (preference.key) {
                "pref_add_logo" -> updateLogoPref(newValue as Boolean)
                "pref_customs_logo" -> updateCustomLogoPref(newValue as Boolean)
            }
            return true
        }

        override fun onPreferenceClick(preference: Preference): Boolean {
            when (preference.key) {
                "pref_custom_logo" -> {
                    logoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            }
            return true
        }
    }
}