package com.example.trumpcards

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class StartGameActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private val handler = Handler(Looper.getMainLooper())
    private var attemptCount = 0
    private val maxAttempts = 15
    private var gameId: String? = null
    private var email: String? = null
    private var isPolling = false
    private lateinit var pollRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_game)

        tvStatus = findViewById(R.id.tv_status)
        tvStatus.text = "Finding an opponent..."

        val sharedPreferences = getSharedPreferences("userPrefs", MODE_PRIVATE)
        email = sharedPreferences.getString("email", null)

        if (email == null) {
            Toast.makeText(this, "Login expired. Please login again.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        createOrJoinGame()
    }

    private fun createOrJoinGame() {
        val url = "${ApiConfig.GAME_URL}/start_game"
        val json = JSONObject().put("email", email)
        val mediaType = "application/json".toMediaTypeOrNull()
        val requestBody = json.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        ApiConfig.client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@StartGameActivity, "Network error!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: return
                val resp = JSONObject(body)
                val message = resp.optString("message", "")
                gameId = resp.optString("game_id","")
                println("game id that is received")
                print(gameId)

                runOnUiThread {
                    if (message == "matched") {
                        gameId = resp.optString("game_id")
                        println("response from server")
                        println(gameId)
                        val opponent = resp.optString("opponent")
                        val safeGameId = gameId
                        if (safeGameId != null) {
                            goToNextActivity(true, opponent, safeGameId)
                        } else {
                            tvStatus.text = "Game ID missing. Please retry."
                            finish()
                        }
                    } else {
                        startPolling()  // Start polling now that we're waiting for a match
                    }
                }
            }
        })
    }

    private fun startPolling() {
        isPolling = true

        pollRunnable = Runnable {
            if (attemptCount >= maxAttempts) {
                tvStatus.text = "No match found. Try again later."
                removeFromWaitingList()
                finish()
                return@Runnable
            }
            println(gameId)

            attemptCount++
            tvStatus.text = "Waiting for match... ($attemptCount/$maxAttempts)"

            val url = "${ApiConfig.GAME_URL}/check_match"
            val json = JSONObject().put("game_id", gameId)  // Send game_id instead of email
            val mediaType = "application/json".toMediaTypeOrNull()
            val requestBody = json.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            ApiConfig.client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        Toast.makeText(this@StartGameActivity, "Error checking match", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    val body = response.body?.string() ?: return
                    val resp = JSONObject(body)
                    val message = resp.optString("message", "")

                    if (message == "matched") {
                        gameId = resp.optString("game_id")
                        val opponent = resp.optString("opponent")
                        val starts = resp.optString("starts")

                        runOnUiThread {
                            handler.removeCallbacks(pollRunnable)
                            isPolling = false
                            val safeGameId = gameId
                            if (safeGameId != null) {
                                goToNextActivity(true, opponent, safeGameId)
                            } else {
                                tvStatus.text = "Game ID missing. Please retry."
                                finish()
                            }
                        }
                    } else {
                        handler.postDelayed(pollRunnable, 1000)  // Continue polling
                    }
                }
            })
        }

        handler.postDelayed(pollRunnable, 1000)
    }

    private fun goToNextActivity(isStarter: Boolean, opponent: String, gameId: String) {
        tvStatus.text = if (isStarter) {
            "Match found! You start the game vs $opponent."
        } else {
            "Match found! $opponent starts the game."
        }

        removeFromWaitingList()

        val intent = Intent(this, GameBeginActivity::class.java)

        intent.putExtra("game_id", gameId)
        intent.putExtra("email", email)
        intent.putExtra("opponent", opponent)

        if (isPolling) {
            handler.removeCallbacks(pollRunnable)
            isPolling = false
        }

        startActivity(intent)
        finish()
    }

    private fun removeFromWaitingList() {
        val url = "${ApiConfig.GAME_URL}/remove_waiting_player"
        val json = JSONObject().put("email", email)
        val mediaType = "application/json".toMediaTypeOrNull()
        val requestBody = json.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        ApiConfig.client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@StartGameActivity, "Error removing from waiting list", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                // No action needed
            }
        })
    }

    override fun onDestroy() {
        if (isPolling) {
            handler.removeCallbacks(pollRunnable)
        }
        super.onDestroy()
    }
}
