package top.fumiama.copymanga.storage

import java.io.File

object FileUtils {
    fun recursiveRemove(f: File) {
        if (f.isDirectory) f.listFiles()?.let {
            for (i in it)
                if (i.isDirectory) recursiveRemove(i)
                else i.delete()
        }
        f.delete()
    }
    fun sizeOf(f: File): Long{
        var size = 0L
        if (f.isDirectory) f.listFiles()?.apply {
            for (i in this)
                size += if (i.isDirectory) sizeOf(i) else i.length()
        }
        return size
    }
}