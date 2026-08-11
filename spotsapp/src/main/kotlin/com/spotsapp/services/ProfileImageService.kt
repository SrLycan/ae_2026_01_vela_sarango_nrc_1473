package com.spotsapp.services

import com.spotsapp.entities.ProfileImage
import com.spotsapp.entities.enums.ProfileImageType
import com.spotsapp.exceptions.BusinessRuleException
import com.spotsapp.exceptions.ResourceNotFoundException
import com.spotsapp.repositories.ProfileImageRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.Instant

/**
 * Guarda/lee el avatar y el banner del perfil directamente en Postgres (columna BYTEA), sin
 * pasar por S3. Reemplaza (no acumula) la imagen anterior del mismo tipo para el mismo usuario.
 */
@Service
@Transactional(readOnly = true)
class ProfileImageService(
    private val profileImageRepository: ProfileImageRepository
) {

    companion object {
        private val log = LoggerFactory.getLogger(ProfileImageService::class.java)
        private const val MAX_SIZE_BYTES = 5L * 1024 * 1024 // 5 MB — suficiente para foto de perfil/banner
        private val ALLOWED_CONTENT_TYPES = setOf("image/jpeg", "image/png", "image/webp")
    }

    @Transactional
    fun upload(username: String, imageType: ProfileImageType, file: MultipartFile): ProfileImage {
        if (file.isEmpty) {
            throw BusinessRuleException("El archivo enviado está vacío.")
        }
        if (file.size > MAX_SIZE_BYTES) {
            throw BusinessRuleException("La imagen supera el tamaño máximo permitido (5 MB).")
        }
        val contentType = file.contentType?.lowercase()
        if (contentType == null || contentType !in ALLOWED_CONTENT_TYPES) {
            throw BusinessRuleException("Formato de imagen no soportado. Usa JPEG, PNG o WEBP.")
        }

        log.info("Guardando {} de {} ({} bytes)", imageType, username, file.size)

        val existing = profileImageRepository.findByUsernameAndImageType(username, imageType)
        val entity = ProfileImage(
            id = existing.map { it.id }.orElse(0L),
            username = username,
            imageType = imageType,
            contentType = contentType,
            imageData = file.bytes,
            updatedAt = Instant.now()
        )
        return profileImageRepository.save(entity)
    }

    fun get(username: String, imageType: ProfileImageType): ProfileImage =
        profileImageRepository.findByUsernameAndImageType(username, imageType).orElseThrow {
            ResourceNotFoundException("El usuario '$username' no tiene ${imageType.name.lowercase()} configurado.")
        }

    fun exists(username: String, imageType: ProfileImageType): Boolean =
        profileImageRepository.existsByUsernameAndImageType(username, imageType)
}
