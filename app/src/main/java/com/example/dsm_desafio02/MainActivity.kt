package com.example.dsm_desafio02

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dsm_desafio02.R
import com.example.dsm_desafio02.adapters.AdaptadorTareas
import com.example.dsm_desafio02.models.Tarea
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MainActivity : AppCompatActivity(), AdaptadorTareas.EscuchadorTarea {
    // Vistas
    private lateinit var txtNombreUsuario: TextView
    private lateinit var txtResumenTareas: TextView
    private lateinit var listaTareasVista: RecyclerView
    private lateinit var contenedorVacio: LinearLayout
    private lateinit var barraProgreso: ProgressBar
    private lateinit var botonAgregarTarea: FloatingActionButton

    // Firebase
    private lateinit var autenticacion: FirebaseAuth
    private lateinit var referenciaTareas: DatabaseReference
    private var escuchadorTareas: ValueEventListener? = null

    // Datos
    private lateinit var adaptador: AdaptadorTareas
    private val listaTareas = mutableListOf<Tarea>()
    private lateinit var idUsuarioActual: String

    override fun onCreate(estadoGuardado: Bundle?) {
        super.onCreate(estadoGuardado)
        setContentView(R.layout.activity_main)

        autenticacion = FirebaseAuth.getInstance()
        val usuarioActual = autenticacion.currentUser ?: run { irALogin(); return }

        idUsuarioActual  = usuarioActual.uid
        referenciaTareas = FirebaseDatabase.getInstance()
            .getReference("tareas")
            .child(idUsuarioActual)

        inicializarVistas()
        configurarBarraHerramientas()
        configurarListaTareas()
        mostrarInfoUsuario()
        cargarTareas()

        botonAgregarTarea.setOnClickListener { mostrarDialogoTarea(null) }
    }

    private fun inicializarVistas() {
        txtNombreUsuario  = findViewById(R.id.tvUserName)
        txtResumenTareas  = findViewById(R.id.tvTaskSummary)
        listaTareasVista  = findViewById(R.id.recyclerTasks)
        contenedorVacio   = findViewById(R.id.layoutEmpty)
        barraProgreso     = findViewById(R.id.progressBar)
        botonAgregarTarea = findViewById(R.id.fabAddTask)
    }

    private fun configurarBarraHerramientas() {
        val barraHerramientas = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(barraHerramientas)
    }

    private fun configurarListaTareas() {
        adaptador = AdaptadorTareas(this, listaTareas, this)
        listaTareasVista.layoutManager = LinearLayoutManager(this)
        listaTareasVista.adapter = adaptador
    }

    private fun mostrarInfoUsuario() {
        val usuario = autenticacion.currentUser ?: return
        val nombre = if (!usuario.displayName.isNullOrEmpty()) usuario.displayName!! else usuario.email ?: ""
        txtNombreUsuario.text = nombre
    }

    private fun cargarTareas() {
        barraProgreso.visibility = View.VISIBLE

        escuchadorTareas = object : ValueEventListener {
            override fun onDataChange(instantanea: DataSnapshot) {
                listaTareas.clear()

                for (instantaneaTarea in instantanea.children) {
                    val tarea = instantaneaTarea.getValue(Tarea::class.java)
                    tarea?.let {
                        it.id = instantaneaTarea.key ?: ""
                        listaTareas.add(it)
                    }
                }

                // Pendientes primero, luego más recientes arriba
                listaTareas.sortWith(compareBy<Tarea> { it.completada }.thenByDescending { it.fechaCreacion })

                barraProgreso.visibility = View.GONE
                adaptador.actualizarLista(listaTareas)
                actualizarInterfaz()
            }

            override fun onCancelled(error: DatabaseError) {
                barraProgreso.visibility = View.GONE
                mostrarMensaje("Error al cargar tareas: ${error.message}", esError = true)
            }
        }

        referenciaTareas.addValueEventListener(escuchadorTareas!!)
    }

    private fun actualizarInterfaz() {
        val cantidadPendientes = listaTareas.count { !it.completada }

        if (listaTareas.isEmpty()) {
            listaTareasVista.visibility = View.GONE
            contenedorVacio.visibility  = View.VISIBLE
            txtResumenTareas.text = "No tienes tareas aún"
        } else {
            listaTareasVista.visibility = View.VISIBLE
            contenedorVacio.visibility  = View.GONE
            txtResumenTareas.text = "$cantidadPendientes tarea(s) pendiente(s)"
        }
    }

    // ── Diálogo agregar / editar ──────────────────────────────────────────────

    private fun mostrarDialogoTarea(tareaAEditar: Tarea?) {
        val vistaDialogo = layoutInflater.inflate(R.layout.activity_dialog_add_task, null)

        val contenedorTitulo      = vistaDialogo.findViewById<TextInputLayout>(R.id.tilTitle)
        val campoTitulo           = vistaDialogo.findViewById<TextInputEditText>(R.id.etTitle)
        val campoDescripcion      = vistaDialogo.findViewById<TextInputEditText>(R.id.etDescription)

        val estaEditando = tareaAEditar != null
        if (estaEditando) {
            campoTitulo.setText(tareaAEditar!!.titulo)
            campoDescripcion.setText(tareaAEditar.descripcion)
        }

        val dialogo = MaterialAlertDialogBuilder(this)
            .setTitle(if (estaEditando) "Editar tarea" else "➕ Nueva tarea")
            .setView(vistaDialogo)
            .setPositiveButton(if (estaEditando) "Guardar cambios" else "Agregar", null)
            .setNegativeButton("Cancelar") { d, _ -> d.dismiss() }
            .create()

        dialogo.setOnShowListener {
            dialogo.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                contenedorTitulo.error = null

                val titulo      = campoTitulo.text.toString().trim()
                val descripcion = campoDescripcion.text.toString().trim()

                when {
                    titulo.isEmpty() -> {
                        contenedorTitulo.error = "El título es obligatorio"
                        return@setOnClickListener
                    }
                    titulo.length < 3 -> {
                        contenedorTitulo.error = "Mínimo 3 caracteres"
                        return@setOnClickListener
                    }
                    titulo.length > 100 -> {
                        contenedorTitulo.error = "Máximo 100 caracteres"
                        return@setOnClickListener
                    }
                }

                if (estaEditando) actualizarTarea(tareaAEditar!!, titulo, descripcion)
                else agregarTarea(titulo, descripcion)

                dialogo.dismiss()
            }
        }

        dialogo.show()
    }

    // ── CRUD Firebase ─────────────────────────────────────────────────────────

    private fun agregarTarea(titulo: String, descripcion: String) {
        val idNuevaTarea = referenciaTareas.push().key ?: return
        val nuevaTarea   = Tarea(
            id           = idNuevaTarea,
            titulo       = titulo,
            descripcion  = descripcion,
            idUsuario    = idUsuarioActual
        )
        referenciaTareas.child(idNuevaTarea).setValue(nuevaTarea)
            .addOnSuccessListener { mostrarMensaje("Tarea agregada") }
            .addOnFailureListener { mostrarMensaje("Error al agregar la tarea", esError = true) }
    }

    private fun actualizarTarea(tarea: Tarea, nuevoTitulo: String, nuevaDescripcion: String) {
        val cambios = mapOf("titulo" to nuevoTitulo, "descripcion" to nuevaDescripcion)
        referenciaTareas.child(tarea.id).updateChildren(cambios)
            .addOnSuccessListener { mostrarMensaje("Tarea actualizada") }
            .addOnFailureListener { mostrarMensaje("Error al actualizar", esError = true) }
    }

    private fun eliminarTarea(tarea: Tarea) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Eliminar tarea")
            .setMessage("¿Deseas eliminar \"${tarea.titulo}\"?")
            .setPositiveButton("Eliminar") { _, _ ->
                referenciaTareas.child(tarea.id).removeValue()
                    .addOnSuccessListener { mostrarMensaje("🗑️ Tarea eliminada") }
                    .addOnFailureListener { mostrarMensaje("Error al eliminar", esError = true) }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }


    override fun alCompletarTarea(tarea: Tarea, marcada: Boolean) {
        referenciaTareas.child(tarea.id).child("completada").setValue(marcada)
            .addOnSuccessListener {
                val mensaje = if (marcada) "✅ Tarea completada" else "🔄 Marcada como pendiente"
                mostrarMensaje(mensaje)
            }
    }

    override fun alEditarTarea(tarea: Tarea) = mostrarDialogoTarea(tarea)

    override fun alEliminarTarea(tarea: Tarea) = eliminarTarea(tarea)


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, 1, 0, "Cerrar sesión")
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        return true
    }

    override fun onOptionsItemSelected(elementoMenu: MenuItem): Boolean {
        if (elementoMenu.itemId == 1) {
            confirmarCierreSesion()
            return true
        }
        return super.onOptionsItemSelected(elementoMenu)
    }

    private fun confirmarCierreSesion() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Cerrar sesión")
            .setMessage("¿Deseas cerrar tu sesión?")
            .setPositiveButton("Sí, salir") { _, _ ->
                escuchadorTareas?.let { referenciaTareas.removeEventListener(it) }
                autenticacion.signOut()
                irALogin()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun irALogin() {
        val intencion = Intent(this, Login::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intencion)
        finish()
    }

    private fun mostrarMensaje(mensaje: String, esError: Boolean = false) {
        val color = if (esError) getColor(R.color.borrarr) else getColor(R.color.tarea_completada_borde)
        Snackbar.make(botonAgregarTarea, mensaje, Snackbar.LENGTH_SHORT)
            .setBackgroundTint(color)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        escuchadorTareas?.let { referenciaTareas.removeEventListener(it) }
    }
}