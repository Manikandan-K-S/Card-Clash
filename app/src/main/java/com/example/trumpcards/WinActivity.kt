package com.example.trumpcards

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class WinActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_win)

        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, HomePageActivity::class.java)
            startActivity(intent)
            finish()
        }, 5000) // 5 seconds delay
    }
}
