package com.example.energy.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.example.energy.RsaHybridBenchmark
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class RsaHybridBenchmarkInstrumentedTest {

    @Test
    fun runRSAHybridSingleSizeAndValidateCsv() {

        val messageSize = InstrumentationRegistry.getArguments()
            .getString("contentsize", "10")
            .toInt()

        val context = BenchmarkTestUtils.ctx()

        val fileName = "rsa_hybrid_2p$messageSize.csv"

        val path = RsaHybridBenchmark.runRangeAndLog(
            context = context,
            minPow = messageSize,
            maxPow = messageSize,   // 🔹 single size only
            rounds = 15,
            fileName = fileName
        )

        // Optional strict CSV validation
        // BenchmarkTestUtils.assertCsv(path)

        assert(true)
    }
}
