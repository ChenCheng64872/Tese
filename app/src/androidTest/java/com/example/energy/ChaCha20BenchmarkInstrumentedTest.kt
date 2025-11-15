package com.example.energy.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.energy.ChaCha20Benchmark
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChaCha20BenchmarkInstrumentedTest {
    @Test
    fun run_chacha20_range_and_validate_csv() {
        val path = ChaCha20Benchmark.runRangeAndLog(
            context = BenchmarkTestUtils.ctx(),
            minPow = BenchmarkTestUtils.MIN_POW,
            maxPow = BenchmarkTestUtils.MAX_POW,
            fileName = "test_chacha20_2p10_2p20.csv"
        )
        BenchmarkTestUtils.assertCsv(path)
    }
}
