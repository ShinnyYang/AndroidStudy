package org.ninetripods.mq.study.anim.fragment

import android.os.Bundle
import android.view.View
import androidx.constraintlayout.motion.widget.MotionLayout
import org.ninetripods.mq.study.R
import org.ninetripods.mq.study.kotlin.base.BaseFragment
import org.ninetripods.mq.study.kotlin.ktx.id

/**
 * MotionLayout 目的地交换动画 Demo。
 * 通过 start/end ConstraintSet 对调左右文案位置。
 */
class MotionLayoutSwapFragment : BaseFragment() {

    private val motionLayout: MotionLayout by id(R.id.motion_layout_swap)
    private var isSwapped = false

    override fun getLayoutId(): Int = R.layout.fragment_motion_city_swap

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        motionLayout.setOnClickListener {
            if (isSwapped) {
                motionLayout.transitionToStart()
            } else {
                motionLayout.transitionToEnd()
            }
            isSwapped = !isSwapped
        }
    }
}
