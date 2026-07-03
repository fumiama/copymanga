package top.fumiama.copymangaweb.tool

import android.app.Activity
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import kotlin.math.max

object InsetsTools {
    fun applySafeContentInsets(activity: Activity, root: View) {
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)

        val startLeft = root.paddingLeft
        val startTop = root.paddingTop
        val startRight = root.paddingRight
        val startBottom = root.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val displayCutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            val captionBar = insets.getInsets(WindowInsetsCompat.Type.captionBar())

            view.updatePadding(
                left = startLeft + max(systemBars.left, displayCutout.left),
                top = startTop + maxOf(systemBars.top, displayCutout.top, captionBar.top),
                right = startRight + max(systemBars.right, displayCutout.right),
                bottom = startBottom + max(systemBars.bottom, displayCutout.bottom)
            )
            insets
        }

        if (ViewCompat.isAttachedToWindow(root)) {
            ViewCompat.requestApplyInsets(root)
        } else {
            root.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) {
                    v.removeOnAttachStateChangeListener(this)
                    ViewCompat.requestApplyInsets(v)
                }

                override fun onViewDetachedFromWindow(v: View) = Unit
            })
        }
    }
}
