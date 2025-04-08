package com.example.trumpcards

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.RetryPolicy
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("userPrefs", MODE_PRIVATE)

        // UI elements
        val etEmail = findViewById<EditText>(R.id.et_email)
        val etPassword = findViewById<EditText>(R.id.et_password)
        val btnLogin = findViewById<Button>(R.id.btn_login)
        val tvRegisterLink = findViewById<TextView>(R.id.tv_register_link)

        // Login button click listener
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loginUser(email, password)
        }

        // Register link click listener
        tvRegisterLink.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            finish()  // Close the LoginActivity
        }
    }

    // Function to handle login
    private fun loginUser(email: String, password: String) {
        val url = "${ApiConfig.BASE_URL}/login"  // Assuming you have an API config

        val requestQueue = Volley.newRequestQueue(this)

        val jsonBody = JSONObject().apply {
            put("email", email)
            put("password", password)
        }

        val stringRequest = object : StringRequest(Request.Method.POST, url,
            { response ->
                // Handle successful response
                Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                saveUserToPreferences(email, password)
                startActivity(Intent(this, HomePageActivity::class.java))  // Navigate to home page
                finish()
            },
            { error ->
                // Handle error
                Toast.makeText(this, "Login failed. Try again!", Toast.LENGTH_SHORT).show()
            }) {
            override fun getBody(): ByteArray {
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

    // Save user email and password to SharedPreferences for session management
    private fun saveUserToPreferences(email: String, password: String) {
        val editor = sharedPreferences.edit()
        editor.putString("email", email)
        editor.putString("password", password)
        editor.apply()
    }
}
