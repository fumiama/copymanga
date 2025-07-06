package top.fumiama.copymangaweb.view

import android.animation.ObjectAnimator
import android.view.View
import android.widget.ProgressBar
import androidx.core.animation.doOnEnd
import androidx.databinding.BindingAdapter

object BindingAdapters {
    @JvmStatic
    @BindingAdapter(
        value = ["animateProgress", "animationDuration"],
        requireAll = false
    )
    fun bindAnimateProgress(
        view: ProgressBar,
        progress: Int?,
        duration: Long?
    ) {
        if (progress == null) return
        val dur = duration ?: 1000L
        ObjectAnimator.ofInt(view, "progress", view.progress, progress).apply {
            setDuration(dur)
            start()
        }.doOnEnd {
            if (progress < 100) return@doOnEnd
            view.apply { post { visibility = View.GONE } }
        }
    }
}
