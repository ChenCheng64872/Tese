package com.example.energy.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.energy.AesGcmBenchmark
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AesGcmBenchmarkInstrumentedTest {
    @Test
    fun runAESGCMRangeAndValidateCsv() {
        val path = AesGcmBenchmark.runRangeAndLog(
            context = BenchmarkTestUtils.ctx(),
            minPow = BenchmarkTestUtils.MIN_POW,
            maxPow = BenchmarkTestUtils.MAX_POW,
            fileName = "test_aesgcm_2p10_2p20.csv"
        )
        BenchmarkTestUtils.assertCsv(path)
    }
}
