package otang.app.scanner.preference

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.res.TypedArrayUtils
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import otang.app.scanner.R

@Suppress("unused")
open class SeekBarPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int,
    defStyleRes: Int = 0
) : Preference(context, attrs, defStyleAttr, defStyleRes), OnSeekBarChangeListener,
    View.OnClickListener, OnLongClickListener {
    private val tag = javaClass.name
    private var mInterval = 1
    private var mShowSign = false
    private var mUnits = ""
    private var mSummary = "Value"
    private var mContinuousUpdates = false
    private var mMinValue = 0
    private var mMaxValue = 100
    private var mDefaultValueExists = false
    private var mDefaultValue = 0
    private var mValue = 0
    private var mValueTextView: TextView? = null
    private var mResetImageView: ImageView? = null
    private var mMinusImageView: ImageView? = null
    private var mPlusImageView: ImageView? = null
    private var mSeekBar: SeekBar? = null
    private var mTrackingTouch = false
    private var mTrackingValue = 0
    private val mContext: Context

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.CustomSeekBarPreference)
        try {
            mShowSign = a.getBoolean(R.styleable.CustomSeekBarPreference_showSign, mShowSign)
            val units = a.getString(R.styleable.CustomSeekBarPreference_units)
            if (units != null) mUnits = " $units"
            mContinuousUpdates = a.getBoolean(
                R.styleable.CustomSeekBarPreference_continuousUpdates,
                mContinuousUpdates
            )
        } finally {
            a.recycle()
        }
        try {
            val newInterval = attrs!!.getAttributeValue(APPNS, "interval")
            if (newInterval != null) mInterval = newInterval.toInt()
        } catch (e: Exception) {
            Log.e(tag, "Invalid interval value", e)
        }
        mMinValue = attrs!!.getAttributeIntValue(APPNS, "min", mMinValue)
        mMaxValue = attrs.getAttributeIntValue(ANDROIDNS, "max", mMaxValue)
        if (mMaxValue < mMinValue) {
            mMaxValue = mMinValue
        }
        val defaultValue = attrs.getAttributeValue(ANDROIDNS, "defaultValue")
        mDefaultValueExists = defaultValue != null && defaultValue.isNotEmpty()
        if (mDefaultValueExists) {
            mDefaultValue = getLimitedValue(defaultValue!!.toInt())
            mValue = mDefaultValue
        } else {
            mValue = mMinValue
        }
        mSummary = attrs.getAttributeValue(ANDROIDNS, "summary")
        if (mSummary.isEmpty() || mSummary == "") {
            mSummary = context.getString(R.string.value)
        }
        mSeekBar = SeekBar(context, attrs)
        layoutResource = R.layout.preference_custom_seekbar
        mContext = context
    }

    @SuppressLint("RestrictedApi")
    constructor(context: Context, attrs: AttributeSet?) : this(
        context,
        attrs,
        TypedArrayUtils.getAttr(
            context,
            androidx.preference.R.attr.preferenceStyle,
            android.R.attr.preferenceStyle
        )
    )

    constructor(context: Context) : this(context, null)

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        try {
            val oldContainer = mSeekBar!!.parent
            val newContainer = holder.findViewById(R.id.seekbar) as ViewGroup
            if (oldContainer !== newContainer) {
                if (oldContainer != null) {
                    (oldContainer as ViewGroup).removeView(mSeekBar)
                }
                newContainer.removeAllViews()
                newContainer.addView(
                    mSeekBar,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
        } catch (ex: Exception) {
            Log.e(tag, "Error binding view: $ex")
        }
        mSeekBar!!.max = getSeekValue(mMaxValue)
        mSeekBar!!.progress = getSeekValue(mValue)
        mSeekBar!!.isEnabled = isEnabled
        mValueTextView = holder.findViewById(R.id.value) as TextView
        mResetImageView = holder.findViewById(R.id.reset) as ImageView
        mMinusImageView = holder.findViewById(R.id.minus) as ImageView
        mPlusImageView = holder.findViewById(R.id.plus) as ImageView
        updateValueViews()
        mSeekBar!!.setOnSeekBarChangeListener(this)
        mResetImageView!!.setOnClickListener(this)
        mMinusImageView!!.setOnClickListener(this)
        mPlusImageView!!.setOnClickListener(this)
        mResetImageView!!.setOnLongClickListener(this)
        mMinusImageView!!.setOnLongClickListener(this)
        mPlusImageView!!.setOnLongClickListener(this)
    }

    private fun getLimitedValue(v: Int): Int {
        return if (v < mMinValue) mMinValue else if (v > mMaxValue) mMaxValue else v
    }

    private fun getSeekValue(v: Int): Int {
        return 0 - Math.floorDiv(mMinValue - v, mInterval)
    }

    private fun getTextValue(v: Int): String {
        return (if (mShowSign && v > 0) "+" else "") + v.toString() + mUnits
    }

    private fun updateValueViews() {
        if (mValueTextView != null) {
            mValueTextView!!.text = context.getString(
                R.string.custom_seekbar_value,
                if (!mTrackingTouch || mContinuousUpdates) mSummary + ": " + getTextValue(mValue) + (if (mDefaultValueExists && mValue == mDefaultValue) " (" + context.getString(
                    R.string.custom_seekbar_default_value
                ) + ")" else "") else getTextValue(mTrackingValue)
            )
        }
        if (mResetImageView != null) {
            if (!mDefaultValueExists || mValue == mDefaultValue || mTrackingTouch) {
                mResetImageView!!.visibility = View.INVISIBLE
            } else {
                mResetImageView!!.visibility = View.VISIBLE
            }
        }
        if (mMinusImageView != null) {
            if (mValue == mMinValue || mTrackingTouch) {
                mMinusImageView!!.isClickable = false
                mMinusImageView!!.setColorFilter(
                    context.getColor(R.color.disabled_text_color),
                    PorterDuff.Mode.MULTIPLY
                )
            } else {
                mMinusImageView!!.isClickable = true
                mMinusImageView!!.clearColorFilter()
            }
        }
        if (mPlusImageView != null) {
            if (mValue == mMaxValue || mTrackingTouch) {
                mPlusImageView!!.isClickable = false
                mPlusImageView!!.setColorFilter(
                    context.getColor(R.color.disabled_text_color),
                    PorterDuff.Mode.MULTIPLY
                )
            } else {
                mPlusImageView!!.isClickable = true
                mPlusImageView!!.clearColorFilter()
            }
        }
    }

    private fun changeValue() {}
    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        val newValue = getLimitedValue(mMinValue + progress * mInterval)
        if (mTrackingTouch && !mContinuousUpdates) {
            mTrackingValue = newValue
            updateValueViews()
            //VibrationUtils.doHapticFeedback(mContext, VibrationEffect.EFFECT_TEXTURE_TICK);
        } else if (mValue != newValue) {
            if (!callChangeListener(newValue)) {
                mSeekBar!!.progress = getSeekValue(mValue)
                return
            }
            changeValue()
            persistInt(newValue)
            mValue = newValue
            updateValueViews()
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {
        mTrackingValue = mValue
        mTrackingTouch = true
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        mTrackingTouch = false
        if (!mContinuousUpdates) {
            onProgressChanged(mSeekBar!!, getSeekValue(mTrackingValue), false)
        }
        notifyChanged()
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.reset -> {
                Toast.makeText(
                    context,
                    context.getString(
                        R.string.custom_seekbar_default_value_to_set,
                        getTextValue(mDefaultValue)
                    ),
                    Toast.LENGTH_LONG
                ).show()
            }

            R.id.minus -> {
                setValue(mValue - mInterval, true)
            }

            R.id.plus -> {
                setValue(mValue + mInterval, true)
            }
        }
        //VibrationUtils.doHapticFeedback(mContext, VibrationEffect.EFFECT_CLICK);
    }

    override fun onLongClick(v: View): Boolean {
        when (v.id) {
            R.id.reset -> {
                setValue(mDefaultValue, true)
            }

            R.id.minus -> {
                setValue(
                    if (mMaxValue - mMinValue > mInterval * 2 && mMaxValue + mMinValue < mValue * 2) Math.floorDiv(
                        mMaxValue + mMinValue,
                        2
                    ) else mMinValue, true
                )
            }

            R.id.plus -> {
                setValue(
                    if (mMaxValue - mMinValue > mInterval * 2 && mMaxValue + mMinValue > mValue * 2) -1 * Math.floorDiv(
                        -1 * (mMaxValue + mMinValue),
                        2
                    ) else mMaxValue, true
                )
            }
        }
        return true
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        mValue = getPersistedInt(mValue)
    }

    override fun setDefaultValue(defaultValue: Any) {
        if (defaultValue is Int) {
            setDefaultValue(defaultValue, mSeekBar != null)
        } else {
            setDefaultValue(defaultValue.toString(), mSeekBar != null)
        }
    }

    @Suppress("NAME_SHADOWING")
    private fun setDefaultValue(newValue: Int, update: Boolean) {
        var newValue = newValue
        newValue = getLimitedValue(newValue)
        if (!mDefaultValueExists || mDefaultValue != newValue) {
            mDefaultValueExists = true
            mDefaultValue = newValue
            if (update) {
                updateValueViews()
            }
        }
    }

    private fun setDefaultValue(newValue: String?, update: Boolean) {
        if (mDefaultValueExists && newValue.isNullOrEmpty()) {
            mDefaultValueExists = false
            if (update) {
                updateValueViews()
            }
        } else if (!newValue.isNullOrEmpty()) {
            setDefaultValue(newValue.toInt(), update)
        }
    }

    fun setMax(max: Int) {
        mMaxValue = max
        mSeekBar!!.max = mMaxValue - mMinValue
    }

    fun setMin(min: Int) {
        mMinValue = min
        mSeekBar!!.max = mMaxValue - mMinValue
    }

    @Suppress("NAME_SHADOWING")
    fun setValue(newValue: Int, update: Boolean) {
        var newValue = newValue
        newValue = getLimitedValue(newValue)
        if (mValue != newValue) {
            if (update) {
                mSeekBar!!.progress = getSeekValue(newValue)
            } else {
                mValue = newValue
            }
        }
    }

    var value: Int
        get() = mValue
        set(newValue) {
            mValue = getLimitedValue(newValue)
            if (mSeekBar != null) {
                mSeekBar!!.progress = getSeekValue(mValue)
            }
        }

    fun refresh(newValue: Int) {
        setValue(newValue, mSeekBar != null)
    }

    companion object {
        private const val APPNS = "http://schemas.android.com/apk/res-auto"
        protected const val ANDROIDNS = "http://schemas.android.com/apk/res/android"
    }
}