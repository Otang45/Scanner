package otang.app.scanner.dialog

import android.graphics.Bitmap
import android.view.View
import com.google.zxing.Result
import com.google.zxing.ResultMetadataType
import com.google.zxing.client.result.ParsedResultType
import com.google.zxing.client.result.ResultParser
import otang.app.scanner.databinding.ResultDialogBinding
import otang.app.scanner.dialog.base.BaseBottomSheetDialogFragment
import otang.app.scanner.util.AppUtils
import java.util.EnumSet

class ResultDialog(private val result: Result, private val bitmap: Bitmap) :
    BaseBottomSheetDialogFragment<ResultDialogBinding>() {

    override fun onViewCreated() {
        val params = binding.root.layoutParams
        params.height = AppUtils.getScreenHeight(requireActivity()) / 5 * 3
        binding.root.layoutParams = params
        binding.iv.setImageBitmap(bitmap)
        val resultType = ResultParser.parseResult(result)
        val resultText = resultType.displayResult.replace("\r", "")
        binding.tvResult.text = resultText
        binding.tvFormat.text = result.barcodeFormat.toString()
        binding.tvType.text = resultType.type.toString()
        binding.tvTime.text = AppUtils.getTimeFormat(result.timestamp, "dd/MM/yy HH:mm")
        val metadata: Map<ResultMetadataType, Any> = result.resultMetadata
        metadata.let {
            val sb = StringBuilder(20)
            for (entry in it.entries) {
                if (DISPLAYABLE_METADATA_TYPES.contains(entry.key)) {
                    sb.append(entry.value).append("\n")
                }
            }
            if (sb.isNotEmpty()) {
                sb.setLength(sb.length - 1)
                binding.tvMetadata.text = sb
            }
        }
        if (resultType.type == ParsedResultType.URI) {
            binding.mbOpen.visibility = View.VISIBLE
        } else {
            binding.mbOpen.visibility = View.GONE
        }
        binding.mbOpen.setOnClickListener {
            AppUtils.openLink(requireActivity(), resultText)
        }
        binding.mbCopy.setOnClickListener {
            AppUtils.copyText(requireActivity(), resultText)
        }
        binding.mbShare.setOnClickListener {
            AppUtils.shareText(requireActivity(), resultText)
        }
    }

    override val viewBinding: ResultDialogBinding
        get() = ResultDialogBinding.inflate(layoutInflater)

    companion object {
        const val TAG = "ResultDialog"
        private val DISPLAYABLE_METADATA_TYPES: Collection<ResultMetadataType> = EnumSet.of(
            ResultMetadataType.ISSUE_NUMBER,
            ResultMetadataType.SUGGESTED_PRICE,
            ResultMetadataType.ERROR_CORRECTION_LEVEL,
            ResultMetadataType.POSSIBLE_COUNTRY
        )
    }
}