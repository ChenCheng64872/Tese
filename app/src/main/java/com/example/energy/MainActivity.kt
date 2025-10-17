package com.example.energy
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var encodeButton: Button
    private lateinit var decodeButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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