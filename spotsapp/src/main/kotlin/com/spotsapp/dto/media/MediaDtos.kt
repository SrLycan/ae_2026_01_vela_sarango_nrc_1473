package com.spotsapp.dto.media

import com.fasterxml.jackson.annotation.JsonFormat
import com.spotsapp.entities.enums.MediaType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.Instant

/** POST /spots/{id}/media — paso 1: pedir la URL prefirmada (ver ADR-001). */
data class MediaPresignRequest(
    @field:NotBlank
    val fileName: String,

    @field:NotBlank
    val contentType: String,

    @field:NotNull
    val type: MediaType
)

data class MediaPresignResponse(
    val uploadUrl: String,
    val publicUrl: String,
    val expiresInSeconds: Long
)

/** POST /spots/{id}/media — paso 2: confirmar la URL final tras subir el archivo a S3. */
data class MediaConfirmRequest(
    @field:NotBlank
    val url: String,

    @field:NotNull
    val type: MediaType
)

data class MediaResponse(
    val id: Long,
    val spotId: Long,
    val url: String,
    val type: MediaType,
    val uploadedByUsername: String,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    val createdAt: Instant
)
