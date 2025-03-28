package top.fumiama.copymanga.lib

import android.util.Log
import com.sun.jna.Memory
import top.fumiama.copymanga.api.Config
import top.fumiama.copymanga.lib.template.LazyLibrary

class Comancry: LazyLibrary<ComancryMethods>(
    ComancryMethods::class.java, "libcomancry.so", "API代理",
    Config.net_use_api_proxy, Config.comancry_version
) {
    private val enabled: Boolean
        get() {
            if (isInInit.get()) {
                Log.d("MyComancry", "$name block enabled for isInInit")
                return false
            }
            return isInUse.value
        }
    val status: String get() = if(enabled) {
        if (isInUse.value) "生效(手动)" else "生效(自动)"
    } else "无效"

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
