package top.fumiama.copymanga.lib

import com.sun.jna.Library
import com.sun.jna.Pointer

interface ComancryMethods : Library {
    fun decrypt(sd: String, data: Pointer, len: Int): String?
}
