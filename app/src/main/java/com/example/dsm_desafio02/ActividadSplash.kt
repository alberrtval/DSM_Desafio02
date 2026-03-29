package com.example.dsm_desafio02

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class ActividadSplash : AppCompatActivity() {

    companion object{
        private const val DURACION_SPLASH = 2000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_actividad_splash)
        Handler(Looper.getMainLooper()).postDelayed({
            val usuarioActual = FirebaseAuth.getInstance().currentUser
            val intencion = if (usuarioActual != null) {
                Intent(this, MainActivity::class.java)
            } else {
                Intent(this, Login::class.java)
            }
            startActivity(intencion)
            finish()
        }, DURACION_SPLASH)
    }
}