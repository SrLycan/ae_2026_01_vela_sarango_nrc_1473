package com.spotsapp.services

import com.spotsapp.entities.ProfileImage
import com.spotsapp.entities.enums.ProfileImageType
import com.spotsapp.exceptions.BusinessRuleException
import com.spotsapp.exceptions.ResourceNotFoundException
import com.spotsapp.repositories.ProfileImageRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.springframework.web.multipart.MultipartFile
import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProfileImageServiceTest {

    private val profileImageRepository = mockk<ProfileImageRepository>()
    private val service = ProfileImageService(profileImageRepository)

    private fun mockFile(
        isEmpty: Boolean = false,
        size: Long = 1024L,
        contentType: String? = "image/jpeg",
        bytes: ByteArray = byteArrayOf(1, 2, 3, 4)
    ): MultipartFile {
        val file = mockk<MultipartFile>()
        every { file.isEmpty } returns isEmpty
        every { file.size } returns size
        every { file.contentType } returns contentType
        every { file.bytes } returns bytes
        return file
    }

    @Test
    fun `upload guarda una nueva imagen cuando el usuario no tiene una previa`() {
        val file = mockFile(contentType = "image/JPEG") // normaliza a minúsculas
        every { profileImageRepository.findByUsernameAndImageType("ana", ProfileImageType.AVATAR) } returns Optional.empty()
        val savedSlot = slot<ProfileImage>()
        every { profileImageRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

        val saved = service.upload("ana", ProfileImageType.AVATAR, file)

        assertEquals("ana", saved.username)
        assertEquals(ProfileImageType.AVATAR, saved.imageType)
        assertEquals("image/jpeg", saved.contentType)
        assertContentEquals(byteArrayOf(1, 2, 3, 4), saved.imageData)
        assertEquals(0L, saved.id)
        verify(exactly = 1) { profileImageRepository.save(any()) }
    }

    @Test
    fun `upload reemplaza la imagen previa conservando su id`() {
        val file = mockFile(bytes = byteArrayOf(9, 9))
        val existing = ProfileImage(
            id = 7L, username = "ana", imageType = ProfileImageType.AVATAR,
            contentType = "image/png", imageData = byteArrayOf(1)
        )
        every { profileImageRepository.findByUsernameAndImageType("ana", ProfileImageType.AVATAR) } returns Optional.of(existing)
        val savedSlot = slot<ProfileImage>()
        every { profileImageRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

        service.upload("ana", ProfileImageType.AVATAR, file)

        assertEquals(7L, savedSlot.captured.id)
        assertContentEquals(byteArrayOf(9, 9), savedSlot.captured.imageData)
    }

    @Test
    fun `upload lanza BusinessRuleException si el archivo esta vacio`() {
        val file = mockFile(isEmpty = true)

        assertFailsWith<BusinessRuleException> { service.upload("ana", ProfileImageType.AVATAR, file) }
        verify(exactly = 0) { profileImageRepository.save(any()) }
    }

    @Test
    fun `upload lanza BusinessRuleException si supera los 5 MB`() {
        val file = mockFile(size = 5L * 1024 * 1024 + 1)

        assertFailsWith<BusinessRuleException> { service.upload("ana", ProfileImageType.AVATAR, file) }
    }

    @Test
    fun `upload lanza BusinessRuleException si el content type no esta soportado`() {
        val file = mockFile(contentType = "image/gif")

        assertFailsWith<BusinessRuleException> { service.upload("ana", ProfileImageType.AVATAR, file) }
    }

    @Test
    fun `upload lanza BusinessRuleException si el content type es null`() {
        val file = mockFile(contentType = null)

        assertFailsWith<BusinessRuleException> { service.upload("ana", ProfileImageType.AVATAR, file) }
    }

    @Test
    fun `get retorna la imagen cuando existe`() {
        val image = ProfileImage(
            id = 7L, username = "ana", imageType = ProfileImageType.BANNER,
            contentType = "image/webp", imageData = byteArrayOf(5)
        )
        every { profileImageRepository.findByUsernameAndImageType("ana", ProfileImageType.BANNER) } returns Optional.of(image)

        val result = service.get("ana", ProfileImageType.BANNER)

        assertEquals(image, result)
    }

    @Test
    fun `get lanza ResourceNotFoundException cuando no existe`() {
        every { profileImageRepository.findByUsernameAndImageType("ana", ProfileImageType.AVATAR) } returns Optional.empty()

        assertFailsWith<ResourceNotFoundException> { service.get("ana", ProfileImageType.AVATAR) }
    }

    @Test
    fun `exists retorna true cuando el usuario tiene la imagen`() {
        every { profileImageRepository.existsByUsernameAndImageType("ana", ProfileImageType.AVATAR) } returns true

        assertTrue(service.exists("ana", ProfileImageType.AVATAR))
    }

    @Test
    fun `exists retorna false cuando el usuario no tiene la imagen`() {
        every { profileImageRepository.existsByUsernameAndImageType("ana", ProfileImageType.AVATAR) } returns false

        assertFalse(service.exists("ana", ProfileImageType.AVATAR))
    }
}
