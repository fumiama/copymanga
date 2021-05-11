package top.fumiama.copymanga.update

import android.util.Log

class SimpleKanban(private val client: Client, private val pwd: String) {   //must run in thread
    private val raw: ByteArray?
        get() {
            var times = 3
            var re: ByteArray
            var firstRecv: ByteArray
            do {
                re = byteArrayOf()
                if(client.initConnect()) {
                    client.sendMessage("${pwd}catquit")
                    client.receiveRawMessage(33)    //Welcome to simple kanban server.
                    try {
                        firstRecv = client.receiveRawMessage(4)     //le
                        val length = convert2Int(firstRecv)
                        Log.d("MySK", "Msg len: $length")
                        if(firstRecv.size > 4) re += firstRecv.copyOfRange(4, firstRecv.size)
                        re += client.receiveRawMessage(length - re.size, setProgress = true)
                        break
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    client.closeConnect()
                }
            } while (times-- > 0)
            return if(re.isEmpty()) null else re
        }

    private fun convert2Int(buffer: ByteArray) =
            (buffer[3].toInt() and 0xff shl 24) or
            (buffer[2].toInt() and 0xff shl 16) or
            (buffer[1].toInt() and 0xff shl 8) or
            (buffer[0].toInt() and 0xff)

    fun fetchRaw(doOnLoadFailure: ()->Unit = {
        Log.d("MySD", "Fetch dict failed")
    }, doOnLoadSuccess: (data: ByteArray)->Unit = {
        Log.d("MySD", "Fetch dict success")
    }) {
        raw?.apply {
            doOnLoadSuccess(this)
        }?:doOnLoadFailure()
    }

    operator fun get(version: Int): String =
        if(client.initConnect()) {
            client.sendMessage("${pwd}get${version}quit")
            client.receiveRawMessage(36)             //Welcome to simple kanban server. get
            val r = try {
                val firstRecv = client.receiveRawMessage(4)
                if(firstRecv.decodeToString() == "null") "null"
                else {
                    val length = convert2Int(firstRecv)
                    Log.d("MySK", "Msg len: $length")
                    var re = byteArrayOf()
                    if(firstRecv.size > 4) re += firstRecv.copyOfRange(4, firstRecv.size)
                    re += client.receiveRawMessage(length - re.size)
                    if(re.isNotEmpty()) re.decodeToString() else "null"
                }
            } catch (e: Exception){
                e.printStackTrace()
                "null"
            }
            client.closeConnect()
            r
        } else "null"
}