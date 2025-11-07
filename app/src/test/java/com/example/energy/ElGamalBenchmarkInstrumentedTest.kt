package com.example.energy.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.energy.ElGamalBenchmark
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ElGamalBenchmarkInstrumentedTest {
    @Test
    fun run_elgamal_range_and_validate_csv() {
        val path = ElGamalBenchmark.runRangeAndLog(
            context = BenchmarkTestUtils.ctx(),
            minPow = BenchmarkTestUtils.MIN_POW,
            maxPow = BenchmarkTestUtils.MAX_POW,
            fileName = "test_elgamal_2p10_2p20.csv"
        )
        BenchmarkTestUtils.assertCsv(path)
    }
}
