package top.fumiama.copymanga.lib

import android.util.Log
import top.fumiama.copymanga.api.Config
import top.fumiama.copymanga.lib.template.LazyLibrary

class Comandy: LazyLibrary<ComandyMethods>(
    ComandyMethods::class.java, "libcomandy.so", "网络增强",
    Config.net_use_comandy, Config.comandy_version
) {
    val enabled: Boolean
        get() {
            if (isInInit.get()) {
                Log.d("MyComandy", "$name block enabled for isInInit")
                return false
            }
            return isInUse.value
        }
    val status: String get() = if(enabled) {
        if (isInUse.value) "生效(手动)" else "生效(自动)"
    } else "无效"

    companion object {
        val instance = Comandy()
    }
}
