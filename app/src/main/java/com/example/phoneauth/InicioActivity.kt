package com.example.phoneauth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class InicioActivity : AppCompatActivity() {
    lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_inicio)

        val emailInput = findViewById<EditText>(R.id.email_input)
        val passwordInput = findViewById<EditText>(R.id.password_input)
        val buttonInicioSesion = findViewById<Button>(R.id.button_inicio_sesion)
        val linkRegistrate = findViewById<TextView>(R.id.link_registrate)
        auth = FirebaseAuth.getInstance()

        seters(buttonInicioSesion, linkRegistrate, emailInput, passwordInput)
    }
    private fun seters(buttonInicioSesion: Button, linkRegistrate: TextView, emailInput : EditText, passwordInput : EditText) {
        buttonInicioSesion.setOnClickListener {
            login(emailInput, passwordInput)
        }
        linkRegistrate.setOnClickListener {
            startActivity(Intent(this, RegistroActivity::class.java))
        }
    }
    private fun login(emailInput: EditText, passwordInput: EditText) {
        val email = emailInput.text.toString()
        val pass = passwordInput.text.toString()

        auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener(this) {
            if (it.isSuccessful) {
                Toast.makeText(this, "Successfully LoggedIn", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MapsActivity::class.java))
            } else
                Toast.makeText(this, "Log In failed ", Toast.LENGTH_SHORT).show()
        }
    }
}