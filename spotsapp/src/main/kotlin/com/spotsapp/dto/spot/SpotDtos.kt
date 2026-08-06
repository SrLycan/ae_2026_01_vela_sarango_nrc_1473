package com.spotsapp.dto.spot

import com.fasterxml.jackson.annotation.JsonFormat
import com.spotsapp.entities.enums.Rarity
import com.spotsapp.entities.enums.SpotStatus
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant

data class SpotCreateRequest(
    @field:NotBlank
    @field:Size(max = 120)
    val name: String,

    @field:NotBlank
    @field:Size(max = 1000)
    val description: String,

    @field:NotNull
    @field:Min(-90) @field:Max(90)
    val latitude: Double,

    @field:NotNull
    @field:Min(-180) @field:Max(180)
    val longitude: Double,

    @field:NotBlank
    @field:Size(max = 255)
    val address: String,

    @field:NotNull
    val categoryId: Long
)

/** Reemplazo completo por el propietario — PUT, ver ADR-004. No permite tocar status/rarity/pointsReward. */
data class SpotUpdateRequest(
    @field:NotBlank
    @field:Size(max = 120)
    val name: String,

    @field:NotBlank
    @field:Size(max = 1000)
    val description: String,

    @field:NotNull
    @field:Min(-90) @field:Max(90)
    val latitude: Double,

    @field:NotNull
    @field:Min(-180) @field:Max(180)
    val longitude: Double,

    @field:NotBlank
    @field:Size(max = 255)
    val address: String,

    @field:NotNull
    val categoryId: Long
)

/** Transición de estado por ADMIN — PATCH /spots/{id}/approve, ver ADR-004. */
data class SpotApproveRequest(
    @field:NotNull
    val rarity: Rarity,

    @field:NotNull
    @field:Min(0)
    val pointsReward: Int
)

/** Transición de estado por ADMIN — PATCH /spots/{id}/reject, ver ADR-004. */
data class SpotRejectRequest(
    @field:NotBlank
    @field:Size(max = 500)
    val rejectionReason: String
)

data class SpotResponse(
    val id: Long,
    val name: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val categoryId: Long,
    val categoryName: String,
    val ownerUsername: String,
    val status: SpotStatus,
    val rarity: Rarity?,
    val pointsReward: Int?,
    val rejectionReason: String?,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    val createdAt: Instant,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    val updatedAt: Instant
)
