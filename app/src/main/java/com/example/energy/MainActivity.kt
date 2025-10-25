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

        val btAes = findViewById<Button>(R.id.btVar1) //  run AES benchmark
        val btCha = findViewById<Button>(R.id.btVar2) //  run ChaCha20 benchmark
        val btElg = findViewById<Button>(R.id.btVar3) // run ElGamal benchmark
        val btGcm = findViewById<Button>(R.id.btVar4)
        val btRsa = findViewById<Button>(R.id.btVar5)
        btAes.setOnClickListener {
            Toast.makeText(this, "Running AES benchmarks (2^10 → 2^20, 15 rounds)…", Toast.LENGTH_SHORT).show()
            executor.execute {
                try {
                    val path = AesBenchmark.runRangeAndLog(
                        context = this,
                        minPow = 10,
                        maxPow = 20,
                        fileName = "aes_bench_2p10_2p20.csv"
                    )
                    runOnUiThread {
                        Toast.makeText(this, "AES done. CSV at:\n$path", Toast.LENGTH_LONG).show()
                    }
                } catch (t: Throwable) {
                    runOnUiThread {
                        Toast.makeText(this, "AES error: ${t.message ?: "unknown"}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        btCha.setOnClickListener {
            Toast.makeText(this, "Running ChaCha20 benchmarks (2^10 → 2^20, 15 rounds)…", Toast.LENGTH_SHORT).show()
            executor.execute {
                try {
                    val path = ChaCha20Benchmark.runRangeAndLog(
                        context = this,
                        minPow = 10,
                        maxPow = 20,
                        fileName = "chacha_bench_2p10_2p20.csv"
                    )
                    runOnUiThread {
                        Toast.makeText(this, "ChaCha20 done. CSV at:\n$path", Toast.LENGTH_LONG).show()
                    }
                } catch (t: Throwable) {
                    runOnUiThread {
                        Toast.makeText(this, "ChaCha20 error: ${t.message ?: "unknown"}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        btElg.setOnClickListener {
            Toast.makeText(
                this,
                "Running ElGamal benchmarks (2^10 → 2^20, 15 rounds)…",
                Toast.LENGTH_SHORT
            ).show()
            executor.execute {
                try {
                    val path = ElGamalBenchmark.runRangeAndLog(
                        context = this,
                        minPow = 10,
                        maxPow = 20,
                        fileName = "elgamal_bench_2p10_2p20.csv"
                    )
                    runOnUiThread {
                        Toast.makeText(this, "ElGamal done. CSV at:\n$path", Toast.LENGTH_LONG)
                            .show()
                    }
                } catch (t: Throwable) {
                    runOnUiThread {
                        Toast.makeText(
                            this,
                            "ElGamal error: ${t.message ?: "unknown"}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
        btGcm.setOnClickListener {
            Toast.makeText(this, "Running AES-GCM benchmarks (2^10 → 2^20, 15 rounds)…", Toast.LENGTH_SHORT).show()
            executor.execute {
                try {
                    val path = AesGcmBenchmark.runRangeAndLog(this, 10, 20, "aesgcm_bench_2p10_2p20.csv")
                    runOnUiThread { Toast.makeText(this, "AES-GCM done. CSV at:\n$path", Toast.LENGTH_LONG).show() }
                } catch (t: Throwable) {
                    runOnUiThread { Toast.makeText(this, "AES-GCM error: ${t.message}", Toast.LENGTH_LONG).show() }
                }
            }
        }

        btRsa.setOnClickListener {
            Toast.makeText(this, "Running RSA (hybrid) benchmarks (2^10 → 2^20, 15 rounds)…", Toast.LENGTH_SHORT).show()
            executor.execute {
                try {
                    val path = RsaHybridBenchmark.runRangeAndLog(this, 10, 20, "rsa_bench_2p10_2p20.csv")
                    runOnUiThread { Toast.makeText(this, "RSA (hybrid) done. CSV at:\n$path", Toast.LENGTH_LONG).show() }
                } catch (t: Throwable) {
                    runOnUiThread { Toast.makeText(this, "RSA error: ${t.message}", Toast.LENGTH_LONG).show() }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }
}
