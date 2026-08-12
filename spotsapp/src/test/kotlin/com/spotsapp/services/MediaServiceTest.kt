package com.spotsapp.services

import com.spotsapp.dto.media.MediaConfirmRequest
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
import io.mockk.slot
import io.mockk.verify
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
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
        every { s3Presigner.presignPutObject(any<PutObjectPresignRequest>()) } returns presigned

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
    fun `presign lanza ResourceNotFoundException si el spot no existe`() {
        every { spotRepository.findById(99L) } returns Optional.empty()

        val request = MediaPresignRequest(fileName = "foto.jpg", contentType = "image/jpeg", type = MediaType.IMAGE)

        assertFailsWith<ResourceNotFoundException> { service.presign(99L, request, "ricardo") }
    }

    @Test
    fun `confirm registra la media cuando el usuario es dueno del spot`() {
        every { spotRepository.findById(10L) } returns Optional.of(spot())
        val mediaSlot = slot<Media>()
        every { mediaRepository.save(capture(mediaSlot)) } answers {
            Media(
                id = 5L, spot = mediaSlot.captured.spot, url = mediaSlot.captured.url,
                type = mediaSlot.captured.type, uploadedByUsername = mediaSlot.captured.uploadedByUsername
            )
        }

        val request = MediaConfirmRequest(url = "https://s3.amazonaws.com/spots/10/foto.jpg", type = MediaType.IMAGE)
        val response = service.confirm(10L, request, "ricardo")

        assertEquals(5L, response.id)
        assertEquals("https://s3.amazonaws.com/spots/10/foto.jpg", response.url)
        assertEquals(MediaType.IMAGE, response.type)
        assertEquals("ricardo", response.uploadedByUsername)
    }

    @Test
    fun `confirm lanza ForbiddenOperationException si el spot no es del usuario`() {
        every { spotRepository.findById(10L) } returns Optional.of(spot())

        val request = MediaConfirmRequest(url = "u", type = MediaType.IMAGE)

        assertFailsWith<ForbiddenOperationException> { service.confirm(10L, request, "otro-usuario") }
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

    @Test
    fun `delete elimina la media cuando el usuario es dueno del spot`() {
        val media = Media(id = 5L, spot = spot(ownerUsername = "ricardo"), url = "url", type = MediaType.IMAGE, uploadedByUsername = "ricardo")
        every { mediaRepository.findById(5L) } returns Optional.of(media)
        every { mediaRepository.delete(any()) } returns Unit

        service.delete(5L, "ricardo")

        verify(exactly = 1) { mediaRepository.delete(media) }
    }

    @Test
    fun `listBySpot retorna las medias del spot`() {
        every { spotRepository.findById(10L) } returns Optional.of(spot())
        val media = Media(id = 5L, spot = spot(), url = "url", type = MediaType.IMAGE, uploadedByUsername = "ricardo")
        every { mediaRepository.findBySpotId(10L) } returns listOf(media)

        val responses = service.listBySpot(10L)

        assertEquals(1, responses.size)
        assertEquals(5L, responses.first().id)
    }

    @Test
    fun `listBySpot lanza ResourceNotFoundException si el spot no existe`() {
        every { spotRepository.findById(99L) } returns Optional.empty()

        assertFailsWith<ResourceNotFoundException> { service.listBySpot(99L) }
    }
}
