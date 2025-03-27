package top.fumiama.copymanga.lib

import top.fumiama.copymanga.api.Config
import top.fumiama.copymanga.lib.template.LazyLibrary

class Comandy: LazyLibrary<ComandyMethods>(
    ComandyMethods::class.java, "libcomandy.so", "网络增强",
    Config.net_use_comandy, Config.comandy_version
) {
    companion object {
        val instance = Comandy()
    }
}
