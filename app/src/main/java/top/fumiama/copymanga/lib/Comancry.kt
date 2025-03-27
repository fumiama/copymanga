package top.fumiama.copymanga.lib

import android.util.Log
import com.sun.jna.Memory
import top.fumiama.copymanga.api.Config
import top.fumiama.copymanga.lib.template.LazyLibrary

class Comancry: LazyLibrary<ComancryMethods>(
    ComancryMethods::class.java, "libcomancry.so", "API代理",
    Config.net_use_api_proxy, Config.comancry_version
) {
    suspend fun decrypt(sd: String, data: ByteArray): String? {
        // 将 ByteArray 转换为 char*
        val nativeMemory = Memory(data.size.toLong())
        nativeMemory.write(0, data, 0, data.size) // 将 byteArray 写入内存
        Log.d("MyComancry", "get data len ${data.size}")
        return getInstance()?.decrypt(sd, nativeMemory, data.size)
    }
    companion object {
        val instance = Comancry()
    }
}
