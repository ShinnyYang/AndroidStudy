package org.ninetripods.mq.study.viewpager2.fragment

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.viewpager2.widget.ViewPager2
import org.ninetripods.lib_viewpager2.MVPager2
import org.ninetripods.lib_viewpager2.adapter.OnBannerClickListener
import org.ninetripods.lib_viewpager2.imageLoader.TextLoader
import org.ninetripods.mq.study.R
import org.ninetripods.mq.study.kotlin.base.BaseFragment
import org.ninetripods.mq.study.kotlin.ktx.id
import org.ninetripods.mq.study.kotlin.ktx.showToast
import org.ninetripods.mq.study.widget.SearchHintMarqueeView

/**
 * 搜索框 Hint 上下轮播对比 Demo：
 * 1) ViewPager2（MVPager2）方案
 * 2) ViewFlipper（SearchHintMarqueeView）方案
 */
class SearchVScrollFragment : BaseFragment() {

    private val mMVPager2: MVPager2 by id(R.id.mvp_pager2)
    private val mTvSearch: TextView by id(R.id.tv_search)
    private val mIvScan: ImageView by id(R.id.iv_scan)

    private val mMarqueeView: SearchHintMarqueeView by id(R.id.search_hint_marquee)
    private val mTvSearchMarquee: TextView by id(R.id.tv_search_marquee)
    private val mIvScanMarquee: ImageView by id(R.id.iv_scan_marquee)

    private val hintModels = listOf("猫咪宠物衣服", "年货节，聚划算", "飞猪旅行", "天猫超市")

    override fun getLayoutId(): Int = R.layout.fragment_search_v_scroll

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initVerticalTxScroll()
        initViewFlipperMarquee()
        mTvSearch.setOnClickListener { showToast("点击搜索（ViewPager2）") }
        mIvScan.setOnClickListener { showToast("点击扫描（ViewPager2）") }
        mTvSearchMarquee.setOnClickListener { showToast("点击搜索（ViewFlipper）") }
        mIvScanMarquee.setOnClickListener { showToast("点击扫描（ViewFlipper）") }
    }

    /** 方案一：ViewPager2 + MVPager2 */
    private fun initVerticalTxScroll() {
        mMVPager2.setModels(hintModels)
            .setOrientation(ViewPager2.ORIENTATION_VERTICAL)
            .setUserInputEnabled(false)
            .setPageInterval(2000)
            .setAnimDuration(300)
            .setAutoPlay(true)
            .setLoader(
                TextLoader().setGravity(Gravity.START or Gravity.CENTER_VERTICAL)
                    .setTextColor(R.color.gray_holo_dark).setTextSize(14f)
            )
            .setOnBannerClickListener(object : OnBannerClickListener {
                override fun onItemClick(position: Int) {
                    showToast(hintModels[position])
                }
            })
            .start()
    }

    /** 方案二：ViewFlipper + Animation */
    private fun initViewFlipperMarquee() {
        mMarqueeView
            .setTexts(hintModels)
            .setTextGravity(Gravity.START or Gravity.CENTER_VERTICAL)
            .setTextColor(R.color.gray_holo_dark)
            .setTextSize(14f)
            .setPageInterval(2000)
            .setAutoPlay(true)
            .setOnTextClick { _, text -> showToast(text) }
            .start()
    }
}
