package com.spotsapp.exceptions

/**
 * Se lanza cuando se busca una entidad (Category, Spot, Media, Review, Follow) por id/clave
 * y no existe. Traducida a 404 por el GlobalExceptionHandler.
 */
class ResourceNotFoundException(message: String) : RuntimeException(message) {

    companion object {
        /** Atajo común: "Spot no encontrado con id 42". */
        fun of(entity: String, id: Any): ResourceNotFoundException =
            ResourceNotFoundException("$entity no encontrado con id $id")
    }
}
