package com.spotsapp.exceptions

/**
 * Se lanza al intentar crear una segunda Review del mismo usuario sobre el mismo Spot,
 * violando el constraint único (spot_id, username). Traducida a 409 por el GlobalExceptionHandler.
 */
class DuplicateReviewException(spotId: Long, username: String) :
    RuntimeException("El usuario '$username' ya tiene una reseña para el spot $spotId")
