package com.example.energy.tests

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.energy.BENCH_CSV_HEADER
import java.io.File

object BenchmarkTestUtils {
    const val MIN_POW = 10
    const val MAX_POW = 20
    private const val EXPECTED_DATA_ROWS = (MAX_POW - MIN_POW) + 1 // 11

    fun ctx(): Context = ApplicationProvider.getApplicationContext()

    fun assertCsv(path: String) {
        val f = File(path)
        require(f.exists() && f.isFile) { "CSV not found at $path" }

        val lines = f.readLines().filter { it.isNotBlank() }
        require(lines.isNotEmpty()) { "CSV is empty" }

        // header
        require(lines.first() == BENCH_CSV_HEADER) {
            "CSV header mismatch.\nExpected: $BENCH_CSV_HEADER\nGot: ${lines.first()}"
        }

        // rows count (header + 11)
        require(lines.size == EXPECTED_DATA_ROWS + 1) {
            "Expected $EXPECTED_DATA_ROWS data rows, got ${lines.size - 1}"
        }

        // 10 columns per row
        lines.drop(1).forEachIndexed { idx, row ->
            val cols = row.split(',')
            require(cols.size == 10) { "Row #$idx has ${cols.size} columns (expected 10): $row" }
        }

        // first and last sizes
        val firstSize = lines[1].substringBefore(',').toInt()
        val lastSize  = lines.last().substringBefore(',').toInt()
        require(firstSize == 1024) { "First size_bytes should be 1024, got $firstSize" }
        require(lastSize == 1_048_576) { "Last size_bytes should be 1048576, got $lastSize" }
    }
}
