package com.example.energy

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private val executor by lazy { Executors.newSingleThreadExecutor() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btAes  = findViewById<Button>(R.id.btVar1) // AES benchmark
        val btCha  = findViewById<Button>(R.id.btVar2) // ChaCha20 benchmark
        val btElg  = findViewById<Button>(R.id.btVar3) // ElGamal benchmark
        val btGcm  = findViewById<Button>(R.id.btVar4) // AES-GCM benchmark
        val btRsa  = findViewById<Button>(R.id.btVar5) // RSA (hybrid) benchmark

        btAes.setOnClickListener {
            runWithUiLock(btAes, "Running AES benchmarks (2^10 → 2^20, 15 rounds)…") {
                val path = AesBenchmark.runRangeAndLog(
                    context = this,
                    minPow = 10,
                    maxPow = 20,
                    fileName = "aes_bench_2p10_2p20.csv"
                )
                "AES done. CSV at:\n$path"
            }
        }

        btCha.setOnClickListener {
            runWithUiLock(btCha, "Running ChaCha20 benchmarks (2^10 → 2^20, 15 rounds)…") {
                val path = ChaCha20Benchmark.runRangeAndLog(
                    context = this,
                    minPow = 10,
                    maxPow = 20,
                    fileName = "chacha_bench_2p10_2p20.csv"
                )
                "ChaCha20 done. CSV at:\n$path"
            }
        }

        btElg.setOnClickListener {
            runWithUiLock(btElg, "Running ElGamal benchmarks (2^10 → 2^20, 15 rounds)…") {
                val path = ElGamalBenchmark.runRangeAndLog(
                    context = this,
                    minPow = 10,
                    maxPow = 20,
                    fileName = "elgamal_bench_2p10_2p20.csv"
                )
                "ElGamal done. CSV at:\n$path"
            }
        }

        btGcm.setOnClickListener {
            runWithUiLock(btGcm, "Running AES-GCM benchmarks (2^10 → 2^20, 15 rounds)…") {
                val path = AesGcmBenchmark.runRangeAndLog(
                    this, 10, 20, "aesgcm_bench_2p10_2p20.csv"
                )
                "AES-GCM done. CSV at:\n$path"
            }
        }

        btRsa.setOnClickListener {
            runWithUiLock(btRsa, "Running RSA (hybrid) benchmarks (2^10 → 2^20, 15 rounds)…") {
                val path = RsaHybridBenchmark.runRangeAndLog(
                    this, 10, 20, "rsa_bench_2p10_2p20.csv"
                )
                "RSA (hybrid) done. CSV at:\n$path"
            }
        }
    }

    private fun runWithUiLock(button: Button, startMsg: String, task: () -> String) {
        Toast.makeText(this, startMsg, Toast.LENGTH_SHORT).show()
        button.isEnabled = false
        executor.execute {
            try {
                val doneMsg = task()
                runOnUiThread { Toast.makeText(this, doneMsg, Toast.LENGTH_LONG).show() }
            } catch (t: Throwable) {
                runOnUiThread {
                    Toast.makeText(this, "Error: ${t.message ?: "unknown"}", Toast.LENGTH_LONG).show()
                }
            } finally {
                runOnUiThread { button.isEnabled = true }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }
}
