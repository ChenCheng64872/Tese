package com.example.energy

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var btAesCbc: Button
    private lateinit var btAesGcm: Button
    private lateinit var btChaCha20: Button
    private lateinit var btRsaHybrid: Button
    private lateinit var btElGamal: Button
    private lateinit var tvStatus: TextView

    private val minPow = 10
    private val maxPow = 20
    private val rounds = 15

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // <- use your layout file

        btAesCbc   = findViewById(R.id.btAesCbc)
        btAesGcm   = findViewById(R.id.btAesGcm)
        btChaCha20 = findViewById(R.id.btChaCha20)
        btRsaHybrid= findViewById(R.id.btRsaHybrid)
        btElGamal  = findViewById(R.id.btElGamal)
        tvStatus   = findViewById(R.id.tvStatus)

        btAesCbc.setOnClickListener {
            runBenchmark("AES-CBC (BC)") {
                AesBenchmark.runRangeAndLog(
                    context = this,
                    minPow = minPow,
                    maxPow = maxPow,
                    rounds = rounds,
                    fileName = "aes_bench_2p${minPow}_2p${maxPow}.csv"
                )
            }
        }

        btAesGcm.setOnClickListener {
            runBenchmark("AES-GCM") {
                AesGcmBenchmark.runRangeAndLog(
                    context = this,
                    minPow = minPow,
                    maxPow = maxPow,
                    rounds = rounds,
                    fileName = "aesgcm_bench_2p${minPow}_2p${maxPow}.csv"
                )
            }
        }

        btChaCha20.setOnClickListener {
            runBenchmark("ChaCha20") {
                ChaCha20Benchmark.runRangeAndLog(
                    context = this,
                    minPow = minPow,
                    maxPow = maxPow,
                    rounds = rounds,
                    fileName = "chacha20_bench_2p${minPow}_2p${maxPow}.csv"
                )
            }
        }

        btRsaHybrid.setOnClickListener {
            runBenchmark("RSA Hybrid") {
                RsaHybridBenchmark.runRangeAndLog(
                    context = this,
                    minPow = minPow,
                    maxPow = maxPow,
                    rounds = rounds,
                    fileName = "rsa_hybrid_bench_2p${minPow}_2p${maxPow}.csv"
                )
            }
        }

        btElGamal.setOnClickListener {
            runBenchmark("ElGamal Hybrid") {
                ElGamalBenchmark.runRangeAndLog(
                    context = this,
                    minPow = minPow,
                    maxPow = maxPow,
                    rounds = rounds,
                    fileName = "elgamal_bench_2p${minPow}_2p${maxPow}.csv"
                )
            }
        }
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        btAesCbc.isEnabled = enabled
        btAesGcm.isEnabled = enabled
        btChaCha20.isEnabled = enabled
        btRsaHybrid.isEnabled = enabled
        btElGamal.isEnabled = enabled
    }

    private fun runBenchmark(name: String, task: suspend () -> String) {
        lifecycleScope.launch {
            setButtonsEnabled(false)
            tvStatus.text = "Running $name …"
            try {
                val path = withContext(Dispatchers.IO) { task() }
                tvStatus.text = "$name done.\nCSV: $path"
                Toast.makeText(this@MainActivity, "Saved: $path", Toast.LENGTH_LONG).show()
                // Optional: tap status to open/share
                tvStatus.setOnClickListener { openCsv(path) }
            } catch (t: Throwable) {
                tvStatus.text = "$name failed: ${t.message}"
                Toast.makeText(this@MainActivity, "Error: ${t.message}", Toast.LENGTH_LONG).show()
            } finally {
                setButtonsEnabled(true)
            }
        }
    }

    private fun openCsv(path: String) {
        val csv = File(path)
        if (!csv.exists()) {
            Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show()
            return
        }
        val uri: Uri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            csv
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "text/csv")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Open CSV"))
    }
}
