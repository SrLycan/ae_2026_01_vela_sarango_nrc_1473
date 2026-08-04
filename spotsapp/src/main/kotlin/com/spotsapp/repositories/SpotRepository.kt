package com.spotsapp.repositories

import com.spotsapp.entities.Spot
import com.spotsapp.entities.enums.Rarity
import com.spotsapp.entities.enums.SpotStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface SpotRepository : JpaRepository<Spot, Long> {

    // SpotService.listMine()
    fun findByOwnerUsername(ownerUsername: String, pageable: Pageable): Page<Spot>

    // SpotService.listApproved() — sin filtros
    fun findByStatus(status: SpotStatus, pageable: Pageable): Page<Spot>

    // SpotService.listApproved() — filtro por categoría
    fun findByStatusAndCategoryId(status: SpotStatus, categoryId: Long, pageable: Pageable): Page<Spot>

    // SpotService.listApproved() — filtro por rareza
    fun findByStatusAndRarity(status: SpotStatus, rarity: Rarity, pageable: Pageable): Page<Spot>

    // SpotService.listApproved() — filtro por categoría + rareza
    fun findByStatusAndCategoryIdAndRarity(
        status: SpotStatus,
        categoryId: Long,
        rarity: Rarity,
        pageable: Pageable
    ): Page<Spot>

    // FeedService — spots recientes de las cuentas seguidas por el usuario actual
    fun findByOwnerUsernameInAndStatusOrderByCreatedAtDesc(
        ownerUsernames: List<String>,
        status: SpotStatus,
        pageable: Pageable
    ): Page<Spot>

    // Validaciones de propiedad (SpotService.update()/delete())
    fun existsByIdAndOwnerUsername(id: Long, ownerUsername: String): Boolean
}
