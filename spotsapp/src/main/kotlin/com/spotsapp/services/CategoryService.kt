package com.spotsapp.services

import com.spotsapp.dto.category.CategoryCreateRequest
import com.spotsapp.dto.category.CategoryResponse
import com.spotsapp.dto.category.CategoryUpdateRequest
import com.spotsapp.exceptions.BusinessRuleException
import com.spotsapp.exceptions.ResourceNotFoundException
import com.spotsapp.mappers.CategoryMapper
import com.spotsapp.repositories.CategoryRepository
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
        if (categoryRepository.existsByName(request.name)) {
            throw BusinessRuleException("Ya existe una categoría con el nombre '${request.name}'")
        }
        val saved = categoryRepository.save(categoryMapper.toEntity(request))
        return categoryMapper.toResponse(saved)
    }

    fun update(id: Long, request: CategoryUpdateRequest): CategoryResponse {
        val category = findEntityOrThrow(id)

        val nameTaken = categoryRepository.findByName(request.name)?.let { it.id != id } ?: false
        if (nameTaken) {
            throw BusinessRuleException("Ya existe una categoría con el nombre '${request.name}'")
        }

        categoryMapper.applyUpdate(category, request)
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
