package com.example.dsm_desafio02.adapters

import android.content.Context
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.dsm_desafio02.R
import com.example.dsm_desafio02.models.Tarea
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdaptadorTareas(
    private val contexto: Context,
    private var listaTareas: MutableList<Tarea>,
    private val escuchador: EscuchadorTarea
) : RecyclerView.Adapter<AdaptadorTareas.VistaHolderTarea>() {

    interface EscuchadorTarea {
        fun alCompletarTarea(tarea: Tarea, marcada: Boolean)
        fun alEditarTarea(tarea: Tarea)
        fun alEliminarTarea(tarea: Tarea)
    }

    override fun onCreateViewHolder(padre: ViewGroup, tipoVista: Int): VistaHolderTarea {
        val vista = LayoutInflater.from(contexto).inflate(R.layout.item_task, padre, false)
        return VistaHolderTarea(vista)
    }

    override fun onBindViewHolder(holder: VistaHolderTarea, posicion: Int) {
        val tarea = listaTareas[posicion]

        holder.txtTitulo.text = tarea.titulo

        // Descripción
        if (tarea.descripcion.isNotEmpty()) {
            holder.txtDescripcion.visibility = View.VISIBLE
            holder.txtDescripcion.text = tarea.descripcion
        } else {
            holder.txtDescripcion.visibility = View.GONE
        }

        // Fecha
        val formatoFecha = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        holder.txtFecha.text = formatoFecha.format(Date(tarea.fechaCreacion))

        // Evitar que el listener se dispare al hacer setState
        holder.chkCompletada.setOnCheckedChangeListener(null)
        holder.chkCompletada.isChecked = tarea.completada

        if (tarea.completada) {
            holder.txtTitulo.paintFlags = holder.txtTitulo.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.txtTitulo.setTextColor(contexto.getColor(R.color.text_secundario))
            holder.txtEstado.text = "✓ Completada"
            holder.txtEstado.setTextColor(contexto.getColor(R.color.tarea_completada_borde))
            (holder.itemView as CardView).setCardBackgroundColor(contexto.getColor(R.color.tarea_completada))
        } else {
            holder.txtTitulo.paintFlags = holder.txtTitulo.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            holder.txtTitulo.setTextColor(contexto.getColor(R.color.text_principal))
            holder.txtEstado.text = "● Pendiente"
            holder.txtEstado.setTextColor(contexto.getColor(R.color.tarea_pendite_borde))
            (holder.itemView as CardView).setCardBackgroundColor(contexto.getColor(R.color.white))
        }

        holder.chkCompletada.setOnCheckedChangeListener { _, marcada ->
            escuchador.alCompletarTarea(tarea, marcada)
        }

        holder.btnEditar.setOnClickListener { escuchador.alEditarTarea(tarea) }
        holder.btnEliminar.setOnClickListener { escuchador.alEliminarTarea(tarea) }
    }

    override fun getItemCount() = listaTareas.size

    fun actualizarLista(nuevaLista: MutableList<Tarea>) {
        listaTareas = nuevaLista
        notifyDataSetChanged()
    }

    // ── ViewHolder ────────────────────────────────────────────────────────────
    class VistaHolderTarea(vistaItem: View) : RecyclerView.ViewHolder(vistaItem) {
        val chkCompletada: CheckBox     = vistaItem.findViewById(R.id.cbCompleted)
        val txtTitulo: TextView         = vistaItem.findViewById(R.id.tvTitle)
        val txtDescripcion: TextView    = vistaItem.findViewById(R.id.tvDescription)
        val txtEstado: TextView         = vistaItem.findViewById(R.id.tvStatus)
        val txtFecha: TextView          = vistaItem.findViewById(R.id.tvDate)
        val btnEditar: ImageButton      = vistaItem.findViewById(R.id.btnEdit)
        val btnEliminar: ImageButton    = vistaItem.findViewById(R.id.btnDelete)
    }
}