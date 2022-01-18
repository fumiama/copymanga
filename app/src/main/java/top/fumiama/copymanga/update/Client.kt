package top.fumiama.copymanga.update
//Fumiama 20210601
//Client.kt
import android.util.Log
import java.io.*
import java.lang.Thread.sleep
import java.net.Socket

class Client(private val ip: String, private val port: Int) {
    //普通数据交互接口
    private var sc: Socket? = null

    //普通交互流
    private var dout: OutputStream? = null
    private var din: InputStream? = null

    //已连接标记
    private val isConnect get() = sc != null && din != null && dout != null

    /**
     * 初始化普通交互连接
     */
    fun initConnect(depth: Int = 0): Boolean{
        if(depth > 3) Log.d("MyC", "connect server failed after $depth tries")
        else try {
            sc = Socket(ip, port) //通过socket连接服务器
            din = sc?.getInputStream()  //获取输入流并转换为StreamReader，约定编码格式
            dout = sc?.getOutputStream()    //获取输出流
            sc?.soTimeout = 10000  //设置连接超时限制
            return if (isConnect) {
                Log.d("MyC", "connect server successful")
                true
            } else {
                Log.d("MyC", "connect server failed, now retry...")
                initConnect(depth + 1)
            }
        } catch (e: IOException) {      //获取输入输出流是可能报IOException的，所以必须try-catch
            e.printStackTrace()
        }
        return false
    }

    /**
     * 发送数据至服务器
     * @param message 要发送至服务器的字符串
     */
    fun sendMessage(message: String?): Boolean = sendMessage(message?.toByteArray())

    fun sendMessage(message: ByteArray?): Boolean {
        try {
            if (isConnect) {
                if (message != null) {        //判断输出流或者消息是否为空，为空的话会产生null pointer错误
                    dout?.write(message)
                    dout?.flush()
                    Log.d("MyC", "Send msg: ${message.decodeToString()}")
                    return true
                } else Log.d("MyC", "The message to be sent is empty")
                Log.d("MyC", "send message succeed")
            } else Log.d("MyC", "send message failed: no connect")
        } catch (e: IOException) {
            Log.d("MyC", "send message failed: crash")
            e.printStackTrace()
        }
        return false
    }

    fun read(): Char? = din?.read()?.toChar()

    private var buffer = ByteArrayQueue()
    private val receiveBuffer = ByteArray(65536)

    fun receiveRawMessage(totalSize: Int, setProgress: Boolean = false) : ByteArray {
        if(totalSize == buffer.size) return buffer.popAll()
        else {
            try {
                if (isConnect) {
                    Log.d("MyC", "开始接收服务端信息")
                    while(totalSize > buffer.size) {
                        val count = din?.read(receiveBuffer)?:0
                        if(count > 0) {
                            buffer += receiveBuffer.copyOfRange(0, count)
                            Log.d("MyC", "reply length:$count")
                            if(setProgress && totalSize > 0) progress?.notify(100 * buffer.size / totalSize)
                        } else sleep(10)
                    }
                } else Log.d("MyC", "no connect to receive message")
            } catch (e: IOException) {
                Log.d("MyC", "receive message failed")
                e.printStackTrace()
            }
            return if(totalSize > 0) buffer.pop(totalSize)?:byteArrayOf() else buffer.popAll()
        }
    }

    fun receiveMessage(totalSize: Int) = receiveRawMessage(totalSize).decodeToString()

    /**
     * 关闭连接
     */
    fun closeConnect() = try {
        din?.close()
        dout?.close()
        sc?.close()
        sc = null
        din = null
        dout = null
        true
    } catch (e: IOException) {
        e.printStackTrace()
        false
    }

    var progress: Progress? = null

    interface Progress {
        fun notify(progressPercentage: Int)
    }
}
