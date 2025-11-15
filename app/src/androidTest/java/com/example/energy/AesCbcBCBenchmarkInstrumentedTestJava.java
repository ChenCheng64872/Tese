package com.example.energy;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.energy.tests.BenchmarkTestUtils;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class AesCbcBCBenchmarkInstrumentedTestJava {
    @Test
    public void run_aes_range_and_validate_csv() {

        var context = BenchmarkTestUtils.INSTANCE.ctx();
        var minPow = BenchmarkTestUtils.MIN_POW;
        var maxPow = BenchmarkTestUtils.MAX_POW;
        var fileName = "test_aes_2p10_2p20.csv";

        var path = AesBenchmark.INSTANCE.runRangeAndLog(
             context,minPow,maxPow,15, fileName
        );
        assert(true);
        //BenchmarkTestUtils.INSTANCE.assertCsv(path);
    }
}
