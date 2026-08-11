package com.spotsapp.controllers

import com.spotsapp.entities.enums.ProfileImageType
import com.spotsapp.exceptions.ForbiddenOperationException
import com.spotsapp.services.ProfileImageService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

/**
 * Foto de perfil y banner del usuario, guardados en Postgres (ver ProfileImage.kt) — no en S3.
 *
 * - PUT /profile/me/avatar y /profile/me/banner: requieren sesión, el username sale del JWT
 *   (mismo patrón que SpotController.resolveUsername).
 * - GET /profile/{username}/avatar y /banner: públicos, devuelven los bytes de la imagen con su
 *   Content-Type real, para poder usarlos directo como `model` de AsyncImage/Coil en el cliente.
 */
@RestController
@RequestMapping("/profile")
class ProfileImageController(
    private val profileImageService: ProfileImageService
) {

    companion object {
        private val log = LoggerFactory.getLogger(ProfileImageController::class.java)

        private fun resolveUsername(authentication: Authentication?): String {
            if (authentication == null) {
                throw ForbiddenOperationException(
                    "No se pudo identificar al usuario autenticado. Verifica que el token JWT contenga el claim 'username'."
                )
            }
            return authentication.name ?: throw ForbiddenOperationException(
                "No se pudo identificar al usuario autenticado. Verifica que el token JWT contenga el claim 'username'."
            )
        }
    }

    @PutMapping("/me/avatar", consumes = ["multipart/form-data"])
    fun uploadMyAvatar(
        authentication: Authentication?,
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<Void> {
        val username = resolveUsername(authentication)
        log.info("Subiendo avatar de {}", username)
        profileImageService.upload(username, ProfileImageType.AVATAR, file)
        return ResponseEntity.noContent().build()
    }

    @PutMapping("/me/banner", consumes = ["multipart/form-data"])
    fun uploadMyBanner(
        authentication: Authentication?,
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<Void> {
        val username = resolveUsername(authentication)
        log.info("Subiendo banner de {}", username)
        profileImageService.upload(username, ProfileImageType.BANNER, file)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{username}/avatar")
    fun getAvatar(@PathVariable username: String): ResponseEntity<ByteArray> = serveImage(username, ProfileImageType.AVATAR)

    @GetMapping("/{username}/banner")
    fun getBanner(@PathVariable username: String): ResponseEntity<ByteArray> = serveImage(username, ProfileImageType.BANNER)

    private fun serveImage(username: String, type: ProfileImageType): ResponseEntity<ByteArray> {
        val image = profileImageService.get(username, type)
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, image.contentType)
            .header(HttpHeaders.CACHE_CONTROL, "public, max-age=300")
            .body(image.imageData)
    }
}
