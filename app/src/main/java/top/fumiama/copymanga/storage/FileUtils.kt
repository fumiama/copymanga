package top.fumiama.copymanga.storage

import android.content.Context
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

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
    fun registerZipExportLauncher(
        fragment: Fragment,
        callback: (Uri?) -> Unit
    ): ActivityResultLauncher<String> {
        return fragment.registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip"), callback)
    }
    fun compressToUserFile(
        context: Context,
        sourceDir: File,
        targetUri: Uri,
        setProgress: (Int) -> Unit = {}
    ) {
        val allFiles = collectAllFiles(sourceDir)
        val totalFiles = allFiles.size
        var filesDone = 0

        setProgress(0) // 确保初始进度为0%

        context.contentResolver.openOutputStream(targetUri)?.use { outputStream ->
            ZipOutputStream(BufferedOutputStream(outputStream)).use { zipOut ->
                for (file in allFiles) {
                    val relativePath = sourceDir.toURI().relativize(file.toURI()).path
                    val entry = ZipEntry(relativePath)
                    zipOut.putNextEntry(entry)

                    FileInputStream(file).use { input ->
                        input.copyTo(zipOut)
                    }

                    zipOut.closeEntry()
                    filesDone++
                    val progress = (filesDone * 100 / totalFiles).coerceIn(0, 100)
                    setProgress(progress)
                }
            }
        }

        setProgress(100) // 确保最终进度为100%
    }

    private fun collectAllFiles(root: File): List<File> {
        val files = mutableListOf<File>()
        root.walkTopDown().forEach {
            if (it.isFile) files.add(it)
        }
        return files
    }

}