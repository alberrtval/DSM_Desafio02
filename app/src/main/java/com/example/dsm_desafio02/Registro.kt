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
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase

class Registro : AppCompatActivity() {
    private lateinit var contenedorNombre: TextInputLayout
    private lateinit var contenedorCorreo: TextInputLayout
    private lateinit var contenedorContrasena: TextInputLayout
    private lateinit var contenedorConfirmar: TextInputLayout
    private lateinit var campoNombre: TextInputEditText
    private lateinit var campoCorreo: TextInputEditText
    private lateinit var campoContrasena: TextInputEditText
    private lateinit var campoConfirmar: TextInputEditText
    private lateinit var btnRegistrarse: MaterialButton
    private lateinit var txtIrALogin: TextView
    private lateinit var barraProgreso: ProgressBar
    private lateinit var autenticacion: FirebaseAuth

    override fun onCreate(estadoGuardado: Bundle?) {
        super.onCreate(estadoGuardado)
        setContentView(R.layout.activity_registro)

        autenticacion = FirebaseAuth.getInstance()
        inicializarVistas()
        asignarEscuchadores()
    }

    private fun inicializarVistas() {
        contenedorNombre    = findViewById(R.id.tilName)
        contenedorCorreo    = findViewById(R.id.tilEmail)
        contenedorContrasena = findViewById(R.id.tilPassword)
        contenedorConfirmar = findViewById(R.id.tilConfirmPassword)
        campoNombre         = findViewById(R.id.etName)
        campoCorreo         = findViewById(R.id.etEmail)
        campoContrasena     = findViewById(R.id.etPassword)
        campoConfirmar      = findViewById(R.id.etConfirmPassword)
        btnRegistrarse      = findViewById(R.id.btnRegister)
        txtIrALogin         = findViewById(R.id.tvGoToLogin)
        barraProgreso       = findViewById(R.id.progressBar)
    }

    private fun asignarEscuchadores() {
        btnRegistrarse.setOnClickListener { intentarRegistrarse() }
        txtIrALogin.setOnClickListener {
            startActivity(Intent(this, Login::class.java))
            finish()
        }
    }

    private fun intentarRegistrarse() {
        contenedorNombre.error    = null
        contenedorCorreo.error    = null
        contenedorContrasena.error = null
        contenedorConfirmar.error = null

        val nombre     = campoNombre.text.toString().trim()
        val correo     = campoCorreo.text.toString().trim()
        val contrasena = campoContrasena.text.toString().trim()
        val confirmar  = campoConfirmar.text.toString().trim()

        var formularioValido = true

        if (nombre.isEmpty()) {
            contenedorNombre.error = "El nombre es obligatorio"
            formularioValido = false
        } else if (nombre.length < 3) {
            contenedorNombre.error = "Mínimo 3 caracteres"
            formularioValido = false
        }

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

        if (confirmar.isEmpty()) {
            contenedorConfirmar.error = "Confirma tu contraseña"
            formularioValido = false
        } else if (contrasena != confirmar) {
            contenedorConfirmar.error = "Las contraseñas no coinciden"
            formularioValido = false
        }

        if (!formularioValido) return

        mostrarCargando(true)

        autenticacion.createUserWithEmailAndPassword(correo, contrasena)
            .addOnCompleteListener { tareaRegistro ->
                if (tareaRegistro.isSuccessful) {
                    val usuarioFirebase = autenticacion.currentUser ?: return@addOnCompleteListener

                    // Actualizar nombre de perfil
                    val actualizacionPerfil = UserProfileChangeRequest.Builder()
                        .setDisplayName(nombre)
                        .build()

                    usuarioFirebase.updateProfile(actualizacionPerfil)
                        .addOnCompleteListener {
                            // Guardar datos extra en Realtime Database
                            val datosUsuario = mapOf(
                                "nombre"        to nombre,
                                "correo"        to correo,
                                "fechaRegistro" to System.currentTimeMillis()
                            )
                            FirebaseDatabase.getInstance()
                                .getReference("usuarios")
                                .child(usuarioFirebase.uid)
                                .setValue(datosUsuario)
                                .addOnCompleteListener {
                                    mostrarCargando(false)
                                    val intencion = Intent(this, MainActivity::class.java).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    }
                                    startActivity(intencion)
                                    finish()
                                }
                        }
                } else {
                    mostrarCargando(false)
                    val mensajeError = obtenerMensajeErrorFirebase(tareaRegistro.exception)
                    Snackbar.make(btnRegistrarse, mensajeError, Snackbar.LENGTH_LONG)
                        .setBackgroundTint(getColor(R.color.borrarr))
                        .show()
                }
            }
    }

    private fun obtenerMensajeErrorFirebase(excepcion: Exception?): String {
        val mensaje = excepcion?.message ?: return "Error desconocido"
        return when {
            "email address is already in use" in mensaje -> "Este correo ya está registrado"
            "badly formatted" in mensaje -> "El correo electrónico no es válido"
            "network" in mensaje -> "Error de conexión. Verifica tu internet"
            else -> "Error al registrarse. Intenta de nuevo"
        }
    }

    private fun mostrarCargando(cargando: Boolean) {
        barraProgreso.visibility = if (cargando) View.VISIBLE else View.GONE
        btnRegistrarse.isEnabled = !cargando
        btnRegistrarse.text = if (cargando) "Creando cuenta..." else "Crear cuenta"
    }
}