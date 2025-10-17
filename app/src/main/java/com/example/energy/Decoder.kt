package com.example.energy
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.energy.Decode.decode


class Decoder : AppCompatActivity() {
    private lateinit var editText: EditText
    private lateinit var textView: TextView
    private lateinit var decryptButton: Button
    private lateinit var copyButton: Button

    private lateinit var clipboardManager: ClipboardManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_decoder)

        editText = findViewById(R.id.editText)
        textView = findViewById(R.id.decryptedTextView)
        decryptButton = findViewById(R.id.decryptButton)
        copyButton = findViewById(R.id.copyButton)

        // create a clipboard manager variable to copy text
        clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager

        decryptButton.setOnClickListener {
            // get the string from the edit text
            val inputText = editText.text.toString()
            // call the decode function from the Decode class
            val decodedText = decode(inputText)
            // set the text to the edit text for display
            textView.text = decodedText
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