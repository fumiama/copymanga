package top.fumiama.copymangaweb.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.ScrollView

@SuppressLint("ClickableViewAccessibility")
class LazyScrollView : ScrollView {
    private val view: View?
        get() = getChildAt(0)

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    init {
        setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_UP -> this.postDelayed({
                    if (view != null && onScrollListener != null) {
                        //Log.d("MyS", "view?.measuredHeight: ${view?.measuredHeight}, scrollY: $scrollY, height: $height")
                        when {
                            (view?.measuredHeight ?: 0) <= scrollY + height -> onScrollListener?.onBottom()
                            scrollY == 0 -> onScrollListener?.onTop()
                            else -> onScrollListener?.onScroll()
                        }
                    }
                }, 233)
            }
            false
        }
    }
    /**
     * 定义接口
     * @author admin
     */
    interface OnScrollListener {
        fun onBottom()
        fun onTop()
        fun onScroll()
    }
    var onScrollListener: OnScrollListener? = null
}
