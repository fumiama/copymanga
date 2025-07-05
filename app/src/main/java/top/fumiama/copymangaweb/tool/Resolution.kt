package top.fumiama.copymangaweb.tool

class Resolution(private val original: Regex) {
    fun wrap(u: String) : String = u.replace(original, "c1500x.")
}
