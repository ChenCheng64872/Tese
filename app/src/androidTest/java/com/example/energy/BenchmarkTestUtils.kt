package com.example.energy.tests
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.energy.BENCH_EXEC_CSV_HEADER
import java.io.File
object BenchmarkTestUtils {
    const val MIN_POW=10
    const val MAX_POW=20
    fun ctx():Context=ApplicationProvider.getApplicationContext()
    fun assertCsv(path:String) {
        val rows=File(path).readLines()
        check(rows.size>1 && rows.first()==BENCH_EXEC_CSV_HEADER)
        val width=rows.first().split(',').size
        rows.drop(1).forEach { check(it.split(',').size==width) }
    }
}
