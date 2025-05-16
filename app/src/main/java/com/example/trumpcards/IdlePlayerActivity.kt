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
import com.bumptech.glide.Glide
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class IdlePlayerActivity : AppCompatActivity() {

    private lateinit var cardImage: ImageView
    private lateinit var cardName: TextView
    private lateinit var tvPower: TextView
    private lateinit var tvStrikeRate: TextView
    private lateinit var tvWickets: TextView
    private lateinit var tvMatchesPlayed: TextView
    private lateinit var tvRunsScored: TextView
    private lateinit var tvHighestScore: TextView
    private lateinit var tvWaiting: TextView

    private var email: String? = null
    private var gameId: String? = null

    private var waitCounter = 0
    private val maxWait = 15
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var pollRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_idle_player)

        // Bind views
        cardImage       = findViewById(R.id.cardImage)
        cardName        = findViewById(R.id.cardName)
        tvPower         = findViewById(R.id.tv_power)
        tvStrikeRate    = findViewById(R.id.tv_strikeRate)
        tvWickets       = findViewById(R.id.tv_wickets)
        tvMatchesPlayed = findViewById(R.id.tv_matchesPlayed)
        tvRunsScored    = findViewById(R.id.tv_runsScored)
        tvHighestScore  = findViewById(R.id.tv_highestScore)
        tvWaiting       = findViewById(R.id.tv_waiting)

        email  = getSharedPreferences("userPrefs", MODE_PRIVATE).getString("email", null)
        gameId = intent.getStringExtra("game_id")

        if (email == null || gameId == null) {
            Toast.makeText(this, "Missing game or user info", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        fetchTopCard()
        setupPolling()
        handler.postDelayed(pollRunnable, 1000)
    }

    private fun fetchTopCard() {
        val url = "${ApiConfig.GAME_URL}/get_game_state"

        val bodyJson = JSONObject()
            .put("email", email)
            .put("game_id", gameId)
        val body = bodyJson.toString()
            .toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        ApiConfig.client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@IdlePlayerActivity, "Failed to load card", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val resp = response.body?.string() ?: return
                val json = JSONObject(resp)
                val card = json.getJSONObject("your_card")

                runOnUiThread {
                    cardName.text        = card.getString("player_name")
                    tvPower.text         = "Power: ${card.getInt("power")}"
                    tvStrikeRate.text    = "Strike Rate: ${card.getDouble("strike_rate")}"
                    tvWickets.text       = "Wickets: ${card.getInt("wickets")}"
                    tvMatchesPlayed.text = "Matches: ${card.getInt("matches_played")}"
                    tvRunsScored.text    = "Runs: ${card.getInt("runs_scored")}"
                    tvHighestScore.text  = "Highest: ${card.getInt("highest_score")}"

                    // Use same image loading style as ActivePlayerActivity
                    val imageUrl = card.getString("img")
                    Log.d("IDLE_PLAYER", "Loading image: $imageUrl")
                    Glide.with(this@IdlePlayerActivity)
                        .load(imageUrl)
                        .placeholder(R.drawable.sample_card)
                        .into(cardImage)
                }
            }
        })
    }

    private fun setupPolling() {
        pollRunnable = object : Runnable {
            override fun run() {
                if (waitCounter >= maxWait) {
                    declareIdleWinner()
                    return
                }

                waitCounter++
                tvWaiting.text = "Waiting for opponent... ($waitCounter/$maxWait)"
                checkTurnProcessed()

                handler.postDelayed(this, 1000)
            }
        }
    }

    private fun checkTurnProcessed() {
        val url = "${ApiConfig.GAME_URL}/check_turn_processed"
        val bodyJson = JSONObject()
            .put("email", email)
            .put("game_id", gameId)
        val body = bodyJson.toString()
            .toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        ApiConfig.client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Silent fail, just try again on next poll
            }

            override fun onResponse(call: Call, response: Response) {
                val json = JSONObject(response.body?.string() ?: return)
                if (json.optBoolean("turn_processed", false)) {
                    runOnUiThread {
                        val intent = Intent(this@IdlePlayerActivity, TurnActivity::class.java)
                        intent.putExtra("email", email)
                        intent.putExtra("game_id", gameId)
                        startActivity(intent)
                        finish()
                    }
                }
            }
        })
    }

    private fun declareIdleWinner() {
        val url = "${ApiConfig.GAME_URL}/declare_idle_winner"
        val bodyJson = JSONObject()
            .put("email", email)
            .put("game_id", gameId)
        val body = bodyJson.toString()
            .toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        ApiConfig.client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@IdlePlayerActivity, "Error declaring win", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    startActivity(Intent(this@IdlePlayerActivity, WinActivity::class.java))
                    finish()
                }
            }
        })
    }

    override fun onDestroy() {
        handler.removeCallbacks(pollRunnable)
        super.onDestroy()
    }
}
