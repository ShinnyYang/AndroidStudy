package org.ninetripods.mq.study.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Shader
import android.text.Layout
import android.util.AttributeSet
import android.view.Gravity
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.withStyledAttributes
import org.ninetripods.mq.study.R

class GradientTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    companion object {
        const val HORIZONTAL = 1
        const val VERTICAL = 2

        // 未显式传色值时使用的默认渐变色。
        private val DEFAULT_COLORS = intArrayOf(0xFFFF7A18.toInt(), 0xFFFF3D81.toInt())
    }

    private data class LineShaderInfo(
        val top: Float,
        val bottom: Float,
        val shader: Shader
    )
    private var textShader: Shader? = null // 单行文本场景下缓存的 shader，避免重复创建。
    private var lineShaderInfos: List<LineShaderInfo> = emptyList() // 多行文本场景下缓存的每行 shader 与裁剪区域。
    private var widthChanged = true // 文本内容、尺寸或方向变化后，标记需要重建 shader。
    private var colors: IntArray? = DEFAULT_COLORS // 当前实际参与绘制的渐变色集合。
    private var gradientOrientation = HORIZONTAL // 渐变方向，决定颜色沿 X 轴还是 Y 轴分布

    init {
        if (attrs != null) {
            //获取start、center(如有)、end对应的颜色
            context.withStyledAttributes(attrs, R.styleable.GradientTextView, defStyleAttr, 0) {
                gradientOrientation = getInt(R.styleable.GradientTextView_gradientOrientation, gradientOrientation)
                if (
                    hasValue(R.styleable.GradientTextView_gradientStartColor) &&
                    hasValue(R.styleable.GradientTextView_gradientEndColor)
                ) {
                    val start = getColor(R.styleable.GradientTextView_gradientStartColor, 0)
                    val end = getColor(R.styleable.GradientTextView_gradientEndColor, 0)
                    colors = if (hasValue(R.styleable.GradientTextView_gradientCenterColor)) {
                        intArrayOf(start, getColor(R.styleable.GradientTextView_gradientCenterColor, 0), end)
                    } else {
                        intArrayOf(start, end)
                    }
                }
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        widthChanged = true
    }

    override fun onTextChanged(text: CharSequence?, start: Int, lengthBefore: Int, lengthAfter: Int) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter)
        widthChanged = true
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        val gradientColors = colors
        if (gradientColors == null || gradientColors.isEmpty()) {
            paint.shader = null
            super.onDraw(canvas)
            return
        }

        val textLayout = layout
        if (textLayout == null || textLayout.lineCount <= 1) {
            // 单行时复用同一个 shader，避免每次 onDraw 都重新创建。
            if (widthChanged || textShader == null) {
                textShader = createSingleLineShader(gradientColors)
                lineShaderInfos = emptyList()
                widthChanged = false
            }
            paint.shader = textShader
            super.onDraw(canvas)
            return
        }

        // 多行文本要按每一行自己的起点和宽度重建渐变，Shader.TileMode.REPEAT 无法稳定覆盖这种场景，比如在设置lineSpace等场景下。
        if (widthChanged || lineShaderInfos.size != textLayout.lineCount) {
            lineShaderInfos = buildMultiLineShaderInfos(textLayout, gradientColors)
            textShader = null
            widthChanged = false
        }
        drawMultiLineGradientText(canvas, lineShaderInfos)
    }

    /**
     * 预先计算多行文本每一行对应的 shader 与上下边界，供 onDraw 直接复用。
     */
    private fun buildMultiLineShaderInfos(textLayout: Layout, gradientColors: IntArray): List<LineShaderInfo> {
        val shaderInfos = ArrayList<LineShaderInfo>(textLayout.lineCount)
        // 文本整体可能有垂直居中或底部对齐偏移，这里先统一算出来。
        val verticalOffset = resolveVerticalOffset(textLayout)

        for (lineIndex in 0 until textLayout.lineCount) {
            val lineStart = textLayout.getLineStart(lineIndex)
            val lineEnd = textLayout.getLineVisibleEnd(lineIndex)
            // 空行没有可见字符，不需要创建 shader。
            if (lineStart >= lineEnd) {
                continue
            }

            // 计算当前行真实的左右/上下边界，用来限定渐变范围和裁剪区域。
            val lineLeft = compoundPaddingLeft + textLayout.getLineLeft(lineIndex)
            val lineTop = extendedPaddingTop + verticalOffset + textLayout.getLineTop(lineIndex).toFloat()
            val lineBottom = extendedPaddingTop + verticalOffset + textLayout.getLineBottom(lineIndex).toFloat()
            // 优先使用 Layout 提供的行宽，取不到时再回退到 paint 实测宽度。
            val lineWidth = textLayout.getLineWidth(lineIndex).takeIf { it > 0f } ?: paint.measureText(textLayout.text, lineStart, lineEnd)
            val lineHeight = (lineBottom - lineTop).takeIf { it > 0f } ?: textSize

            // 缓存当前行的 shader 和绘制边界，后续 onDraw 只负责复用。
            shaderInfos.add(
                LineShaderInfo(
                    top = lineTop,
                    bottom = lineBottom,
                    shader = createLineShader(gradientColors, lineLeft, lineTop, lineWidth, lineHeight))
            )
        }
        return shaderInfos
    }

    @SuppressLint("WrongCall")
    private fun drawMultiLineGradientText(canvas: Canvas, shaderInfos: List<LineShaderInfo>) {
        val saveCount = canvas.save()

        for (shaderInfo in shaderInfos) {
            // 每一行单独创建 shader，并只裁剪当前行区域绘制。
            paint.shader = shaderInfo.shader
            canvas.save()
            canvas.clipRect(0f, shaderInfo.top, width.toFloat(), shaderInfo.bottom)
            super.onDraw(canvas)
            canvas.restore()
        }
        paint.shader = null
        canvas.restoreToCount(saveCount)
    }

    private fun createSingleLineShader(gradientColors: IntArray): Shader? {
        val textWidth = paint.measureText(text.toString()).takeIf { it > 0f } ?: return null
        return if (gradientOrientation == HORIZONTAL) {
            LinearGradient(0f, 0f, textWidth, 0f, gradientColors, null, Shader.TileMode.CLAMP)
        } else {
            LinearGradient(0f, 0f, 0f, textSize, gradientColors, null, Shader.TileMode.CLAMP)
        }
    }

    private fun createLineShader(
        gradientColors: IntArray,
        lineLeft: Float,
        lineTop: Float,
        lineWidth: Float,
        lineHeight: Float
    ): Shader {
        return if (gradientOrientation == HORIZONTAL) {
            LinearGradient(lineLeft, 0f, lineLeft + lineWidth, 0f, gradientColors, null, Shader.TileMode.CLAMP)
        } else {
            LinearGradient(0f, lineTop, 0f, lineTop + lineHeight, gradientColors, null, Shader.TileMode.CLAMP)
        }
    }

    /**
     * 计算文本整体在 TextView 内容区域内需要补上的垂直偏移量。
     */
    private fun resolveVerticalOffset(textLayout: Layout): Float {
        // 可用高度要扣掉上下 padding，和 Layout 实际绘制区域保持一致。
        val availableHeight = height - compoundPaddingTop - compoundPaddingBottom
        val textHeight = textLayout.height
        // 文本已经占满或超过可用高度时，不需要再额外偏移。
        if (availableHeight <= textHeight) {
            return 0f
        }
        // 当 TextView 设置了垂直居中或底部对齐时，手动补上文本整体偏移。
        return when (gravity and Gravity.VERTICAL_GRAVITY_MASK) {
            Gravity.BOTTOM -> (availableHeight - textHeight).toFloat()
            Gravity.CENTER_VERTICAL -> (availableHeight - textHeight) / 2f
            // 顶部对齐或未设置垂直重力时，保持从内容区顶部开始绘制。
            else -> 0f
        }
    }

    fun setGradientColors(@ColorInt vararg colorValues: Int) {
        colors = if (colorValues.isNotEmpty()) colorValues else null
        widthChanged = true
        invalidate()
    }

    fun setGradientOrientation(orientation: Int) {
        gradientOrientation = orientation
        widthChanged = true
        invalidate()
    }

    fun setColors(colorValues: IntArray) {
        colors = colorValues
        widthChanged = true
        invalidate()
    }
}
