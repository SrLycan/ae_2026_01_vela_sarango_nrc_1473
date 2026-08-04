package com.spotsapp.dto.review

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant

data class ReviewCreateRequest(
    @field:NotNull
    val spotId: Long,

    @field:NotNull
    @field:Min(1) @field:Max(5)
    val rating: Int,

    @field:Size(max = 1000)
    val comment: String? = null
)

data class ReviewUpdateRequest(
    @field:NotNull
    @field:Min(1) @field:Max(5)
    val rating: Int,

    @field:Size(max = 1000)
    val comment: String? = null
)

data class ReviewResponse(
    val id: Long,
    val spotId: Long,
    val username: String,
    val rating: Int,
    val comment: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)
