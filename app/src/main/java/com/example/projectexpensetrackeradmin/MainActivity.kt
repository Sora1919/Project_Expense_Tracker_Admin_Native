package com.example.projectexpensetrackeradmin

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.projectexpensetrackeradmin.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ensureAnonymousAuth()

        binding.btnEnterApp.setOnClickListener {
            startActivity(Intent(this, ProjectListActivity::class.java))
        }
    }

    private fun ensureAnonymousAuth() {
        val auth = FirebaseAuth.getInstance()

        // Already signed in
        if (auth.currentUser != null) return

        auth.signInAnonymously()
            .addOnSuccessListener {
                // Optional: show small success message once
                // Snackbar.make(binding.root, "Signed in (anonymous)", Snackbar.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Snackbar.make(binding.root, "Firebase auth failed: ${e.message}", Snackbar.LENGTH_LONG).show()
            }
    }
}