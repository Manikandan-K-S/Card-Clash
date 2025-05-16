package com.example.trumpcards

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class HomePageActivity : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_page)

        sharedPreferences = getSharedPreferences("userPrefs", MODE_PRIVATE)

        val btnStartGame = findViewById<Button>(R.id.btn_start_game)
        val btnViewHistory = findViewById<Button>(R.id.btn_view_history)

        val ivLogout = findViewById<ImageView>(R.id.iv_logout)
        ivLogout.setOnClickListener {
            logoutUser()
        }


        btnStartGame.setOnClickListener {
            startActivity(Intent(this, StartGameActivity::class.java))
        }

        btnViewHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
    }

    private fun logoutUser() {
        sharedPreferences.edit().clear().apply()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
