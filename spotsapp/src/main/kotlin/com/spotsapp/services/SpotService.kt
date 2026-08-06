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
import com.spotsapp.utils.haversineMeters
import com.spotsapp.utils.normalizeSpaces
import com.spotsapp.utils.roundToCoordinatePrecision
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

        // Estados que "cuentan" para el chequeo de ubicación duplicada — un spot REJECTED
        // no bloquea al usuario de volver a intentar en el mismo punto.
        private val ACTIVE_STATUSES = listOf(SpotStatus.PENDING, SpotStatus.APPROVED)

        // Radio de tolerancia para "misma ubicación" (RN nueva). 20m cubre el caso de dos
        // pines que representan el mismo lugar real pero no cayeron en coordenadas idénticas
        // (uno elegido por Places Autocomplete, otro arrastrando el pin del mapa) — sigue
        // siendo lo bastante chico para no bloquear dos spots legítimamente distintos que
        // están cerca (ej. dos locales en la misma plaza).
        private const val DUPLICATE_RADIUS_METERS = 20.0
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
        val normalized = request.copy(
            name = request.name.normalizeSpaces(),
            description = request.description.normalizeSpaces(),
            address = request.address.normalizeSpaces(),
            latitude = request.latitude.roundToCoordinatePrecision(),
            longitude = request.longitude.roundToCoordinatePrecision()
        )
        if (hasNearbyActiveSpot(ownerUsername, normalized.latitude, normalized.longitude)) {
            throw BusinessRuleException(
                "Ya tienes un spot pendiente o aprobado a menos de ${DUPLICATE_RADIUS_METERS.toInt()}m de esa ubicación"
            )
        }
        log.debug("Creando spot '{}' para usuario '{}'", normalized.name, ownerUsername)
        val category = findCategoryOrThrow(normalized.categoryId)
        val spot = spotMapper.toEntity(normalized, category, ownerUsername)
        val saved = spotRepository.save(spot)
        log.info("Spot '{}' (id={}) creado exitosamente por '{}'", saved.name, saved.id, ownerUsername)
        return spotMapper.toResponse(saved)
    }

    fun update(id: Long, request: SpotUpdateRequest, currentUsername: String): SpotResponse {
        val spot = findEntityOrThrow(id)
        requireOwner(spot.ownerUsername, currentUsername)

        val normalized = request.copy(
            name = request.name.normalizeSpaces(),
            description = request.description.normalizeSpaces(),
            address = request.address.normalizeSpaces(),
            latitude = request.latitude.roundToCoordinatePrecision(),
            longitude = request.longitude.roundToCoordinatePrecision()
        )
        if (hasNearbyActiveSpot(currentUsername, normalized.latitude, normalized.longitude, excludeId = id)) {
            throw BusinessRuleException(
                "Ya tienes otro spot pendiente o aprobado a menos de ${DUPLICATE_RADIUS_METERS.toInt()}m de esa ubicación"
            )
        }
        val category = findCategoryOrThrow(normalized.categoryId)
        spotMapper.applyUpdate(spot, normalized, category)
        return spotMapper.toResponse(spotRepository.save(spot))
    }

    fun delete(id: Long, currentUsername: String, isAdmin: Boolean) {
        val spot = findEntityOrThrow(id)
        requireOwnerOrAdmin(spot.ownerUsername, currentUsername, isAdmin)
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

    /** true si el usuario ya tiene un spot PENDING/APPROVED a <= DUPLICATE_RADIUS_METERS de ahí. */
    private fun hasNearbyActiveSpot(
        ownerUsername: String, latitude: Double, longitude: Double, excludeId: Long? = null
    ): Boolean =
        spotRepository.findByOwnerUsernameAndStatusIn(ownerUsername, ACTIVE_STATUSES)
            .filter { it.id != excludeId }
            .any { haversineMeters(it.latitude, it.longitude, latitude, longitude) <= DUPLICATE_RADIUS_METERS }

    /**
     * Igual que requireOwner(), pero además deja pasar a un ADMIN aunque no sea el dueño —
     * usado solo en delete(): un admin puede borrar cualquier spot (moderación de contenido),
     * pero editar (update()) sigue siendo exclusivo del dueño, eso no cambió.
     */
    private fun requireOwnerOrAdmin(ownerUsername: String, currentUsername: String, isAdmin: Boolean) {
        if (ownerUsername != currentUsername && !isAdmin) {
            throw ForbiddenOperationException("No puedes eliminar un spot que no te pertenece")
        }
    }

    private fun findEntityOrThrow(id: Long) =
        spotRepository.findById(id).orElseThrow { ResourceNotFoundException.of("Spot", id) }

    private fun findCategoryOrThrow(categoryId: Long) =
        categoryRepository.findById(categoryId).orElseThrow { ResourceNotFoundException.of("Category", categoryId) }
}
