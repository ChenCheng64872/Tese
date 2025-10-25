package com.example.energy

import android.content.Context
import java.io.File

/**
 * Minimal, reusable CSV logger:
 *  - Ensures dir/file exist
 *  - Writes header once if file is new/empty
 *  - Thread-safe append
 */
class CsvLogger(
    context: Context,
    fileName: String,
    private val header: String,
    directory: File? = null
) {
    private val file: File

    init {
        val dir = directory ?: (context.getExternalFilesDir(null) ?: context.filesDir)
        if (!dir.exists()) dir.mkdirs()
        file = File(dir, fileName)
        ensureHeader()
    }

    @Synchronized
    fun appendLine(line: String) {
        file.appendText(line)
    }

    fun file(): File = file

    private fun ensureHeader() {
        if (!file.exists() || file.length() == 0L) {
            file.appendText(header.ensureEndsWithNewline())
        }
    }
}

private fun String.ensureEndsWithNewline(): String =
    if (endsWith("\n")) this else this + "\n"
