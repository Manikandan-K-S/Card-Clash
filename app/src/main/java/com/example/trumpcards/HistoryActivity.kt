package com.example.trumpcards

import android.os.Bundle
import android.view.View
import android.widget.ListView
import android.widget.SimpleAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONObject

class HistoryActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var matchHistoryAdapter: SimpleAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        listView = findViewById(R.id.historyListView)


        // Get the email from shared preferences (or intent extras if needed)
        val email  = getSharedPreferences("userPrefs", MODE_PRIVATE).getString("email", "null").toString()

        // Fetch match history from the server
        fetchMatchHistory(email)
    }

    private fun fetchMatchHistory(email: String) {
        val url = "${ApiConfig.BASE_URL}/get_match_history"
        val jsonBody = JSONObject().apply {
            put("email", email)
        }

        val jsonObjectRequest = object : JsonObjectRequest(
            Method.POST, url, jsonBody,
            Response.Listener { response ->
                val history = response.getJSONArray("history")
                val matchList = ArrayList<Map<String, String>>()

                for (i in 0 until history.length()) {
                    val match = history.getJSONObject(i)
                    val opponent = match.getString("opponent")
                    val result = match.getString("result")

                    val matchMap = HashMap<String, String>()
                    matchMap["opponent"] = opponent
                    matchMap["result"] = result

                    matchList.add(matchMap)
                }

                val noHistoryText = findViewById<TextView>(R.id.noHistoryText)
                val historyListView = findViewById<ListView>(R.id.historyListView)

                if (matchList.isEmpty()) {
                    noHistoryText.visibility = View.VISIBLE
                    historyListView.visibility = View.GONE
                } else {
                    noHistoryText.visibility = View.GONE
                    historyListView.visibility = View.VISIBLE

                    matchHistoryAdapter = SimpleAdapter(
                        this@HistoryActivity,
                        matchList,
                        R.layout.match_history_item,
                        arrayOf("opponent", "result"),
                        intArrayOf(R.id.opponentName, R.id.matchResult)
                    )
                    historyListView.adapter = matchHistoryAdapter
                }
            },
            Response.ErrorListener { error ->
                Toast.makeText(this, "Failed to load history", Toast.LENGTH_SHORT).show()
                error.printStackTrace()
            }
        ) {
            override fun getBody(): ByteArray {
                return jsonBody.toString().toByteArray(Charsets.UTF_8)
            }

            override fun getBodyContentType(): String {
                return "application/json"
            }
        }

        Volley.newRequestQueue(this).add(jsonObjectRequest)
    }

}
