package com.example.trumpcards

import android.content.Intent
import android.content.SharedPreferences
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import okhttp3.*
import org.json.JSONObject


class GameBeginActivity : AppCompatActivity() {

    private lateinit var tvWhoStarts: TextView
    private lateinit var ivToss: ImageView
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_begin)

        tvWhoStarts = findViewById(R.id.tv_who_starts)
        ivToss = findViewById(R.id.iv_toss)

        // Start GIF animation first
        Glide.with(this)
            .asGif()
            .load(R.drawable.toss_animation)
            .into(ivToss)

        // Get data from Intent
        val gameId = intent.getStringExtra("game_id") ?: ""
        val opponent = intent.getStringExtra("opponent")

        // Get player email from shared preferences
        val sharedPreferences: SharedPreferences = getSharedPreferences("userPrefs", MODE_PRIVATE)
        val email = sharedPreferences.getString("email", null)

        if (email == null) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Request the backend to decide the starter
        decideStarter(gameId)
    }

    private fun decideStarter(gameId: String) {
        val client = OkHttpClient()
        val url = "${ApiConfig.GAME_URL}/decide_starter"
        val json = JSONObject().put("game_id", gameId)
        val mediaType = "application/json".toMediaTypeOrNull()
        val requestBody = json.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@GameBeginActivity, "Error deciding starter", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: return
                val resp = JSONObject(body)
                val starter = resp.optString("starter", "")
                val opponent = intent.getStringExtra("opponent")


                runOnUiThread {
                    if (starter.isNotEmpty()) {
                        // Determine who starts the game
                        val email = getSharedPreferences("userPrefs", MODE_PRIVATE).getString("email", null)
                        val starts = if (starter == email) email else opponent
                        tvWhoStarts.text = if (starts == email) {
                            "🎉 You won the toss! You start the game."
                        } else {
                            "⌛ Opponent won the toss. Please wait for their move."
                        }

                        // Start game after 3 seconds
                        handler.postDelayed({
                            val nextIntent = if (starts == email) {
                                Intent(this@GameBeginActivity, ActivePlayerActivity::class.java)
                            } else {
                                Intent(this@GameBeginActivity, IdlePlayerActivity::class.java)
                            }

                            nextIntent.putExtra("game_id", gameId)
                            nextIntent.putExtra("opponent", opponent)
                            startActivity(nextIntent)
                            finish()
                        }, 3000)
                    }
                }
            }
        })
    }
}
