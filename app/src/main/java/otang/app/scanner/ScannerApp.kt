package otang.app.scanner

import android.app.Application
import com.google.android.material.color.DynamicColors

class ScannerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}