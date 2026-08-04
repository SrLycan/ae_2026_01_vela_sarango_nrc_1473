package com.spotsapp.mappers

import com.spotsapp.dto.media.MediaConfirmRequest
import com.spotsapp.dto.media.MediaResponse
import com.spotsapp.entities.Media
import com.spotsapp.entities.Spot
import org.springframework.stereotype.Component

@Component
class MediaMapper {

    /** MediaService — registro final tras confirmar la subida a S3 (ADR-001). */
    fun toEntity(request: MediaConfirmRequest, spot: Spot, uploadedByUsername: String): Media =
        Media(
            spot = spot,
            url = request.url,
            type = request.type,
            uploadedByUsername = uploadedByUsername
        )

    fun toResponse(media: Media): MediaResponse =
        MediaResponse(
            id = requireNotNull(media.id),
            spotId = requireNotNull(media.spot.id),
            url = media.url,
            type = media.type,
            uploadedByUsername = media.uploadedByUsername,
            createdAt = media.createdAt
        )
}
