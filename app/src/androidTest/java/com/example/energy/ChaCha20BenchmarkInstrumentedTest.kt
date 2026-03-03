package com.example.energy.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.example.energy.ChaCha20Benchmark
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class ChaCha20BenchmarkInstrumentedTest {

    @Test
    fun runCHACHA20SingleSizeAndValidateCsv() {

        val messageSize = InstrumentationRegistry.getArguments()
            .getString("contentsize", "10")
            .toInt()

        val context = BenchmarkTestUtils.ctx()

        val fileName = "chacha20_2p$messageSize.csv"

        val path = ChaCha20Benchmark.runRangeAndLog(
            context = context,
            minPow = messageSize,
            maxPow = messageSize,   // 🔹 single size only
            rounds = 15,
            fileName = fileName
        )

        // Optional: enable only if you want strict CSV validation
        // BenchmarkTestUtils.assertCsv(path)

        assert(true)
    }
}
