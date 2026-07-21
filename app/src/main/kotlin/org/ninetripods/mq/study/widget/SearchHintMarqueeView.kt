package org.ninetripods.mq.study.widget

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.ViewFlipper
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import org.ninetripods.mq.study.R

/**
 * 搜索框 Hint 上下轮播。
 * 基于 [ViewFlipper] + in/out Animation，适合轻量文案切换。
 */
class SearchHintMarqueeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : ViewFlipper(context, attrs) {

    private val items = mutableListOf<String>()
    private var autoPlay = true
    private var intervalMs = 3000
    private var textGravity: Int = Gravity.CENTER_VERTICAL
    private var textSizeSp: Float = 14f

    @ColorRes
    private var textColorRes: Int = R.color.black
    private var onTextClick: ((Int, String) -> Unit)? = null

    init {
        inAnimation = AnimationUtils.loadAnimation(context, R.anim.search_hint_marquee_in)
        outAnimation = AnimationUtils.loadAnimation(context, R.anim.search_hint_marquee_out)
        flipInterval = intervalMs
        isAutoStart = false
        measureAllChildren = false
    }

    fun setTexts(texts: List<String>): SearchHintMarqueeView {
        items.clear()
        items.addAll(texts.filter { it.isNotBlank() })
        return this
    }

    fun setTextGravity(gravity: Int): SearchHintMarqueeView {
        textGravity = gravity
        return this
    }

    fun setTextSize(sp: Float): SearchHintMarqueeView {
        textSizeSp = sp
        return this
    }

    fun setTextColor(@ColorRes colorRes: Int): SearchHintMarqueeView {
        textColorRes = colorRes
        return this
    }

    fun setPageInterval(interval: Int): SearchHintMarqueeView {
        intervalMs = interval
        flipInterval = intervalMs
        return this
    }

    fun setAutoPlay(enabled: Boolean): SearchHintMarqueeView {
        autoPlay = enabled
        if (!enabled && isFlipping) {
            stopFlipping()
        }
        return this
    }

    fun setOnTextClick(listener: ((Int, String) -> Unit)?): SearchHintMarqueeView {
        onTextClick = listener
        return this
    }

    fun start() {
        ensureChildrenBuilt()
        if (items.isEmpty()) {
            visibility = GONE
            if (isFlipping) stopFlipping()
            return
        }
        visibility = VISIBLE
        displayedChild = displayedChild.coerceIn(0, items.lastIndex)
        if (shouldFlip()) {
            startFlippingSafely()
        } else if (isFlipping) {
            stopFlipping()
        }
    }

    fun getCurrentRealPosition(): Int {
        if (items.isEmpty()) return 0
        return displayedChild.coerceIn(0, items.lastIndex)
    }

    fun pauseAutoPlay() {
        if (isFlipping) {
            stopFlipping()
        }
    }

    fun resumeAutoPlay() {
        if (shouldFlip() && isShown) {
            startFlippingSafely()
        }
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (VISIBLE == visibility && shouldFlip()) {
            startFlippingSafely()
        } else if (GONE == visibility || INVISIBLE == visibility) {
            if (isFlipping) {
                stopFlipping()
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (shouldFlip()) {
            startFlippingSafely()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (isFlipping) {
            stopFlipping()
        }
    }

    private fun ensureChildrenBuilt() {
        removeAllViews()
        items.forEachIndexed { index, text ->
            addView(createItemView(index, text))
        }
        visibility = if (items.isEmpty()) GONE else VISIBLE
        if (items.isNotEmpty() && displayedChild > items.lastIndex) {
            displayedChild = 0
        }
    }

    private fun createItemView(index: Int, text: String): FrameLayout {
        val container = FrameLayout(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT,
            )
        }
        val textView = TextView(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.MATCH_PARENT,
            )
            gravity = textGravity
            setTextColor(ContextCompat.getColor(context, textColorRes))
            textSize = textSizeSp
            maxLines = 1
            this.text = text
            setOnClickListener {
                onTextClick?.invoke(index, text)
            }
        }
        container.addView(textView)
        return container
    }

    private fun shouldFlip(): Boolean = autoPlay && items.size > 1

    private fun startFlippingSafely() {
        if (!isFlipping) {
            startFlipping()
        }
    }
}
