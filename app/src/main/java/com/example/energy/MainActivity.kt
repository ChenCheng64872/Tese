package com.example.energy
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.energy.chacha20.ChaChaBenchmark

class MainActivity : AppCompatActivity() {
    private lateinit var encodeButton: Button
    private lateinit var decodeButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        Thread {
            val results = AesBenchmark.run()
            results.forEach {
                Log.i("AES-BENCH", it.asLine())
            }
        }.start()

        Thread {
            val results = ChaChaBenchmark.run()
            results.forEach { android.util.Log.i("CHACHA-BENCH", it.asLine()) }
        }.start()

        encodeButton = findViewById<Button>(R.id.btVar1)
        decodeButton = findViewById<Button>(R.id.btVar2)

        encodeButton.setOnClickListener {
            val intent = Intent(
                this,
                Encoder::class.java
            )
            startActivity(intent)
        }

        decodeButton.setOnClickListener {
            val intent = Intent(
                this,
                Decoder::class.java
            )
            startActivity(intent)
        }
    }
}