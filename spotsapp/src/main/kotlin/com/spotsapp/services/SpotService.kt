package com.spotsapp.services

import com.spotsapp.dto.spot.SpotApproveRequest
import com.spotsapp.dto.spot.SpotCreateRequest
import com.spotsapp.dto.spot.SpotRejectRequest
import com.spotsapp.dto.spot.SpotResponse
import com.spotsapp.dto.spot.SpotUpdateRequest
import com.spotsapp.entities.enums.Rarity
import com.spotsapp.entities.enums.SpotStatus
import com.spotsapp.exceptions.BusinessRuleException
import com.spotsapp.exceptions.ForbiddenOperationException
import com.spotsapp.exceptions.ResourceNotFoundException
import com.spotsapp.mappers.SpotMapper
import com.spotsapp.repositories.CategoryRepository
import com.spotsapp.repositories.SpotRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Ciclo de vida de Spot (RF-03, RF-04). La propiedad (owner == usuario actual) se valida acá
 * porque depende del dato, no de la ruta — SecurityConfig (Fase 6) solo puede filtrar por rol.
 */
@Service
@Transactional
class SpotService(
    private val spotRepository: SpotRepository,
    private val categoryRepository: CategoryRepository,
    private val spotMapper: SpotMapper
) {

    companion object {
        private val log = LoggerFactory.getLogger(SpotService::class.java)
    }

    /** RF-03 — el spot siempre nace PENDING (valor por defecto de la entidad, ver SpotMapper).
     * `@Transactional` explícito aquí (además del de clase) para garantizar transacción
     * al leer la categoría y persistir el spot, incluso con proxies CGLIB de Spring.
     */
    @Transactional
    fun create(request: SpotCreateRequest, ownerUsername: String): SpotResponse {
        if (ownerUsername.isBlank()) {
            throw ForbiddenOperationException("No se pudo identificar al usuario autenticado")
        }
        log.debug("Creando spot '{}' para usuario '{}'", request.name, ownerUsername)
        val category = findCategoryOrThrow(request.categoryId)
        val spot = spotMapper.toEntity(request, category, ownerUsername)
        val saved = spotRepository.save(spot)
        log.info("Spot '{}' (id={}) creado exitosamente por '{}'", saved.name, saved.id, ownerUsername)
        return spotMapper.toResponse(saved)
    }

    fun update(id: Long, request: SpotUpdateRequest, currentUsername: String): SpotResponse {
        val spot = findEntityOrThrow(id)
        requireOwner(spot.ownerUsername, currentUsername)

        val category = findCategoryOrThrow(request.categoryId)
        spotMapper.applyUpdate(spot, request, category)
        return spotMapper.toResponse(spotRepository.save(spot))
    }

    fun delete(id: Long, currentUsername: String) {
        val spot = findEntityOrThrow(id)
        requireOwner(spot.ownerUsername, currentUsername)
        spotRepository.delete(spot)
    }

    /** RF-04 — solo ADMIN (SecurityConfig). Solo tiene sentido sobre un spot PENDING. */
    fun approve(id: Long, request: SpotApproveRequest): SpotResponse {
        val spot = findEntityOrThrow(id)
        if (spot.status != SpotStatus.PENDING) {
            throw BusinessRuleException("Solo se pueden aprobar spots en estado PENDING (actual: ${spot.status})")
        }
        spotMapper.applyApprove(spot, request)
        return spotMapper.toResponse(spotRepository.save(spot))
    }

    /** RF-04 — solo ADMIN (SecurityConfig). Solo tiene sentido sobre un spot PENDING. */
    fun reject(id: Long, request: SpotRejectRequest): SpotResponse {
        val spot = findEntityOrThrow(id)
        if (spot.status != SpotStatus.PENDING) {
            throw BusinessRuleException("Solo se pueden rechazar spots en estado PENDING (actual: ${spot.status})")
        }
        spotMapper.applyReject(spot, request)
        return spotMapper.toResponse(spotRepository.save(spot))
    }

    @Transactional(readOnly = true)
    fun getById(id: Long): SpotResponse = spotMapper.toResponse(findEntityOrThrow(id))

    @Transactional(readOnly = true)
    fun listApproved(categoryId: Long?, rarity: Rarity?, pageable: Pageable): Page<SpotResponse> {
        val page = when {
            categoryId != null && rarity != null ->
                spotRepository.findByStatusAndCategoryIdAndRarity(SpotStatus.APPROVED, categoryId, rarity, pageable)
            categoryId != null ->
                spotRepository.findByStatusAndCategoryId(SpotStatus.APPROVED, categoryId, pageable)
            rarity != null ->
                spotRepository.findByStatusAndRarity(SpotStatus.APPROVED, rarity, pageable)
            else ->
                spotRepository.findByStatus(SpotStatus.APPROVED, pageable)
        }
        return page.map(spotMapper::toResponse)
    }

    @Transactional(readOnly = true)
    fun listMine(ownerUsername: String, pageable: Pageable): Page<SpotResponse> =
        spotRepository.findByOwnerUsername(ownerUsername, pageable).map(spotMapper::toResponse)

    /** Fase 10.7 — solo ADMIN (SecurityConfig). Lista spots PENDING para moderar. */
    @Transactional(readOnly = true)
    fun listPending(pageable: Pageable): Page<SpotResponse> =
        spotRepository.findByStatus(SpotStatus.PENDING, pageable).map(spotMapper::toResponse)

    private fun requireOwner(ownerUsername: String, currentUsername: String) {
        if (ownerUsername != currentUsername) {
            throw ForbiddenOperationException("No puedes modificar un spot que no te pertenece")
        }
    }

    private fun findEntityOrThrow(id: Long) =
        spotRepository.findById(id).orElseThrow { ResourceNotFoundException.of("Spot", id) }

    private fun findCategoryOrThrow(categoryId: Long) =
        categoryRepository.findById(categoryId).orElseThrow { ResourceNotFoundException.of("Category", categoryId) }
}
