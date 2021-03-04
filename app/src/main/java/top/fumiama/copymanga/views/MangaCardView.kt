package top.fumiama.copymanga.views

import android.content.Context
import android.util.AttributeSet
import androidx.cardview.widget.CardView
import java.io.File

class MangaCardView:CardView {
    constructor(context: Context): super(context)
    constructor(context: Context, attrs: AttributeSet?): super (context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int): super(context, attrs, defStyleAttr)

    var name = ""
    var append: String? = null
    var headImageUrl: String? = null
    //var uuid: String? = null
    var path: String? = null
    var isFinish = false
    var index = 0
    var chapterUUID: String? = null
    var pageNumber: Int? = null
}