package com.spotsapp.controllers

import com.spotsapp.dto.media.MediaConfirmRequest
import com.spotsapp.dto.media.MediaPresignRequest
import com.spotsapp.dto.media.MediaPresignResponse
import com.spotsapp.dto.media.MediaResponse
import com.spotsapp.services.MediaService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * RF-05 — subida vía URL prefirmada de S3 (ADR-001), en dos pasos: /presign (pide la URL de
 * subida) y /confirm (registra la URL final una vez que el cliente ya subió el archivo).
 */
@RestController
class MediaController(
    private val mediaService: MediaService
) {

    /** Público — multimedia de un spot. */
    @GetMapping("/spots/{spotId}/media")
    fun listBySpot(@PathVariable spotId: Long): List<MediaResponse> = mediaService.listBySpot(spotId)

    /** USER/ADMIN + propiedad del spot — paso 1: obtener la URL prefirmada de subida. */
    @PostMapping("/spots/{spotId}/media/presign")
    fun presign(
        @PathVariable spotId: Long,
        @Valid @RequestBody request: MediaPresignRequest,
        authentication: Authentication
    ): MediaPresignResponse = mediaService.presign(spotId, request, authentication.name)

    /** USER/ADMIN + propiedad del spot — paso 2: confirmar la URL final tras subir a S3. */
    @PostMapping("/spots/{spotId}/media/confirm")
    @ResponseStatus(HttpStatus.CREATED)
    fun confirm(
        @PathVariable spotId: Long,
        @Valid @RequestBody request: MediaConfirmRequest,
        authentication: Authentication
    ): MediaResponse = mediaService.confirm(spotId, request, authentication.name)

    /** USER/ADMIN + propiedad del spot. */
    @DeleteMapping("/media/{id}")
    fun delete(@PathVariable id: Long, authentication: Authentication): ResponseEntity<Void> {
        mediaService.delete(id, authentication.name)
        return ResponseEntity.noContent().build()
    }
}
