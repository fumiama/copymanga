package top.fumiama.copymangaweb.view

import android.content.Context
import android.util.AttributeSet
import android.widget.ToggleButton

class ChapterToggleButton: ToggleButton {
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int): super(context, attrs, defStyleAttr)
    constructor(context: Context?, attrs: AttributeSet?): super(context, attrs)
    constructor(context: Context?): super(context, null)

    var url: CharSequence? = null
    val hash get() = url?.toString()?.substringAfterLast('/')
    var caption: CharSequence? = null
    var index: Int = 0
    var chapterName: CharSequence = "null"
        set(value) {
            textOn = value
            textOff = value
            text = value
            field = value
        }
}