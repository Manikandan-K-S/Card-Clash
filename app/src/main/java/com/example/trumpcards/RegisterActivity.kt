package com.example.trumpcards

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
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

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialize UI elements
        val etEmail = findViewById<EditText>(R.id.et_email)
        val etName = findViewById<EditText>(R.id.et_name)
        val etPassword = findViewById<EditText>(R.id.et_password)
        val etConfirmPassword = findViewById<EditText>(R.id.et_confirm_password)
        val btnRegister = findViewById<Button>(R.id.btn_register)
        val tvLoginLink = findViewById<TextView>(R.id.tv_login_link)

        // Register button click listener
        btnRegister.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val name = etName.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            if (email.isEmpty() || name.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            registerUser(email, name, password)
        }

        // Login link click listener
        tvLoginLink.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun registerUser(email: String, name: String, password: String) {
        val url = "${ApiConfig.BASE_URL}/register"  // Use the URL from ApiConfig

        val requestQueue = Volley.newRequestQueue(this)

        val jsonBody = JSONObject().apply {
            put("email", email)
            put("name", name)
            put("pwd", password)
        }

        val stringRequest = object : StringRequest(Request.Method.POST, url,
            { response ->
                // Handle successful response
                Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
                saveUserToPreferences(email, password)
                startActivity(Intent(this, HomePageActivity::class.java))
                finish()
            },
            { error ->
                // Log the error details
                Log.e("RegisterActivity", "Error: ${error.localizedMessage}")
                if (error.networkResponse != null) {
                    val errorResponse = String(error.networkResponse.data)
                    Log.e("RegisterActivity", "Server error response: $errorResponse")
                }
                Toast.makeText(this, "Registration failed. Try again!", Toast.LENGTH_SHORT).show()
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

    private fun saveUserToPreferences(email: String, password: String) {
        val sharedPreferences: SharedPreferences = getSharedPreferences("userPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("email", email)
        editor.putString("password", password)
        editor.apply()
    }
}
