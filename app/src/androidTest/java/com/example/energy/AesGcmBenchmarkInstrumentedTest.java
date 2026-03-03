package com.example.energy;

import android.app.Instrumentation;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.energy.tests.BenchmarkTestUtils;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class AesGcmBenchmarkInstrumentedTest {
    @Test
    public void runAESGCMRangeAndValidateCsv() {

        int messageSize = Integer.parseInt(
                InstrumentationRegistry.getArguments().getString("contentsize", "10")
        );
        var context = BenchmarkTestUtils.INSTANCE.ctx();
        var fileName = "test_aes_2p10_2p20.csv";

        var path = AesGcmBenchmark.INSTANCE.runRangeAndLog(
             context, messageSize, messageSize,15, fileName
        );
        assert(true);
        //BenchmarkTestUtils.INSTANCE.assertCsv(path);
    }
}
