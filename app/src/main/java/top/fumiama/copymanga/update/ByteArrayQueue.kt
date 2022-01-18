package top.fumiama.copymanga.update
//Fumiama 20210601
//ByteArrayQueue.kt
//FIFO队列
class ByteArrayQueue {
    private var elements = byteArrayOf()
    val size get() = elements.size
    fun append(items: ByteArray) {
        elements += items
    }
    fun pop(num: Int = 1): ByteArray? {
        return if(num <= elements.size) {
            val re = elements.copyOfRange(0, num)
            elements = elements.copyOfRange(num, elements.size)
            re
        } else null
    }
    fun clear() {
        elements = byteArrayOf()
    }
    fun popAll(): ByteArray {
        val re = elements
        clear()
        return re
    }
    operator fun plusAssign(items: ByteArray) = append(items)
}