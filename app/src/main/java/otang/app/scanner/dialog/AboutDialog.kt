package otang.app.scanner.dialog

import otang.app.scanner.R
import otang.app.scanner.databinding.AboutDialogBinding
import otang.app.scanner.dialog.base.BaseBottomSheetDialogFragment
import otang.app.scanner.util.AppUtils

class AboutDialog : BaseBottomSheetDialogFragment<AboutDialogBinding>() {
    override fun onViewCreated() {
        binding.mbShare.setOnClickListener {
            AppUtils.shareText(requireActivity(), getString(R.string.github_url))
        }
        binding.mbGithub.setOnClickListener {
            AppUtils.openLink(requireActivity(), getString(R.string.github_url))
        }
    }

    override val viewBinding: AboutDialogBinding
        get() = AboutDialogBinding.inflate(layoutInflater)

    companion object {
        const val TAG = "AboutDialog"
    }
}