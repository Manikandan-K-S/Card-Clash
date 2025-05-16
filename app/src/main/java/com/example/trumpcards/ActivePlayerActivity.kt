package com.example.trumpcards

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import org.json.JSONObject

class ActivePlayerActivity : AppCompatActivity() {

    private lateinit var cardImage: ImageView
    private lateinit var cardName: TextView
    private lateinit var power: Button
    private lateinit var strikeRate: Button
    private lateinit var wickets: Button
    private lateinit var matchesPlayed: Button
    private lateinit var runsScored: Button
    private lateinit var highestScore: Button

    private lateinit var gameId: String
    var email: String? = null
    private lateinit var cardId: String  // to send with attribute

    private val timeoutHandler = Handler(Looper.getMainLooper())
    private val timeoutRunnable = Runnable {
        val intent = Intent(this, LoseActivity::class.java).apply {
            putExtra("game_id", gameId)
            putExtra("email", email)
        }
        startActivity(intent)
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_active_player)

        gameId = intent.getStringExtra("game_id") ?: ""
        val sharedPreferences = getSharedPreferences("userPrefs", MODE_PRIVATE)
        email = sharedPreferences.getString("email", null)

        Log.d("ACTIVE_PLAYER", "Game ID: $gameId, Email: $email")

        cardImage = findViewById(R.id.cardImage)
        cardName = findViewById(R.id.cardName)
        power = findViewById(R.id.power)
        strikeRate = findViewById(R.id.strikeRate)
        wickets = findViewById(R.id.wickets)
        matchesPlayed = findViewById(R.id.matchesPlayed)
        runsScored = findViewById(R.id.runsScored)
        highestScore = findViewById(R.id.highestScore)

        fetchGameState()

        setAttributeClickListener(power, "power")
        setAttributeClickListener(strikeRate, "strike_rate")
        setAttributeClickListener(wickets, "wickets")
        setAttributeClickListener(matchesPlayed, "matches_played")
        setAttributeClickListener(runsScored, "runs_scored")
        setAttributeClickListener(highestScore, "highest_score")

        // Start 15-second timeout
        timeoutHandler.postDelayed(timeoutRunnable, 15000)
    }

    private fun fetchGameState() {
        val url = "${ApiConfig.GAME_URL}/get_game_state"
        Log.d("ACTIVE_PLAYER", "Fetching game state from $url")

        val jsonBody = JSONObject().apply {
            put("game_id", gameId)
            put("email", email)
        }

        Volley.newRequestQueue(this).apply {
            add(JsonObjectRequest(Request.Method.POST, url, jsonBody,
                { response ->
                    Log.d("ACTIVE_PLAYER", "Game state response: $response")

                    val currentCard = response.getJSONObject("your_card")
                    cardId = currentCard.getString("id")
                    cardName.text = currentCard.getString("player_name")
                    power.text = "Power: ${currentCard.getInt("power")}"
                    strikeRate.text = "Strike Rate: ${currentCard.getDouble("strike_rate")}"
                    wickets.text = "Wickets: ${currentCard.getInt("wickets")}"
                    matchesPlayed.text = "Matches: ${currentCard.getInt("matches_played")}"
                    runsScored.text = "Runs: ${currentCard.getInt("runs_scored")}"
                    highestScore.text = "Highest: ${currentCard.getInt("highest_score")}"

                    val imageUrl = currentCard.getString("img")
                    Glide.with(this@ActivePlayerActivity)
                        .load(imageUrl)
                        .apply(
                            RequestOptions()
                                .placeholder(R.drawable.sample_card)
                                .error(R.drawable.sample_card)
                                .centerCrop()
                        )
                        .into(cardImage)
                },
                { error ->
                    Toast.makeText(this@ActivePlayerActivity, "Error fetching game state", Toast.LENGTH_SHORT).show()
                    Log.e("ACTIVE_PLAYER", "Volley error: ${error.message}", error)
                }
            ))
        }
    }

    private fun setAttributeClickListener(button: Button, attribute: String) {
        button.setOnClickListener {
            timeoutHandler.removeCallbacks(timeoutRunnable) // Cancel auto-lose timer

            val url = "${ApiConfig.GAME_URL}/play_turn"
            val jsonBody = JSONObject().apply {
                put("game_id", gameId)
                put("email", email)
                put("attribute", attribute)
            }

            val request = JsonObjectRequest(
                Request.Method.POST, url, jsonBody,
                { response ->
                    Log.d("ACTIVE_PLAYER", "Turn played successfully: $response")
                    val intent = Intent(this@ActivePlayerActivity, TurnActivity::class.java).apply {
                        putExtra("game_id", gameId)
                        putExtra("email", email)
                    }
                    startActivity(intent)
                    finish()
                },
                { error ->
                    Log.e("ACTIVE_PLAYER", "Error playing turn: ${error.message}", error)
                    Toast.makeText(this@ActivePlayerActivity, "Failed to play turn", Toast.LENGTH_SHORT).show()
                }
            )

            Volley.newRequestQueue(this).add(request)
        }
    }

}
