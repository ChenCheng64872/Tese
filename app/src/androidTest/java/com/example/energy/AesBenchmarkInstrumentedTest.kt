package com.example.energy.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.energy.AesBenchmark
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AesCbcBCBenchmarkInstrumentedTest {
    @Test
    fun run_aes_range_and_validate_csv() {
        val path = AesBenchmark.runRangeAndLog(
            context = BenchmarkTestUtils.ctx(),
            minPow = BenchmarkTestUtils.MIN_POW,
            maxPow = BenchmarkTestUtils.MAX_POW,
            fileName = "test_aes_2p10_2p20.csv"
        )
        BenchmarkTestUtils.assertCsv(path)
    }
}
