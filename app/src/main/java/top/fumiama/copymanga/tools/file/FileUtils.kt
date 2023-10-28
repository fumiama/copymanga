package top.fumiama.copymanga.tools.file

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
}