package otang.app.scanner.activity

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.viewbinding.ViewBinding
import com.google.android.material.internal.EdgeToEdgeUtils

abstract class BaseActivity<T : ViewBinding> : AppCompatActivity() {
    protected lateinit var binding: T

    @SuppressLint("RestrictedApi")
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        EdgeToEdgeUtils.applyEdgeToEdge(window, true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = viewBinding
        setContentView(binding.root)
    }

    abstract val viewBinding: T
}