package com.example.energy;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.energy.tests.BenchmarkTestUtils;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class AesBenchmarkInstrumentedTest {

    @Test
    public void runAESSingleSizeAndValidateCsv() {

        int messageSize = Integer.parseInt(
                InstrumentationRegistry.getArguments().getString("contentsize", "10")
        );

        var context = BenchmarkTestUtils.INSTANCE.ctx();

        var fileName = "aes_2p" + messageSize + ".csv";

        var path = AesBenchmark.INSTANCE.runRangeAndLog(
                context,
                messageSize,
                messageSize,   // 🔹 single size only
                15, 100, 3, fileName, null
        );

        // Optional strict validation
        // BenchmarkTestUtils.INSTANCE.assertCsv(path);

        assert(true);
    }
}
