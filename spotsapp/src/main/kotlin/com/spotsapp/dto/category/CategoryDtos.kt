package com.spotsapp.dto.category

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CategoryCreateRequest(
    @field:NotBlank
    @field:Size(max = 60)
    val name: String,

    @field:Size(max = 255)
    val description: String? = null
)

data class CategoryUpdateRequest(
    @field:NotBlank
    @field:Size(max = 60)
    val name: String,

    @field:Size(max = 255)
    val description: String? = null
)

data class CategoryResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val createdAt: java.time.Instant
)
