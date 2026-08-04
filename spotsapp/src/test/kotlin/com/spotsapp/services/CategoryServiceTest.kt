package com.spotsapp.services

import com.spotsapp.dto.category.CategoryCreateRequest
import com.spotsapp.entities.Category
import com.spotsapp.exceptions.BusinessRuleException
import com.spotsapp.exceptions.ResourceNotFoundException
import com.spotsapp.mappers.CategoryMapper
import com.spotsapp.repositories.CategoryRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CategoryServiceTest {

    private val categoryRepository = mockk<CategoryRepository>()
    private val categoryMapper = CategoryMapper() // sin dependencias, se usa la implementación real
    private val service = CategoryService(categoryRepository, categoryMapper)

    @Test
    fun `create guarda la categoria cuando el nombre no existe`() {
        val request = CategoryCreateRequest(name = "Miradores", description = "Vistas panorámicas")
        val savedSlot = slot<Category>()

        every { categoryRepository.existsByName("Miradores") } returns false
        every { categoryRepository.save(capture(savedSlot)) } answers {
            Category(id = 1L, name = savedSlot.captured.name, description = savedSlot.captured.description)
        }

        val response = service.create(request)

        assertEquals("Miradores", response.name)
        assertEquals(1L, response.id)
        verify(exactly = 1) { categoryRepository.save(any()) }
    }

    @Test
    fun `create lanza BusinessRuleException si el nombre ya existe`() {
        val request = CategoryCreateRequest(name = "Miradores", description = null)
        every { categoryRepository.existsByName("Miradores") } returns true

        assertFailsWith<BusinessRuleException> { service.create(request) }
        verify(exactly = 0) { categoryRepository.save(any()) }
    }

    @Test
    fun `delete lanza ResourceNotFoundException si la categoria no existe`() {
        every { categoryRepository.findById(99L) } returns Optional.empty()

        assertFailsWith<ResourceNotFoundException> { service.delete(99L) }
        verify(exactly = 0) { categoryRepository.delete(any()) }
    }
}
