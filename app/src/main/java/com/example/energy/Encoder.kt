package com.example.energy

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.energy.Encode.encode


class Encoder : AppCompatActivity() {
    private lateinit var editText: EditText
    private lateinit var textView: TextView
    private lateinit var encryptButton: Button
    private lateinit var copyButton: Button

    private lateinit var clipboardManager: ClipboardManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_encoder)

        // link the edittext and textview with its id
        editText = findViewById(R.id.editText)
        textView = findViewById(R.id.encryptedTextView)
        encryptButton = findViewById(R.id.encryptButton)
        copyButton = findViewById(R.id.copyButton)


        // create a clipboard manager variable to copy text
        clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager

        encryptButton.setOnClickListener {

            // get user input
            val inputText = editText.text.toString()

            // call the encode function from the Encode class
            val encodedText = encode(inputText)

            // display the encrypted text
            textView.text = encodedText
        }

        copyButton.setOnClickListener {
            // get the text from the textview
            val data = textView.text.toString().trim { it <= ' ' }

            // check if the textview is not empty
            if (data.isNotEmpty()) {

                // copy the text in the clip board
                val copiedTextClip = ClipData.newPlainText("text", data)
                clipboardManager.setPrimaryClip(copiedTextClip)

                // display message that the text has been copied
                Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}