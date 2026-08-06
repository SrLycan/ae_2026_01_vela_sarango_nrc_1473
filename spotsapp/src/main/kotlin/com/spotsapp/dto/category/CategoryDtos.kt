package com.spotsapp.dto.category

import com.fasterxml.jackson.annotation.JsonFormat
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
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    val createdAt: java.time.Instant
)
