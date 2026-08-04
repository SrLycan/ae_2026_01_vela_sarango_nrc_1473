package com.spotsapp.services

import com.spotsapp.dto.media.MediaConfirmRequest
import com.spotsapp.dto.media.MediaPresignRequest
import com.spotsapp.dto.media.MediaPresignResponse
import com.spotsapp.dto.media.MediaResponse
import com.spotsapp.entities.Spot
import com.spotsapp.exceptions.ForbiddenOperationException
import com.spotsapp.exceptions.ResourceNotFoundException
import com.spotsapp.mappers.MediaMapper
import com.spotsapp.repositories.MediaRepository
import com.spotsapp.repositories.SpotRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.time.Duration
import java.util.UUID

/**
 * Subida de multimedia vía URL prefirmada de S3 (RF-05, ADR-001). El binario nunca pasa por
 * el backend: acá solo se genera la URL de subida (presign) y luego se registra la URL final
 * que confirma el cliente (confirm).
 */
@Service
@Transactional
class MediaService(
    private val mediaRepository: MediaRepository,
    private val spotRepository: SpotRepository,
    private val mediaMapper: MediaMapper,
    private val s3Presigner: S3Presigner,
    @Value("\${aws.s3.bucket}") private val bucket: String,
    @Value("\${aws.s3.region}") private val region: String
) {

    companion object {
        private val PRESIGN_DURATION: Duration = Duration.ofMinutes(10)
        private const val EXPIRES_IN_SECONDS = 600L
    }

    /** Paso 1 — genera la URL de subida. Valida que el spot exista y sea del usuario actual. */
    @Transactional(readOnly = true)
    fun presign(spotId: Long, request: MediaPresignRequest, currentUsername: String): MediaPresignResponse {
        val spot = findSpotOrThrow(spotId)
        requireSpotOwner(spot, currentUsername)

        val key = "spots/$spotId/${UUID.randomUUID()}-${request.fileName}"

        val putObjectRequest = PutObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .contentType(request.contentType)
            .build()

        val presignRequest = PutObjectPresignRequest.builder()
            .signatureDuration(PRESIGN_DURATION)
            .putObjectRequest(putObjectRequest)
            .build()

        val presigned = s3Presigner.presignPutObject(presignRequest)

        return MediaPresignResponse(
            uploadUrl = presigned.url().toString(),
            publicUrl = "https://$bucket.s3.$region.amazonaws.com/$key",
            expiresInSeconds = EXPIRES_IN_SECONDS
        )
    }

    /** Paso 2 — registra la URL final tras confirmar que el archivo ya se subió a S3. */
    fun confirm(spotId: Long, request: MediaConfirmRequest, currentUsername: String): MediaResponse {
        val spot = findSpotOrThrow(spotId)
        requireSpotOwner(spot, currentUsername)

        val media = mediaMapper.toEntity(request, spot, currentUsername)
        return mediaMapper.toResponse(mediaRepository.save(media))
    }

    fun delete(mediaId: Long, currentUsername: String) {
        val media = mediaRepository.findById(mediaId)
            .orElseThrow { ResourceNotFoundException.of("Media", mediaId) }

        if (media.spot.ownerUsername != currentUsername) {
            throw ForbiddenOperationException("No puedes borrar multimedia de un spot que no te pertenece")
        }
        mediaRepository.delete(media)
    }

    @Transactional(readOnly = true)
    fun listBySpot(spotId: Long): List<MediaResponse> {
        findSpotOrThrow(spotId)
        return mediaRepository.findBySpotId(spotId).map(mediaMapper::toResponse)
    }

    private fun requireSpotOwner(spot: Spot, currentUsername: String) {
        if (spot.ownerUsername != currentUsername) {
            throw ForbiddenOperationException("No puedes agregar multimedia a un spot que no te pertenece")
        }
    }

    private fun findSpotOrThrow(spotId: Long) =
        spotRepository.findById(spotId).orElseThrow { ResourceNotFoundException.of("Spot", spotId) }
}
