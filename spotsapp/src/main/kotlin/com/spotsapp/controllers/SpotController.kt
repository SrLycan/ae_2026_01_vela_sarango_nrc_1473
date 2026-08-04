package com.spotsapp.controllers

import com.spotsapp.dto.spot.SpotApproveRequest
import com.spotsapp.dto.spot.SpotCreateRequest
import com.spotsapp.dto.spot.SpotRejectRequest
import com.spotsapp.dto.spot.SpotResponse
import com.spotsapp.dto.spot.SpotUpdateRequest
import com.spotsapp.entities.enums.Rarity
import com.spotsapp.exceptions.ForbiddenOperationException
import com.spotsapp.services.SpotService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * RF-03 / RF-04. "/me" se declara antes que "/{id}" por legibilidad — Spring MVC ya
 * prioriza la coincidencia exacta sobre la variable de ruta, así que el orden no es
 * estrictamente necesario, pero deja explícita la intención.
 */
@RestController
@RequestMapping("/spots")
class SpotController(
    private val spotService: SpotService
) {

    companion object {
        private val log = LoggerFactory.getLogger(SpotController::class.java)

        private fun resolveUsername(authentication: Authentication?): String {
            if (authentication == null) {
                log.warn("No hay autenticación disponible para esta solicitud")
                throw ForbiddenOperationException(
                    "No se pudo identificar al usuario autenticado. Verifica que el token JWT contenga el claim 'username'."
                )
            }
            return authentication.name ?: throw ForbiddenOperationException(
                "No se pudo identificar al usuario autenticado. Verifica que el token JWT contenga el claim 'username'."
            )
        }
    }

    /** Público — solo spots APPROVED, con filtros opcionales de categoría y rareza. */
    @GetMapping
    fun listApproved(
        @RequestParam(required = false) categoryId: Long?,
        @RequestParam(required = false) rarity: Rarity?,
        @PageableDefault(size = 20, sort = ["createdAt"], direction = Sort.Direction.DESC) pageable: Pageable
    ): Page<SpotResponse> = spotService.listApproved(categoryId, rarity, pageable)

    /** USER/ADMIN — spots propios, en cualquier estado. */
    @GetMapping("/me")
    fun listMine(
        authentication: Authentication?,
        @PageableDefault(size = 20, sort = ["createdAt"], direction = Sort.Direction.DESC) pageable: Pageable
    ): Page<SpotResponse> = spotService.listMine(resolveUsername(authentication), pageable)

    /** Solo ADMIN (SecurityConfig) — spots PENDING para moderar (Fase 10.7). */
    @GetMapping("/pending")
    fun listPending(
        @PageableDefault(size = 20, sort = ["createdAt"], direction = Sort.Direction.DESC) pageable: Pageable
    ): Page<SpotResponse> = spotService.listPending(pageable)

    /** Público — detalle de un spot. */
    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): SpotResponse = spotService.getById(id)

    /** USER/ADMIN — nace PENDING. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @Valid @RequestBody request: SpotCreateRequest,
        authentication: Authentication?
    ): SpotResponse {
        val username = resolveUsername(authentication)
        log.debug("Creando spot para usuario '{}'", username)
        return spotService.create(request, username)
    }

    /** USER/ADMIN + propiedad (validada en SpotService). */
    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: SpotUpdateRequest,
        authentication: Authentication?
    ): SpotResponse = spotService.update(id, request, resolveUsername(authentication))

    /** USER/ADMIN + propiedad (validada en SpotService). */
    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long, authentication: Authentication?): ResponseEntity<Void> {
        spotService.delete(id, resolveUsername(authentication))
        return ResponseEntity.noContent().build()
    }

    /** Solo ADMIN (SecurityConfig). Solo tiene sentido sobre un spot PENDING. */
    @PatchMapping("/{id}/approve")
    fun approve(@PathVariable id: Long, @Valid @RequestBody request: SpotApproveRequest): SpotResponse =
        spotService.approve(id, request)

    /** Solo ADMIN (SecurityConfig). Solo tiene sentido sobre un spot PENDING. */
    @PatchMapping("/{id}/reject")
    fun reject(@PathVariable id: Long, @Valid @RequestBody request: SpotRejectRequest): SpotResponse =
        spotService.reject(id, request)
}
