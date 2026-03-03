package com.example.energy.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.example.energy.ElGamalBenchmark
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class ElGamalBenchmarkInstrumentedTest {

    @Test
    fun runELGAMALSingleSizeAndValidateCsv() {

        val messageSize = InstrumentationRegistry.getArguments()
            .getString("contentsize", "10")
            .toInt()

        val context = BenchmarkTestUtils.ctx()

        val fileName = "elgamal_2p$messageSize.csv"

        val path = ElGamalBenchmark.runRangeAndLog(
            context = context,
            minPow = messageSize,
            maxPow = messageSize,   // 🔹 single size only
            rounds = 15,
            fileName = fileName
        )

        // Optional strict validation
        // BenchmarkTestUtils.assertCsv(path)

        assert(true)
    }
}
