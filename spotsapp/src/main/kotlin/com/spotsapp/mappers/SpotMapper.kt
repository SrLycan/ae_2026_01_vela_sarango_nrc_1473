package com.spotsapp.mappers

import com.spotsapp.dto.spot.SpotApproveRequest
import com.spotsapp.dto.spot.SpotCreateRequest
import com.spotsapp.dto.spot.SpotRejectRequest
import com.spotsapp.dto.spot.SpotResponse
import com.spotsapp.dto.spot.SpotUpdateRequest
import com.spotsapp.entities.Category
import com.spotsapp.entities.Spot
import com.spotsapp.entities.enums.SpotStatus
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class SpotMapper {

    /** SpotService.create() — status queda PENDING por defecto (valor por defecto de la entidad). */
    fun toEntity(request: SpotCreateRequest, category: Category, ownerUsername: String): Spot =
        Spot(
            name = request.name,
            description = request.description,
            latitude = request.latitude,
            longitude = request.longitude,
            address = request.address,
            category = category,
            ownerUsername = ownerUsername
        )

    /** SpotService.update() — reemplazo completo de campos editables por el propietario (PUT, ADR-004). */
    fun applyUpdate(spot: Spot, request: SpotUpdateRequest, category: Category) {
        spot.name = request.name
        spot.description = request.description
        spot.latitude = request.latitude
        spot.longitude = request.longitude
        spot.address = request.address
        spot.category = category
        spot.updatedAt = Instant.now()
    }

    /** SpotService.approve() — solo ADMIN, PATCH (ADR-004). */
    fun applyApprove(spot: Spot, request: SpotApproveRequest) {
        spot.status = SpotStatus.APPROVED
        spot.rarity = request.rarity
        spot.pointsReward = request.pointsReward
        spot.rejectionReason = null
        spot.updatedAt = Instant.now()
    }

    /** SpotService.reject() — solo ADMIN, PATCH (ADR-004). */
    fun applyReject(spot: Spot, request: SpotRejectRequest) {
        spot.status = SpotStatus.REJECTED
        spot.rejectionReason = request.rejectionReason
        spot.rarity = null
        spot.pointsReward = null
        spot.updatedAt = Instant.now()
    }

    fun toResponse(spot: Spot): SpotResponse {
        val catId = spot.category.id
            ?: throw IllegalStateException("La categoría del spot '${spot.name}' no tiene ID")
        val spotId = spot.id
            ?: throw IllegalStateException("El spot '${spot.name}' no tiene ID después de persistir")

        return SpotResponse(
            id = spotId,
            name = spot.name,
            description = spot.description,
            latitude = spot.latitude,
            longitude = spot.longitude,
            address = spot.address,
            categoryId = catId,
            categoryName = spot.category.name,
            ownerUsername = spot.ownerUsername,
            status = spot.status,
            rarity = spot.rarity,
            pointsReward = spot.pointsReward,
            rejectionReason = spot.rejectionReason,
            createdAt = spot.createdAt,
            updatedAt = spot.updatedAt
        )
    }
}
