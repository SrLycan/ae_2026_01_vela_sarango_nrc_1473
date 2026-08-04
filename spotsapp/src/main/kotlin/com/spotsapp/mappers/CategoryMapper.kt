package com.spotsapp.mappers

import com.spotsapp.dto.category.CategoryCreateRequest
import com.spotsapp.dto.category.CategoryResponse
import com.spotsapp.dto.category.CategoryUpdateRequest
import com.spotsapp.entities.Category
import org.springframework.stereotype.Component

@Component
class CategoryMapper {

    fun toEntity(request: CategoryCreateRequest): Category =
        Category(
            name = request.name,
            description = request.description
        )

    /** Muta la entidad en sitio con los datos del PUT — usada por CategoryService.update(). */
    fun applyUpdate(category: Category, request: CategoryUpdateRequest) {
        category.name = request.name
        category.description = request.description
    }

    fun toResponse(category: Category): CategoryResponse =
        CategoryResponse(
            id = requireNotNull(category.id),
            name = category.name,
            description = category.description,
            createdAt = category.createdAt
        )
}
