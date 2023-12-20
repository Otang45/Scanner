package otang.app.scanner.util

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import androidx.preference.PreferenceManager

class PreferenceUtils(context: Context) {
    private val pref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val editor: SharedPreferences.Editor = pref.edit()

    // Decode
    val frameColor: Int = pref.getInt("pref_frame_color", Color.WHITE)
    val maskColor: Int = pref.getInt("pref_mask_color", Color.BLACK)
    val pointColor: Int = pref.getInt("pref_point_color", Color.GREEN)
    val cornerSize: Int = pref.getInt("pref_corner_size", 75)
    val cornerRadius: Int = pref.getInt("pref_corner_radius", 50)
    val cornerWidth: Int = pref.getInt("pref_corner_width", 10)

    // Encode
    val bgColor: Int = pref.getInt("pref_bg_color", Color.WHITE)
    val contentColor: Int = pref.getInt("pref_content_color", Color.BLACK)
    val qrMargin: Int = pref.getInt("pref_qr_margin", 2)
    val useCustomLogo: Boolean = pref.getBoolean("pref_customs_logo", false)
    val addLogo: Boolean = pref.getBoolean("pref_add_logo", false)
    val addText: Boolean = pref.getBoolean("pref_add_text", true)
    val logoShape: String = pref.getString("pref_logo_shape", "0") ?: "0"
    val customLogo: String = pref.getString("pref_custom_logo", "") ?: ""
    fun setCustomLogo(data: String) {
        editor.putString("pref_custom_logo", data).commit()
    }
}