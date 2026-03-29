package com.example.dsm_desafio02

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.dsm_desafio02.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth

class Login : AppCompatActivity() {
    private lateinit var contenedorCorreo: TextInputLayout
    private lateinit var contenedorContrasena: TextInputLayout
    private lateinit var campoCorreo: TextInputEditText
    private lateinit var campoContrasena: TextInputEditText
    private lateinit var btnIniciarSesion: MaterialButton
    private lateinit var txtIrARegistro: TextView
    private lateinit var barraProgreso: ProgressBar
    private lateinit var autenticacion: FirebaseAuth

    override fun onCreate(estadoGuardado: Bundle?) {
        super.onCreate(estadoGuardado)
        setContentView(R.layout.activity_login)

        autenticacion = FirebaseAuth.getInstance()
        inicializarVistas()
        asignarEscuchadores()
    }

    private fun inicializarVistas() {
        contenedorCorreo     = findViewById(R.id.tilEmail)
        contenedorContrasena = findViewById(R.id.tilPassword)
        campoCorreo          = findViewById(R.id.etEmail)
        campoContrasena      = findViewById(R.id.etPassword)
        btnIniciarSesion     = findViewById(R.id.btnLogin)
        txtIrARegistro       = findViewById(R.id.tvGoToRegister)
        barraProgreso        = findViewById(R.id.progressBar)
    }

    private fun asignarEscuchadores() {
        btnIniciarSesion.setOnClickListener { intentarIniciarSesion() }
        txtIrARegistro.setOnClickListener {
            startActivity(Intent(this, Registro::class.java))
        }
    }

    private fun intentarIniciarSesion() {
        contenedorCorreo.error = null
        contenedorContrasena.error = null

        val correo     = campoCorreo.text.toString().trim()
        val contrasena = campoContrasena.text.toString().trim()

        var formularioValido = true

        if (correo.isEmpty()) {
            contenedorCorreo.error = "El correo es obligatorio"
            formularioValido = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            contenedorCorreo.error = "Ingresa un correo válido"
            formularioValido = false
        }

        if (contrasena.isEmpty()) {
            contenedorContrasena.error = "La contraseña es obligatoria"
            formularioValido = false
        } else if (contrasena.length < 6) {
            contenedorContrasena.error = "Mínimo 6 caracteres"
            formularioValido = false
        }

        if (!formularioValido) return

        mostrarCargando(true)

        autenticacion.signInWithEmailAndPassword(correo, contrasena)
            .addOnCompleteListener { tarea ->
                mostrarCargando(false)
                if (tarea.isSuccessful) {
                    val intencion = Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intencion)
                    finish()
                } else {
                    val mensajeError = obtenerMensajeErrorFirebase(tarea.exception)
                    Snackbar.make(btnIniciarSesion, mensajeError, Snackbar.LENGTH_LONG)
                        .setBackgroundTint(getColor(R.color.borrarr))
                        .show()
                }
            }
    }

    private fun obtenerMensajeErrorFirebase(excepcion: Exception?): String {
        val mensaje = excepcion?.message ?: return "Error desconocido"
        return when {
            "no user record" in mensaje || "user-not-found" in mensaje ->
                "No existe una cuenta con ese correo"
            "password is invalid" in mensaje || "wrong-password" in mensaje ->
                "Contraseña incorrecta"
            "badly formatted" in mensaje ->
                "El correo electrónico no es válido"
            "network" in mensaje ->
                "Error de conexión. Verifica tu internet"
            "too-many-requests" in mensaje ->
                "Demasiados intentos. Intenta más tarde"
            else -> "Error al iniciar sesión. Verifica tus datos"
        }
    }

    private fun mostrarCargando(cargando: Boolean) {
        barraProgreso.visibility = if (cargando) View.VISIBLE else View.GONE
        btnIniciarSesion.isEnabled = !cargando
        btnIniciarSesion.text = if (cargando) "Ingresando..." else "Iniciar Sesión"
    }
}