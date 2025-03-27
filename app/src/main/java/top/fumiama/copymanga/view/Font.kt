package top.fumiama.copymanga.view

import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import top.fumiama.copymanga.MainActivity
import top.fumiama.dmzj.copymanga.R

object Font {
    var nisiTypeFace: Typeface? = null
        get() {
            if (field != null) return field
            field = MainActivity.mainWeakReference?.get()?.let {
                ResourcesCompat.getFont(it.applicationContext, R.font.nisi)
            }
            return field
        }
}
