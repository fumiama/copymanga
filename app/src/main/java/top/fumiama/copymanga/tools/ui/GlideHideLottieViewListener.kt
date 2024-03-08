package top.fumiama.copymanga.tools.ui

import android.graphics.drawable.Drawable
import android.view.View
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import java.lang.ref.WeakReference

class GlideHideLottieViewListener(private val wla: WeakReference<LottieAnimationView>, private val runAfterLoad: (() -> Unit)? = null): RequestListener<Drawable> {
    override fun onLoadFailed(
        e: GlideException?,
        model: Any?,
        target: Target<Drawable>,
        isFirstResource: Boolean
    ): Boolean {
        return false
    }

    override fun onResourceReady(
        resource: Drawable,
        model: Any,
        target: Target<Drawable>?,
        dataSource: DataSource,
        isFirstResource: Boolean
    ): Boolean {
        wla.get()?.apply {
            pauseAnimation()
            visibility = View.GONE
            runAfterLoad?.let { it() }
        }
        return false
    }
}