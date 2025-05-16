package com.example.trumpcards

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import org.json.JSONObject

class TurnActivity : AppCompatActivity() {

    private lateinit var cardImage: ImageView
    private lateinit var resultText: TextView
    private lateinit var attributeText: TextView
    private lateinit var gameId: String
    private var email: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_turn)

        cardImage = findViewById(R.id.cardImage)
        resultText = findViewById(R.id.resultText)
        attributeText = findViewById(R.id.attributeText)

        gameId = intent.getStringExtra("game_id") ?: ""
        val sharedPreferences = getSharedPreferences("userPrefs", MODE_PRIVATE)
        email = sharedPreferences.getString("email", null)

        fetchTurnDetails()
    }

    private fun fetchTurnDetails() {
        val url = "${ApiConfig.GAME_URL}/get_turn_details"
        val jsonBody = JSONObject().apply {
            put("game_id", gameId)
            put("email", email)
        }

        val request = JsonObjectRequest(Request.Method.POST, url, jsonBody,
            { response ->
                Log.d("TURN_ACTIVITY", "Turn details: $response")

                val result = response.optBoolean("result", false)
                val card = response.optJSONObject("card")
                val message = response.optString("message", "")
                val attribute = response.optString("attribute", "")
                val gameOver = response.optBoolean("gameOver", false)
                val nextTurn = response.optString("next_turn", "")
                val overallWinner = response.optString("overallWinner", "")

                resultText.text = message
                attributeText.text = "Attribute used: $attribute"

                card?.getString("img")?.let {
                    Glide.with(this).load(it).into(cardImage)
                }

                Handler(Looper.getMainLooper()).postDelayed({
                    if (gameOver) {
                        val intent = Intent(
                            this,
                            if (overallWinner == email) WinActivity::class.java else LoseActivity::class.java
                        ).apply {
                            putExtra("game_id", gameId)
                            putExtra("email", email)
                        }
                        startActivity(intent)
                    } else {
                        val nextActivity = if (nextTurn == email)
                            ActivePlayerActivity::class.java else IdlePlayerActivity::class.java
                        val intent = Intent(this, nextActivity).apply {
                            putExtra("game_id", gameId)
                            putExtra("email", email)
                        }
                        startActivity(intent)
                    }
                    finish()
                }, 3000)
            },
            { error ->
                Log.e("TURN_ACTIVITY", "Error fetching turn details: ${error.message}", error)
                Toast.makeText(this, "Failed to load turn result", Toast.LENGTH_SHORT).show()
            }
        )

        Volley.newRequestQueue(this).add(request)
    }
}
