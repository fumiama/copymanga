package top.fumiama.copymanga.manga

class Book(val pathWord: String, val readLocally: Boolean = false) {
    /**
     * 更新云端最新信息
     */
    suspend fun update() {

    }
}