package com.spotsapp.services

import com.spotsapp.dto.media.MediaPresignRequest
import com.spotsapp.entities.Category
import com.spotsapp.entities.Media
import com.spotsapp.entities.Spot
import com.spotsapp.entities.enums.MediaType
import com.spotsapp.exceptions.ForbiddenOperationException
import com.spotsapp.exceptions.ResourceNotFoundException
import com.spotsapp.mappers.MediaMapper
import com.spotsapp.repositories.MediaRepository
import com.spotsapp.repositories.SpotRepository
import io.mockk.every
import io.mockk.mockk
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest
import java.net.URI
import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MediaServiceTest {

    private val mediaRepository = mockk<MediaRepository>()
    private val spotRepository = mockk<SpotRepository>()
    private val mediaMapper = MediaMapper()
    private val s3Presigner = mockk<S3Presigner>()
    private val service = MediaService(
        mediaRepository, spotRepository, mediaMapper, s3Presigner,
        bucket = "spots-app-dev", region = "us-east-1"
    )

    private val category = Category(id = 1L, name = "Miradores")

    private fun spot(ownerUsername: String = "ricardo") = Spot(
        id = 10L, name = "Mirador X", description = "desc", latitude = 0.0, longitude = 0.0,
        address = "dir", category = category, ownerUsername = ownerUsername
    )

    @Test
    fun `presign genera la url cuando el usuario es el propietario del spot`() {
        every { spotRepository.findById(10L) } returns Optional.of(spot())

        val presigned = mockk<PresignedPutObjectRequest>()
        every { presigned.url() } returns URI.create("https://spots-app-dev.s3.us-east-1.amazonaws.com/spots/10/foto.jpg").toURL()
        every { s3Presigner.presignPutObject(any()) } returns presigned

        val request = MediaPresignRequest(fileName = "foto.jpg", contentType = "image/jpeg", type = MediaType.IMAGE)
        val response = service.presign(10L, request, "ricardo")

        assertEquals(600L, response.expiresInSeconds)
        assertEquals("https://spots-app-dev.s3.us-east-1.amazonaws.com/spots/10/foto.jpg", response.uploadUrl)
    }

    @Test
    fun `presign lanza ForbiddenOperationException si el spot no es del usuario`() {
        every { spotRepository.findById(10L) } returns Optional.of(spot(ownerUsername = "ricardo"))

        val request = MediaPresignRequest(fileName = "foto.jpg", contentType = "image/jpeg", type = MediaType.IMAGE)

        assertFailsWith<ForbiddenOperationException> { service.presign(10L, request, "otro-usuario") }
    }

    @Test
    fun `delete lanza ResourceNotFoundException si la media no existe`() {
        every { mediaRepository.findById(5L) } returns Optional.empty()

        assertFailsWith<ResourceNotFoundException> { service.delete(5L, "ricardo") }
    }

    @Test
    fun `delete lanza ForbiddenOperationException si el spot de la media no es del usuario`() {
        val media = Media(id = 5L, spot = spot(ownerUsername = "ricardo"), url = "url", type = MediaType.IMAGE, uploadedByUsername = "ricardo")
        every { mediaRepository.findById(5L) } returns Optional.of(media)

        assertFailsWith<ForbiddenOperationException> { service.delete(5L, "otro-usuario") }
    }
}
