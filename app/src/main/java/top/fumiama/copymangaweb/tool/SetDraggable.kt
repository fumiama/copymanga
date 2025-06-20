package top.fumiama.copymangaweb.tool

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

class SetDraggable {
    private var screenWidth = 0
    private var screenHeight = 0
    fun with(context: Context): SetDraggable {
        val dm = context.resources.displayMetrics
        screenWidth = dm.widthPixels
        screenHeight = dm.heightPixels
        return this
    }

    @SuppressLint("ClickableViewAccessibility")
    fun onto(target: View) {
        var lastX = 0
        var lastY = 0
        var firstX = 0
        var firstY = 0
        target.post { target.setOnTouchListener { v: View, event: MotionEvent ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = event.rawX.toInt()
                    lastY = event.rawY.toInt()
                    firstX = lastX
                    firstY = lastY
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX.toInt() - lastX
                    val dy = event.rawY.toInt() - lastY
                    var left = v.left + dx
                    var top = v.top + dy
                    var right = v.right + dx
                    var bottom = v.bottom + dy
                    if (left < 0) {
                        left = 0
                        right = left + v.width
                    }
                    if (right > screenWidth) {
                        right = screenWidth
                        left = right - v.width
                    }
                    if (top < 0) {
                        top = 0
                        bottom = top + v.height
                    }
                    if (bottom > screenHeight) {
                        bottom = screenHeight
                        top = bottom - v.height
                    }
                    v.layout(left, top, right, bottom)
                    lastX = event.rawX.toInt()
                    lastY = event.rawY.toInt()
                }
            }
            abs(firstX - lastX) > 3 || abs(firstY - lastY) > 3      // 移动微小则判断为点击
        } }
    }
}