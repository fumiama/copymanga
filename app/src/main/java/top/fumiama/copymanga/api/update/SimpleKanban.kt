package top.fumiama.copymanga.api.update

import android.util.Log
import top.fumiama.sdict.io.Client

class SimpleKanban(private val client: Client, private val pwd: String) {   //must run in thread
    private val raw: ByteArray?
        get() {
            var times = 3
            var re: ByteArray
            var firstReceived: ByteArray
            do {
                re = byteArrayOf()
                if(client.initConnect()) {
                    client.sendMessage("${pwd}catquit")
                    client.receiveRawMessage(33)    //Welcome to simple kanban server.
                    try {
                        firstReceived = client.receiveRawMessage(4)     //le
                        val length = convert2Int(firstReceived)
                        Log.d("MySK", "Msg len: $length")
                        if(firstReceived.size > 4) re += firstReceived.copyOfRange(4, firstReceived.size)
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

    suspend fun fetchRaw(doOnLoadFailure: suspend ()->Unit = {
        Log.d("MySD", "Fetch dict failed")
    }, doOnLoadSuccess: suspend (data: ByteArray)->Unit = {
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
                val firstReceive = client.receiveRawMessage(4)
                if(firstReceive.decodeToString() == "null") "null"
                else {
                    val length = convert2Int(firstReceive)
                    Log.d("MySK", "Msg len: $length")
                    var re = byteArrayOf()
                    if(firstReceive.size > 4) re += firstReceive.copyOfRange(4, firstReceive.size)
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