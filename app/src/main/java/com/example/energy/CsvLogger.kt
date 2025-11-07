package com.example.energy

import android.content.Context
import java.io.File

class CsvLogger(
    private val context: Context,
    private val fileName: String,
    private val header: String
) {
    private val file: File by lazy {
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        File(dir, fileName)
    }

    fun path(): String = file.absolutePath

    fun startFresh() {
        file.writeText(header + "\n")
    }

    fun appendLine(line: String) {
        file.appendText(line + "\n")
    }
}
