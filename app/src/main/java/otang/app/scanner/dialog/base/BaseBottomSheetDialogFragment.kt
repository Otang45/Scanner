package otang.app.scanner.dialog.base

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.WindowCompat
import androidx.viewbinding.ViewBinding
import com.google.android.material.R
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.internal.EdgeToEdgeUtils

abstract class BaseBottomSheetDialogFragment<T : ViewBinding> : BottomSheetDialogFragment() {
    protected lateinit var binding: T

    override fun onCreateDialog(bundle: Bundle?): Dialog {
        return BaseBottomSheetDialog(requireActivity())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        viewGroup: ViewGroup?,
        bundle: Bundle?
    ): View? {
        binding = viewBinding
        return binding.root
    }

    override fun onViewCreated(view: View, bundle: Bundle?) {
        super.onViewCreated(view, bundle)
        onViewCreated()
    }

    abstract val viewBinding: T
    abstract fun onViewCreated()

    inner class BaseBottomSheetDialog(context: Context) : BottomSheetDialog(context) {
        @SuppressLint("PrivateResource")
        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            window?.let {
                val root =
                    View.inflate(context, R.layout.design_bottom_sheet_dialog, null) as FrameLayout
                val coordinator = root.findViewById<View>(R.id.coordinator) as CoordinatorLayout
                val container = root.findViewById<View>(R.id.design_bottom_sheet) as FrameLayout
                // If the navigation bar is transparent at all the BottomSheet should be edge to edge.
                root.fitsSystemWindows = false
                container.fitsSystemWindows = false
                coordinator.fitsSystemWindows = false
                WindowCompat.setDecorFitsSystemWindows(it, false)
            }
        }

        @SuppressLint("RestrictedApi")
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            window?.let { EdgeToEdgeUtils.applyEdgeToEdge(it, true) }
        }
    }
}