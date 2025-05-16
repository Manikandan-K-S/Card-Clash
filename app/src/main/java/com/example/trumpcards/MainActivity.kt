package com.example.trumpcards

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.RetryPolicy
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val sharedPreferences = getSharedPreferences("userPrefs", MODE_PRIVATE)
        val email = sharedPreferences.getString("email", null)
        val password = sharedPreferences.getString("password", null)

        if (email != null && password != null) {
            authenticateUser(email, password)
        } else {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun authenticateUser(email: String, password: String) {
        val url = "${ApiConfig.BASE_URL}/login"  // Use the centralized BASE_URL
        val requestQueue = Volley.newRequestQueue(this)

        val jsonBody = JSONObject().apply {
            put("email", email)
            put("password", password)
        }

        val stringRequest = object : StringRequest(Request.Method.POST, url,
            { response ->
                // If login is successful, navigate to HomePageActivity
                val intent = Intent(this, HomePageActivity::class.java)
                startActivity(intent)
                finish()  // Close MainActivity
            },
            { error ->
                Toast.makeText(this, "Invalid credentials, please try again.", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }) {
            override fun getBody(): ByteArray {
                // Return the JSON body in UTF-8 format
                return jsonBody.toString().toByteArray(Charsets.UTF_8)
            }
            override fun getHeaders(): MutableMap<String, String> {
                return mutableMapOf("Content-Type" to "application/json")
            }

            override fun getRetryPolicy(): RetryPolicy {
                return DefaultRetryPolicy(
                    5000,  // Timeout in milliseconds
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
                )
            }
        }

        requestQueue.add(stringRequest)
    }
}
