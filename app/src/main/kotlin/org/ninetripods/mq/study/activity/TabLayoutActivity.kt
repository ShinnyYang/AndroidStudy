package org.ninetripods.mq.study.activity

import android.content.res.ColorStateList
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.TooltipCompat
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import org.ninetripods.mq.study.BaseActivity
import org.ninetripods.mq.study.R

class TabLayoutActivity : BaseActivity() {

    private data class TabSectionConfig(
        val title: String,
        val description: String,
        val kind: TabSectionKind,
        val selectedColorRes: Int = R.color.tab_demo_accent,
        val unselectedColorRes: Int = R.color.tab_demo_unselected
    )

    private enum class TabSectionKind {
        BASIC,
        ICON_TEXT,
        ROUND_INDICATOR,
        TEXT_WIDTH_INDICATOR,
        CAPSULE,
        BADGE,
        LONG_SCROLL
    }

    private val sharedTabs = listOf("关注", "推荐", "热门", "数码相机", "汽车", "游戏")
    private val iconResCycle = listOf(
        android.R.drawable.btn_star_big_off,
        android.R.drawable.ic_menu_compass,
        android.R.drawable.ic_menu_today
    )
    private val sections = listOf(
        TabSectionConfig("基础文字 Tabs", "基础文字样式，切换同一套内容区。", TabSectionKind.BASIC),
        TabSectionConfig("图标 + 文字 Tabs", "通过自定义 view 同时展示图标和文字。", TabSectionKind.ICON_TEXT),
        TabSectionConfig("圆角下划线 Indicator", "使用圆角短 indicator 的样式。", TabSectionKind.ROUND_INDICATOR),
        TabSectionConfig("Indicator 与文字同宽", "让 indicator 更贴近文字宽度。", TabSectionKind.TEXT_WIDTH_INDICATOR, R.color.tab_demo_orange),
        TabSectionConfig("胶囊选中态", "圆角容器配合胶囊式选中背景。", TabSectionKind.CAPSULE, R.color.white, R.color.black),
        TabSectionConfig("Badge / 红点", "数字 badge 和纯红点提醒效果。", TabSectionKind.BADGE, R.color.tab_demo_red),
        TabSectionConfig("长标题可滚动 Tabs", "用更长标题展示横向滚动场景。", TabSectionKind.LONG_SCROLL, R.color.tab_demo_purple)
    )

    private val tabLayouts = mutableListOf<TabLayout>()
    private var sharedViewPager: ViewPager2? = null
    private var syncFromTab = false
    private var syncFromPager = false

    override fun setContentView() {
        setContentView(R.layout.activity_tab_layout)
    }

    override fun initViews() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        initToolBar(toolbar, "TabLayout 示例", true, false)

        val pager = findViewById<ViewPager2>(R.id.tabsDemoSharedViewPager)
        val container = findViewById<LinearLayout>(R.id.tabsDemoSectionsContainer)
        sharedViewPager = pager
        pager.adapter = SharedTabsPagerAdapter(sharedTabs)
        pager.offscreenPageLimit = 1

