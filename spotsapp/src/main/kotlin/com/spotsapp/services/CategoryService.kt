package com.spotsapp.services

import com.spotsapp.dto.category.CategoryCreateRequest
import com.spotsapp.dto.category.CategoryResponse
import com.spotsapp.dto.category.CategoryUpdateRequest
import com.spotsapp.exceptions.BusinessRuleException
import com.spotsapp.exceptions.ResourceNotFoundException
import com.spotsapp.mappers.CategoryMapper
import com.spotsapp.repositories.CategoryRepository
import com.spotsapp.utils.normalizeSpaces
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * CRUD de Category (RF-02). La restricción "solo ADMIN puede crear/editar/borrar" se aplica
 * a nivel de endpoint en SecurityConfig (Fase 6) — este service asume que ya se llegó
 * autorizado y se enfoca en las reglas de negocio propias de la entidad.
 */
@Service
@Transactional
class CategoryService(
    private val categoryRepository: CategoryRepository,
    private val categoryMapper: CategoryMapper
) {

    fun create(request: CategoryCreateRequest): CategoryResponse {
        val normalized = request.copy(
            name = request.name.normalizeSpaces(),
            description = request.description?.normalizeSpaces()?.ifBlank { null }
        )
        if (categoryRepository.existsByName(normalized.name)) {
            throw BusinessRuleException("Ya existe una categoría con el nombre '${normalized.name}'")
        }
        val saved = categoryRepository.save(categoryMapper.toEntity(normalized))
        return categoryMapper.toResponse(saved)
    }

    fun update(id: Long, request: CategoryUpdateRequest): CategoryResponse {
        val category = findEntityOrThrow(id)
        val normalized = request.copy(
            name = request.name.normalizeSpaces(),
            description = request.description?.normalizeSpaces()?.ifBlank { null }
        )

        val nameTaken = categoryRepository.findByName(normalized.name)?.let { it.id != id } ?: false
        if (nameTaken) {
            throw BusinessRuleException("Ya existe una categoría con el nombre '${normalized.name}'")
        }

        categoryMapper.applyUpdate(category, normalized)
        return categoryMapper.toResponse(categoryRepository.save(category))
    }

    fun delete(id: Long) {
        val category = findEntityOrThrow(id)
        categoryRepository.delete(category)
    }

    @Transactional(readOnly = true)
    fun getById(id: Long): CategoryResponse = categoryMapper.toResponse(findEntityOrThrow(id))

    @Transactional(readOnly = true)
    fun listAll(): List<CategoryResponse> =
        categoryRepository.findAll().map(categoryMapper::toResponse)

    private fun findEntityOrThrow(id: Long) =
        categoryRepository.findById(id).orElseThrow { ResourceNotFoundException.of("Category", id) }
}
