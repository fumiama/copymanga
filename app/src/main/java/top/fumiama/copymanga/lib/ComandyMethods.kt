package top.fumiama.copymanga.lib

import com.sun.jna.Library

interface ComandyMethods : Library {
    // fun add_dns(para: String?, is_ipv6: Int): String?

    fun request(para: String): String?

    fun progress(para: String): Int
}
