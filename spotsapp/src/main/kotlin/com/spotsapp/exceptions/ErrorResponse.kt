package com.spotsapp.exceptions

import java.time.Instant

/**
 * Cuerpo JSON uniforme para todas las respuestas de error de la API.
 * `fieldErrors` solo se llena en errores de validación (400 por @Valid).
 */
data class ErrorResponse(
    val timestamp: Instant = Instant.now(),
    val status: Int,
    val error: String,
    val message: String,
    val path: String,
    val fieldErrors: Map<String, String>? = null
)