        sections.forEach { config ->
            val sectionView = LayoutInflater.from(this).inflate(R.layout.include_tab_layout_section, container, false)
            container.addView(sectionView)
            bindSection(sectionView, config)
        }

        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (syncFromTab) return
                syncFromPager = true
                tabLayouts.forEach { tabLayout ->
                    val targetPosition = position.coerceAtMost(tabLayout.tabCount - 1)
                    val tab = tabLayout.getTabAt(targetPosition)
                    if (tab != null && !tab.isSelected) {
                        tab.select()
                    }
                    centerSelectedTab(tabLayout, targetPosition)
                    updateTabSelection(tabLayout)
                }
                syncFromPager = false
            }
        })
    }

    private fun bindSection(sectionView: View, config: TabSectionConfig) {
        val titleView = sectionView.findViewById<TextView>(R.id.sectionTitle)
        val descriptionView = sectionView.findViewById<TextView>(R.id.sectionDescription)
        val tabLayout = sectionView.findViewById<TabLayout>(R.id.sectionTabLayout)
        val pager = sharedViewPager ?: return

        titleView.text = config.title
        descriptionView.text = config.description
        tabLayout.setTag(R.id.sectionTabLayout, config)
        tabLayouts += tabLayout

        when (config.kind) {
            TabSectionKind.BASIC -> {
                setupBaseTabLayout(tabLayout, config.selectedColorRes)
                sharedTabs.forEachIndexed { index, title ->
                    tabLayout.addTab(tabLayout.newTab().setText(title), index == pager.currentItem)
                }
            }

            TabSectionKind.ICON_TEXT -> {
                setupBaseTabLayout(tabLayout, config.selectedColorRes)
                tabLayout.setSelectedTabIndicatorHeight(dp(5))
                sharedTabs.forEachIndexed { index, title ->
                    val tab = tabLayout.newTab()
                    tab.customView = createIconTabView(title, resolveTabIcon(index))
                    tabLayout.addTab(tab, index == pager.currentItem)
                }
            }

            TabSectionKind.ROUND_INDICATOR -> {
                setupBaseTabLayout(tabLayout, config.selectedColorRes)
                tabLayout.setSelectedTabIndicator(ContextCompat.getDrawable(this, R.drawable.tab_demo_indicator_round))
                tabLayout.setSelectedTabIndicatorHeight(dp(5))
                listOf("Android", "Kotlin", "Flutter").forEachIndexed { index, title ->
                    tabLayout.addTab(tabLayout.newTab().setText(title), index == pager.currentItem)
                }
            }

            TabSectionKind.TEXT_WIDTH_INDICATOR -> {
                setupBaseTabLayout(tabLayout, config.selectedColorRes)
                tabLayout.setSelectedTabIndicatorHeight(dp(3))
                listOf("短标题", "中等标题", "超长一点的标题").forEachIndexed { index, title ->
                    tabLayout.addTab(tabLayout.newTab().setText(title), index == pager.currentItem)
                }
            }

            TabSectionKind.CAPSULE -> {
                tabLayout.background = ContextCompat.getDrawable(this, R.drawable.bg_tab_demo_container)
                tabLayout.tabMode = TabLayout.MODE_FIXED
                tabLayout.tabGravity = TabLayout.GRAVITY_FILL
                tabLayout.tabRippleColor = null
                tabLayout.isTabIndicatorFullWidth = true
                tabLayout.setSelectedTabIndicator(ContextCompat.getDrawable(this, R.drawable.tab_demo_indicator_capsule))
                tabLayout.setSelectedTabIndicatorHeight(dp(42))
                tabLayout.layoutParams = tabLayout.layoutParams.apply { height = dp(42) }
                tabLayout.setPadding(dp(1), dp(1), dp(1), dp(1))
                tabLayout.setTabTextColors(
                    ContextCompat.getColor(this, R.color.black),
                    ContextCompat.getColor(this, R.color.tab_demo_accent)
                )
                listOf("视频", "直播", "音乐").forEachIndexed { index, title ->
                    tabLayout.addTab(tabLayout.newTab().setText(title), index == pager.currentItem.coerceAtMost(2))
                }
                if (pager.currentItem > 2) {
                    pager.setCurrentItem(0, false)
                }
            }

            TabSectionKind.BADGE -> {
                setupBaseTabLayout(tabLayout, config.selectedColorRes)
                tabLayout.setSelectedTabIndicatorHeight(dp(3))
                listOf("消息", "评论", "点赞", "订阅").forEachIndexed { index, title ->
                    val badgeText = when (index) {
                        0 -> "8"
                        2 -> "99"
                        else -> null
                    }
                    val showDot = index == 1
                    val tab = tabLayout.newTab()
                    tab.customView = createBadgeTabView(title, badgeText, showDot)
                    tabLayout.addTab(tab, index == pager.currentItem)
                }
            }

            TabSectionKind.LONG_SCROLL -> {
                setupBaseTabLayout(tabLayout, config.selectedColorRes)
                tabLayout.setSelectedTabIndicatorHeight(dp(3))
                listOf("数码相机", "新能源汽车", "开放世界游戏", "影视综艺", "人工智能", "极限运动").forEachIndexed { index, title ->
                    tabLayout.addTab(tabLayout.newTab().setText(title), index == pager.currentItem)
                }
            }
        }

        applySectionTabSizing(tabLayout, config.kind)
        disableTabLongPressHints(tabLayout)
        updateTabSelection(tabLayout)

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                centerSelectedTab(tabLayout, tab.position)
                updateTabSelection(tabLayout)
                if (!syncFromPager && pager.currentItem != tab.position) {
                    syncFromTab = true
                    pager.setCurrentItem(tab.position, true)
                    syncFromTab = false
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                updateTabSelection(tabLayout)
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
                centerSelectedTab(tabLayout, tab.position)
                updateTabSelection(tabLayout)
                if (pager.currentItem != tab.position) {
                    pager.setCurrentItem(tab.position, true)
                }
            }
        })

        tabLayout.post {
            val targetPosition = pager.currentItem.coerceAtMost(tabLayout.tabCount - 1)
            centerSelectedTab(tabLayout, targetPosition)
            updateTabSelection(tabLayout)
        }
    }

    private fun setupBaseTabLayout(tabLayout: TabLayout, selectedColorRes: Int) {
        tabLayout.setBackgroundColor(ContextCompat.getColor(this, android.R.color.white))
        tabLayout.tabMode = TabLayout.MODE_SCROLLABLE
        tabLayout.tabGravity = TabLayout.GRAVITY_START

        tabLayout.setSelectedTabIndicatorColor(ContextCompat.getColor(this, selectedColorRes))
        tabLayout.setSelectedTabIndicatorHeight(dp(2))
        tabLayout.setTabTextColors(
            ContextCompat.getColor(this, R.color.tab_demo_unselected),
            ContextCompat.getColor(this, selectedColorRes)
        )
    }

    private fun applySectionTabSizing(tabLayout: TabLayout, kind: TabSectionKind) {
        val slidingTabIndicator = tabLayout.getChildAt(0) as? LinearLayout ?: return
        val minWidth = when (kind) {
            TabSectionKind.BASIC, TabSectionKind.ICON_TEXT, TabSectionKind.BADGE -> dp(72)
            TabSectionKind.ROUND_INDICATOR -> dp(84)
            TabSectionKind.TEXT_WIDTH_INDICATOR -> dp(56)
            TabSectionKind.CAPSULE -> dp(96)
            TabSectionKind.LONG_SCROLL -> dp(50)
        }
        val margin = when (kind) {
            TabSectionKind.CAPSULE -> dp(2)
            TabSectionKind.ICON_TEXT -> dp(4)
            else -> 0
        }
        for (index in 0 until slidingTabIndicator.childCount) {
            val child = slidingTabIndicator.getChildAt(index)
            child.minimumWidth = minWidth
            val params = child.layoutParams as? LinearLayout.LayoutParams ?: continue
            params.marginStart = margin
            params.marginEnd = margin
            child.layoutParams = params
        }
    }

    private fun updateTabSelection(tabLayout: TabLayout) {
        val config = tabLayout.getTag(R.id.sectionTabLayout) as? TabSectionConfig
        val selectedColor = ContextCompat.getColor(this, config?.selectedColorRes ?: R.color.tab_demo_accent)
        val unselectedColor = ContextCompat.getColor(this, config?.unselectedColorRes ?: R.color.tab_demo_unselected)
        for (index in 0 until tabLayout.tabCount) {
            val tab = tabLayout.getTabAt(index) ?: continue
            val selected = tab.isSelected
            val customView = tab.customView
            if (customView != null) {
                val textView = customView.findViewById<TextView>(R.id.tabText)
                textView.setTextColor(if (selected) selectedColor else unselectedColor)
                textView.textSize = if (selected) 16f else 14f
                textView.setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)

                val iconView = customView.findViewById<ImageView?>(R.id.tabIcon)
                iconView?.imageTintList = ColorStateList.valueOf(if (selected) selectedColor else unselectedColor)
                continue
            }

            val textView = findTabTextView(tab.view) ?: continue
            textView.textSize = if (selected) 16f else 14f
            textView.setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)
            textView.setTextColor(if (selected) selectedColor else unselectedColor)
        }
    }

    private fun disableTabLongPressHints(tabLayout: TabLayout) {
        val slidingTabIndicator = tabLayout.getChildAt(0) as? ViewGroup ?: return
        for (index in 0 until slidingTabIndicator.childCount) {
            val tabView = slidingTabIndicator.getChildAt(index)
            tabView.contentDescription = null
            tabView.isLongClickable = false
            TooltipCompat.setTooltipText(tabView, null)
            tabView.findViewById<View?>(R.id.tabText)?.contentDescription = null
        }
    }

    private fun createIconTabView(title: String, iconRes: Int): View {
        return LayoutInflater.from(this)
            .inflate(R.layout.item_tab_layout_tab, null, false)
            .apply {
                findViewById<TextView>(R.id.tabText).text = title
                findViewById<ImageView>(R.id.tabIcon).setImageResource(iconRes)
            }
    }

    private fun createBadgeTabView(title: String, badgeText: String?, showDot: Boolean): View {
        return LayoutInflater.from(this)
            .inflate(R.layout.item_tab_layout_badge_tab, null, false)
            .apply {
                findViewById<TextView>(R.id.tabText).text = title
                val badgeView = findViewById<TextView>(R.id.tabBadge)
                val dotView = findViewById<View>(R.id.tabDot)
                if (badgeText.isNullOrEmpty()) {
                    badgeView.visibility = View.GONE
                } else {
                    badgeView.text = badgeText
                    badgeView.visibility = View.VISIBLE
                }
                dotView.visibility = if (showDot) View.VISIBLE else View.GONE
            }
    }

    private fun resolveTabIcon(position: Int): Int = iconResCycle[position % iconResCycle.size]

    private fun centerSelectedTab(tabLayout: TabLayout, position: Int) {
        val slidingTabIndicator = tabLayout.getChildAt(0) as? ViewGroup ?: return
        val tabView = slidingTabIndicator.getChildAt(position) ?: return
        val targetScrollX = tabView.left - (tabLayout.width - tabView.width) / 2
        tabLayout.smoothScrollTo(targetScrollX.coerceAtLeast(0), 0)
    }

    private fun findTabTextView(view: View): TextView? {
        if (view is TextView) return view
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                val textView = findTabTextView(view.getChildAt(index))
                if (textView != null) return textView
            }
        }
        return null
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private class SharedTabsPagerAdapter(
        private val titles: List<String>
    ) : RecyclerView.Adapter<SharedTabsPagerAdapter.PageViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_tab_layout_page, parent, false)
            return PageViewHolder(view)
        }

        override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
            holder.bind(titles[position])
        }

        override fun getItemCount(): Int = titles.size

        class PageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            fun bind(tabTitle: String) {
                itemView.findViewById<TextView>(R.id.tabsPageTitle).text = tabTitle
                itemView.findViewById<TextView>(R.id.tabsPageDescription).text =
                    "这是共享内容区中的“$tabTitle”页面，所有示例 TabLayout 都会联动到这里。"
            }
        }
    }
}
