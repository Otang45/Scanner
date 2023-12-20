package otang.app.scanner.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.ColorUtils
import com.google.zxing.ResultPoint
import otang.app.scanner.util.PreferenceUtils
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class ViewfinderView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private var prefUtils: PreferenceUtils
    private val mMaskPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mFramePaint: Paint
    private val mPath: Path
    private lateinit var framingRect: Rect
    private val mFrameRatio = 1f
    private var mFrameSize = 0.75f
    private val mFrameVerticalBias = 0.5f
    private var mPossibleResultPoints: MutableList<ResultPoint>
    private var mLastPossibleResultPoints: List<ResultPoint>?
    private val mPaint: Paint

    init {
        prefUtils = PreferenceUtils(context)
        mMaskPaint.style = Paint.Style.FILL
        mMaskPaint.color = ColorUtils.setAlphaComponent(prefUtils.maskColor, 128)
        mFramePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mFramePaint.style = Paint.Style.STROKE
        mFramePaint.strokeWidth = prefUtils.cornerWidth.toFloat()
        mFramePaint.color = prefUtils.frameColor
        mPath = Path()
        mPath.fillType = Path.FillType.EVEN_ODD
        mPossibleResultPoints = ArrayList(5)
        mLastPossibleResultPoints = null
        mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mPaint.color = prefUtils.pointColor
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        val frame = getFrameRect()
        val top = frame.top.toFloat()
        val left = frame.left.toFloat()
        val right = frame.right.toFloat()
        val bottom = frame.bottom.toFloat()
        val frameCornersSize = prefUtils.cornerSize.toFloat()
        val frameCornersRadius = prefUtils.cornerRadius.toFloat()
        val path = mPath
        if (frameCornersRadius > 0) {
            val normalizedRadius = min(frameCornersRadius, max(frameCornersSize - 1, 0f))
            path.reset()
            path.moveTo(left, top + normalizedRadius)
            path.quadTo(left, top, left + normalizedRadius, top)
            path.lineTo(right - normalizedRadius, top)
            path.quadTo(right, top, right, top + normalizedRadius)
            path.lineTo(right, bottom - normalizedRadius)
            path.quadTo(right, bottom, right - normalizedRadius, bottom)
            path.lineTo(left + normalizedRadius, bottom)
            path.quadTo(left, bottom, left, bottom - normalizedRadius)
            path.lineTo(left, top + normalizedRadius)
            path.moveTo(0f, 0f)
            path.lineTo(width.toFloat(), 0f)
            path.lineTo(width.toFloat(), height.toFloat())
            path.lineTo(0f, height.toFloat())
            path.lineTo(0f, 0f)
            canvas.drawPath(path, mMaskPaint)
            path.reset()
            path.moveTo(left, top + frameCornersSize)
            path.lineTo(left, top + normalizedRadius)
            path.quadTo(left, top, left + normalizedRadius, top)
            path.lineTo(left + frameCornersSize, top)
            path.moveTo(right - frameCornersSize, top)
            path.lineTo(right - normalizedRadius, top)
            path.quadTo(right, top, right, top + normalizedRadius)
            path.lineTo(right, top + frameCornersSize)
            path.moveTo(right, bottom - frameCornersSize)
            path.lineTo(right, bottom - normalizedRadius)
            path.quadTo(right, bottom, right - normalizedRadius, bottom)
            path.lineTo(right - frameCornersSize, bottom)
            path.moveTo(left + frameCornersSize, bottom)
            path.lineTo(left + normalizedRadius, bottom)
            path.quadTo(left, bottom, left, bottom - normalizedRadius)
            path.lineTo(left, bottom - frameCornersSize)
            canvas.drawPath(path, mFramePaint)
        } else {
            path.reset()
            path.moveTo(left, top)
            path.lineTo(right, top)
            path.lineTo(right, bottom)
            path.lineTo(left, bottom)
            path.lineTo(left, top)
            path.moveTo(0f, 0f)
            path.lineTo(width.toFloat(), 0f)
            path.lineTo(width.toFloat(), height.toFloat())
            path.lineTo(0f, height.toFloat())
            path.lineTo(0f, 0f)
            canvas.drawPath(path, mMaskPaint)
            path.reset()
            path.moveTo(left, top + frameCornersSize)
            path.lineTo(left, top)
            path.lineTo(left + frameCornersSize, top)
            path.moveTo(right - frameCornersSize, top)
            path.lineTo(right, top)
            path.lineTo(right, top + frameCornersSize)
            path.moveTo(right, bottom - frameCornersSize)
            path.lineTo(right, bottom)
            path.lineTo(right - frameCornersSize, bottom)
            path.moveTo(left + frameCornersSize, bottom)
            path.lineTo(left, bottom)
            path.lineTo(left, bottom - frameCornersSize)
            canvas.drawPath(path, mFramePaint)
        }
        val currentPossible = mPossibleResultPoints
        val currentLast = mLastPossibleResultPoints
        val frameLeft = frame.left
        val frameTop = frame.top
        if (currentPossible.isEmpty()) {
            mLastPossibleResultPoints = null
        } else {
            mPossibleResultPoints = ArrayList(5)
            mLastPossibleResultPoints = currentPossible
            mPaint.alpha = CURRENT_POINT_OPACITY
            synchronized(currentPossible) {
                for (point in currentPossible) {
                    canvas.drawCircle(
                        (frameLeft + (point.x).toInt()).toFloat(),
                        (frameTop + (point.y).toInt()).toFloat(),
                        POINT_SIZE.toFloat(),
                        mPaint
                    )
                }
            }
        }
        currentLast?.let {
            mPaint.alpha = CURRENT_POINT_OPACITY / 2
            synchronized(it) {
                val radius = POINT_SIZE / 2.0f
                for (point in it) {
                    canvas.drawCircle(
                        (frameLeft + (point.x).toInt()).toFloat(),
                        (frameTop + (point.y).toInt()).toFloat(),
                        radius,
                        mPaint
                    )
                }
            }
        }
        postInvalidateDelayed(
            ANIMATION_DELAY,
            frame.left - POINT_SIZE,
            frame.top - POINT_SIZE,
            frame.right + POINT_SIZE,
            frame.bottom + POINT_SIZE
        )
    }

    private fun getFrameRect(): Rect {
        val viewAR = width.toFloat() / height.toFloat()
        val frameAR = mFrameRatio / mFrameRatio
        val frameSize = mFrameSize
        val frameWidth: Int
        val frameHeight: Int
        if (viewAR <= frameAR) {
            frameWidth = (width * frameSize).roundToInt()
            frameHeight = (frameWidth / frameAR).roundToInt()
        } else {
            frameHeight = (height * frameSize).roundToInt()
            frameWidth = (frameHeight * frameAR).roundToInt()
        }
        val frameLeft = (width - frameWidth) / 2
        val frameTop = ((height - frameHeight) * mFrameVerticalBias).roundToInt()
        framingRect = Rect(frameLeft, frameTop, frameLeft + frameWidth, frameTop + frameHeight)
        return framingRect
    }

    fun addPossibleResultPoint(point: ResultPoint) {
        val points = mPossibleResultPoints
        synchronized(points) {
            points.add(point)
            val size = points.size
            if (size > MAX_RESULT_POINTS) {
                points.subList(0, size - MAX_RESULT_POINTS / 2).clear()
            }
        }
    }

    companion object {
        private const val CURRENT_POINT_OPACITY = 0xA0
        private const val ANIMATION_DELAY = 80L
        private const val POINT_SIZE = 6
        private const val MAX_RESULT_POINTS = 20
    }
}