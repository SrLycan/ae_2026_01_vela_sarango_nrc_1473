package com.spotsapp.services

import com.spotsapp.dto.category.CategoryCreateRequest
import com.spotsapp.dto.category.CategoryUpdateRequest
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
import kotlin.test.assertNull

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
    fun `create normaliza el nombre y limpia la descripcion en blanco`() {
        val request = CategoryCreateRequest(name = "  Miradores  ", description = "   ")
        val savedSlot = slot<Category>()

        every { categoryRepository.existsByName("Miradores") } returns false
        every { categoryRepository.save(capture(savedSlot)) } answers {
            Category(id = 1L, name = savedSlot.captured.name, description = savedSlot.captured.description)
        }

        val response = service.create(request)

        assertEquals("Miradores", response.name)
        assertNull(response.description)
    }

    @Test
    fun `create lanza BusinessRuleException si el nombre ya existe`() {
        val request = CategoryCreateRequest(name = "Miradores", description = null)
        every { categoryRepository.existsByName("Miradores") } returns true

        assertFailsWith<BusinessRuleException> { service.create(request) }
        verify(exactly = 0) { categoryRepository.save(any()) }
    }

    @Test
    fun `update actualiza la categoria cuando el nombre no esta en uso`() {
        val category = Category(id = 1L, name = "Viejo", description = "antes")
        every { categoryRepository.findById(1L) } returns Optional.of(category)
        every { categoryRepository.findByName("Nuevo") } returns null
        every { categoryRepository.save(any()) } answers { firstArg() }

        val response = service.update(1L, CategoryUpdateRequest(name = "Nuevo", description = "después"))

        assertEquals("Nuevo", response.name)
        assertEquals("después", response.description)
    }

    @Test
    fun `update lanza BusinessRuleException si el nombre pertenece a otra categoria`() {
        val category = Category(id = 1L, name = "Viejo")
        every { categoryRepository.findById(1L) } returns Optional.of(category)
        every { categoryRepository.findByName("Nuevo") } returns Category(id = 2L, name = "Nuevo")

        assertFailsWith<BusinessRuleException> {
            service.update(1L, CategoryUpdateRequest(name = "Nuevo", description = null))
        }
        verify(exactly = 0) { categoryRepository.save(any()) }
    }

    @Test
    fun `update permite conservar el mismo nombre de la categoria`() {
        val category = Category(id = 1L, name = "Miradores")
        every { categoryRepository.findById(1L) } returns Optional.of(category)
        every { categoryRepository.findByName("Miradores") } returns category
        every { categoryRepository.save(any()) } answers { firstArg() }

        val response = service.update(1L, CategoryUpdateRequest(name = "Miradores", description = "x"))

        assertEquals("Miradores", response.name)
    }

    @Test
    fun `update lanza ResourceNotFoundException si la categoria no existe`() {
        every { categoryRepository.findById(99L) } returns Optional.empty()

        assertFailsWith<ResourceNotFoundException> {
            service.update(99L, CategoryUpdateRequest(name = "Nuevo", description = null))
        }
    }

    @Test
    fun `delete elimina la categoria cuando existe`() {
        val category = Category(id = 1L, name = "Miradores")
        every { categoryRepository.findById(1L) } returns Optional.of(category)
        every { categoryRepository.delete(any()) } returns Unit

        service.delete(1L)

        verify(exactly = 1) { categoryRepository.delete(category) }
    }

    @Test
    fun `delete lanza ResourceNotFoundException si la categoria no existe`() {
        every { categoryRepository.findById(99L) } returns Optional.empty()

        assertFailsWith<ResourceNotFoundException> { service.delete(99L) }
        verify(exactly = 0) { categoryRepository.delete(any()) }
    }

    @Test
    fun `getById retorna la categoria`() {
        val category = Category(id = 1L, name = "Miradores", description = "Vistas")
        every { categoryRepository.findById(1L) } returns Optional.of(category)

        val response = service.getById(1L)

        assertEquals("Miradores", response.name)
        assertEquals(1L, response.id)
    }

    @Test
    fun `listAll retorna todas las categorias`() {
        every { categoryRepository.findAll() } returns
            listOf(Category(id = 1L, name = "Miradores"), Category(id = 2L, name = "Gastronomía"))

        val responses = service.listAll()

        assertEquals(2, responses.size)
        assertEquals("Miradores", responses[0].name)
        assertEquals("Gastronomía", responses[1].name)
    }
}
