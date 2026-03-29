package com.example.dsm_desafio02.models

data class Tarea(
    var id: String = "",
    var titulo: String = "",
    var descripcion: String = "",
    var completada: Boolean = false,
    var fechaCreacion: Long = System.currentTimeMillis(),
    var idUsuario: String = ""
) {
    constructor() : this("", "", "", false, System.currentTimeMillis(), "")
}