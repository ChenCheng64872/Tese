package com.example.energy

import android.content.Context
import java.io.File

class CsvLogger(
    private val context: Context,
    private val fileName: String,
    private val header: String,
    private val subDir: String? = null
) {
    private val file: File by lazy {
        val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
        val finalDir = if (subDir != null) File(baseDir, subDir) else baseDir
        if (!finalDir.exists()) finalDir.mkdirs()
        File(finalDir, fileName)
    }

    fun path(): String = file.absolutePath

    fun startFresh() {
        file.writeText(header + "\n")
    }

    fun appendLine(line: String) {
        file.appendText(line + "\n")
    }
}
