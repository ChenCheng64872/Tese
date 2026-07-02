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

    private lateinit var btAscon: Button
    private lateinit var btElephant: Button
    private lateinit var btGiftCofb: Button
    private lateinit var btGrain128: Button
    private lateinit var btXoodyak: Button
    private lateinit var btAesCbc: Button
    private lateinit var btAesGcm: Button
    private lateinit var btChaCha20: Button
    private lateinit var btRsaHybrid: Button
    private lateinit var btElGamal: Button
    private lateinit var btRunAll: Button
    private lateinit var btShareAll: Button
    private lateinit var tvStatus: TextView

    private val minPow = 10
    private val maxPow = 18

    private val roundsFast = 5
    private val innerFast = 100
    private val warmupFast = 3

    private val roundsHybrid = 5
    private val innerHybrid = 10
    private val warmupHybrid = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // <- use your layout file
        btAscon     = findViewById(R.id.btAscon)
        btElephant  = findViewById(R.id.btElephant)
        btGiftCofb  = findViewById(R.id.btGiftCofb)
        btGrain128  = findViewById(R.id.btGrain128)
        btXoodyak   = findViewById(R.id.btXoodyak)
        btAesCbc   = findViewById(R.id.btAesCbc)
        btAesGcm   = findViewById(R.id.btAesGcm)
        btChaCha20 = findViewById(R.id.btChaCha20)
        btRsaHybrid= findViewById(R.id.btRsaHybrid)
        btElGamal  = findViewById(R.id.btElGamal)
        btRunAll    = findViewById(R.id.btRunAll)
        btShareAll  = findViewById(R.id.btShareAll)
        tvStatus   = findViewById(R.id.tvStatus)

        btRunAll.setOnClickListener {
            runAllBenchmarks()
        }

        btAesCbc.setOnClickListener {
            runBenchmark("AES-CBC (BC)") {
                AesBenchmark.runRangeAndLog(
                    context = this,
                    minPow = minPow,
                    maxPow = maxPow,
                    rounds = roundsFast,
                    innerIterations = innerFast,
                    warmupRounds = warmupFast,
                    fileName = "aes_bench_2p${minPow}_2p${maxPow}.csv"
                )
            }
        }
        btAscon.setOnClickListener {
            runBenchmark("Ascon-128") {
                AsconLwcBenchmark.runRangeAndLog(
                    context = this,
                    minPow = minPow,
                    maxPow = maxPow,
                    rounds = roundsFast,
                    innerIterations = 50,
                    warmupRounds = warmupFast,
                    fileName = "ascon_bench_2p${minPow}_2p${maxPow}.csv"
                )
            }
        }
        btElephant.setOnClickListener {
            runBenchmark("Elephant") {
                ElephantBenchmark.runRangeAndLog(
                    context = this,
                    minPow = minPow,
                    maxPow = maxPow,
                    rounds = roundsFast,
                    innerIterations = 10,
                    warmupRounds = warmupFast,
                    fileName = "elephant_bench_2p${minPow}_2p${maxPow}.csv"
                )
            }
        }

        btGiftCofb.setOnClickListener {
            runBenchmark("GIFT-COFB") {
                GiftCofbBenchmark.runRangeAndLog(
                    context = this,
                    minPow = minPow,
                    maxPow = maxPow,
                    rounds = roundsFast,
                    innerIterations = 20,
                    warmupRounds = warmupFast,
                    fileName = "giftcofb_bench_2p${minPow}_2p${maxPow}.csv"
                )
            }
        }

        btGrain128.setOnClickListener {
            runBenchmark("Grain-128AEAD") {
                Grain128AeadBenchmark.runRangeAndLog(
                    context = this,
                    minPow = minPow,
                    maxPow = maxPow,
                    rounds = roundsFast,
                    innerIterations = 20,
                    warmupRounds = warmupFast,
                    fileName = "grain128aead_bench_2p${minPow}_2p${maxPow}.csv"
                )
            }
        }

        btXoodyak.setOnClickListener {
            runBenchmark("Xoodyak") {
                XoodyakBenchmark.runRangeAndLog(
                    context = this,
                    minPow = minPow,
                    maxPow = maxPow,
                    rounds = roundsFast,
                    innerIterations = 50,
                    warmupRounds = warmupFast,
                    fileName = "xoodyak_bench_2p${minPow}_2p${maxPow}.csv"
                )
            }
        }


        btAesGcm.setOnClickListener {
            runBenchmark("AES-GCM") {
                AesGcmBenchmark.runRangeAndLog(
                    context = this,
                    minPow = minPow,
                    maxPow = maxPow,
                    rounds = roundsFast,
                    innerIterations = innerFast,
                    warmupRounds = warmupFast,
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
                    rounds = roundsFast,
                    innerIterations = innerFast,
                    warmupRounds = warmupFast,
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
                    rounds = roundsHybrid,
                    innerIterations = innerHybrid,
                    warmupRounds = warmupHybrid,
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
                    rounds = roundsHybrid,
                    innerIterations = innerHybrid,
                    warmupRounds = warmupHybrid,
                    fileName = "elgamal_bench_2p${minPow}_2p${maxPow}.csv"
                )
            }
        }
    }

    private fun runAllBenchmarks() {
        val subDir = "benchmarks_${System.currentTimeMillis()}"
        lifecycleScope.launch {
            setButtonsEnabled(false)
            val startTime = System.currentTimeMillis()
            val results = mutableListOf<String>()
            
            val benchmarks = listOf(
                "AES-CBC" to { AesBenchmark.runRangeAndLog(this@MainActivity, minPow, maxPow, roundsFast, innerFast, warmupFast, subDir = subDir) },
                "Ascon-128" to { AsconLwcBenchmark.runRangeAndLog(this@MainActivity, minPow, maxPow, roundsFast, 50, warmupFast, subDir = subDir) },
                "Elephant" to { ElephantBenchmark.runRangeAndLog(this@MainActivity, minPow, maxPow, roundsFast, 10, warmupFast, subDir = subDir) },
                "GIFT-COFB" to { GiftCofbBenchmark.runRangeAndLog(this@MainActivity, minPow, maxPow, roundsFast, 20, warmupFast, subDir = subDir) },
                "Grain-128" to { Grain128AeadBenchmark.runRangeAndLog(this@MainActivity, minPow, maxPow, roundsFast, 20, warmupFast, subDir = subDir) },
                "Xoodyak" to { XoodyakBenchmark.runRangeAndLog(this@MainActivity, minPow, maxPow, roundsFast, 50, warmupFast, subDir = subDir) },
                "AES-GCM" to { AesGcmBenchmark.runRangeAndLog(this@MainActivity, minPow, maxPow, roundsFast, innerFast, warmupFast, subDir = subDir) },
                "ChaCha20" to { ChaCha20Benchmark.runRangeAndLog(this@MainActivity, minPow, maxPow, roundsFast, innerFast, warmupFast, subDir = subDir) },
                "RSA Hybrid" to { RsaHybridBenchmark.runRangeAndLog(this@MainActivity, minPow, maxPow, roundsHybrid, innerHybrid, warmupHybrid, subDir = subDir) },
                "ElGamal" to { ElGamalBenchmark.runRangeAndLog(this@MainActivity, minPow, maxPow, roundsHybrid, innerHybrid, warmupHybrid, subDir = subDir) }
            )

            try {
                for ((name, task) in benchmarks) {
                    tvStatus.text = "Running All: $name ..."
                    val path = withContext(Dispatchers.IO) { task() }
                    results.add("$name: $path")
                }
                val duration = (System.currentTimeMillis() - startTime) / 1000
                tvStatus.text = "All Done! (${duration}s)\nFiles saved in: $subDir\n\n" + results.joinToString("\n")
                
                btShareAll.visibility = android.view.View.VISIBLE
                btShareAll.setOnClickListener {
                    shareDirectory(subDir)
                }
            } catch (t: Throwable) {
                tvStatus.text = "Batch failed: ${t.message}"
            } finally {
                setButtonsEnabled(true)
            }
        }
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        btAesCbc.isEnabled = enabled
        btAesGcm.isEnabled = enabled
        btChaCha20.isEnabled = enabled
        btRsaHybrid.isEnabled = enabled
        btElGamal.isEnabled = enabled
        btAscon.isEnabled = enabled
        btGrain128.isEnabled = enabled
        btXoodyak.isEnabled = enabled
        btRunAll.isEnabled = enabled
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
        // Changed to ACTION_SEND to make it easier to extract data from a real phone
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share CSV Result"))
    }

    private fun shareDirectory(subDirName: String) {
        val dir = File(getExternalFilesDir(null), subDirName)
        if (!dir.exists() || !dir.isDirectory) return

        val files = dir.listFiles { _, name -> name.endsWith(".csv") } ?: return
        val uris = ArrayList<Uri>()
        for (file in files) {
            val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
            uris.add(uri)
        }

        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "text/csv"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share All CSVs"))
    }
}
