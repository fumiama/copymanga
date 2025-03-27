package top.fumiama.copymanga.view

import android.content.Context
import android.util.AttributeSet
import com.akscorp.overscrollablescrollview.OverscrollableNestedScrollView
import kotlinx.android.synthetic.main.app_bar_main.*
import top.fumiama.copymanga.MainActivity.Companion.mainWeakReference

open class OverScrollView :OverscrollableNestedScrollView{
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    override fun isAchieveTop(): Boolean {
        val re = super.isAchieveTop()
        if(re) mainWeakReference?.get()?.appbar?.setExpanded(true)
        return re
    }
}