package top.fumiama.copymanga.update

import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
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
            sc?.soTimeout = 2333  //设置连接超时限制
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
    fun sendMessage(message: CharSequence?): Boolean {
        try {
            if (isConnect) {
                if (message != null) {        //判断输出流或者消息是否为空，为空的话会产生nullpoint错误
                    dout?.write(message.toString().toByteArray())
                    dout?.flush()
                    Log.d("MyC", "Send msg: $message")
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

    var buffer = byteArrayOf()

    fun receiveRawMessage(totalSize: Int = -1, bufferSize: Int = 1048576, setProgress: Boolean = false) : ByteArray {
        if(totalSize == buffer.size) {
            val re = buffer
            buffer = byteArrayOf()
            return re
        } else {
            var re = byteArrayOf()
            try {
                if (isConnect) {
                    Log.d("MyC", "开始接收服务端信息")
                    val inMessage = ByteArray(bufferSize)     //设置接受缓冲，避免接受数据过长占用过多内存
                    var a: Int
                    do {
                        a = din?.read(inMessage)?:0 //a存储返回消息的长度
                        if(a > 0) {
                            re += inMessage.copyOf(a)
                            Log.d("MyC", "reply length:$a")
                            if(totalSize < 0 && a < bufferSize) break
                            else if(setProgress && totalSize > 0) progress?.notify(100 * re.size / totalSize)
                        } else break
                    } while (totalSize > re.size)
                } else Log.d("MyC", "no connect to receive message")
            } catch (e: IOException) {
                Log.d("MyC", "receive message failed")
                e.printStackTrace()
            }
            if(totalSize > 0 && re.size > totalSize) {
                Log.d("MyC", "Reduce re size from ${re.size} to $totalSize")
                buffer += re.copyOfRange(totalSize, re.size)
                re = re.copyOf(totalSize)
            } else if(totalSize > 0 && buffer.isNotEmpty()) {
                Log.d("MyC", "Increase re size.")
                buffer += re
                if(buffer.size > totalSize) {
                    re = buffer.copyOf(totalSize)
                    buffer = buffer.copyOfRange(totalSize, buffer.size)
                } else {
                    re = buffer
                    buffer = byteArrayOf()
                }
            } else if(totalSize < 0 && buffer.isNotEmpty()) {
                re = buffer
                buffer = byteArrayOf()
                Log.d("MyC", "clear buffer")
            }
            return re
        }
    }

    //fun receiveMessage() = receiveRawMessage().decodeToString()

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
