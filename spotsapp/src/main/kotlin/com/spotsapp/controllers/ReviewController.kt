package com.spotsapp.controllers

import com.spotsapp.dto.review.ReviewCreateRequest
import com.spotsapp.dto.review.ReviewResponse
import com.spotsapp.dto.review.ReviewUpdateRequest
import com.spotsapp.services.ReviewService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * RF-06. El spotId viaja en el body de ReviewCreateRequest (matriz define POST /reviews,
 * no POST /spots/{id}/reviews).
 */
@RestController
class ReviewController(
    private val reviewService: ReviewService
) {

    /** Público. */
    @GetMapping("/spots/{spotId}/reviews")
    fun listBySpot(@PathVariable spotId: Long): List<ReviewResponse> = reviewService.listBySpot(spotId)

    /** USER/ADMIN — el spot debe estar APPROVED, una reseña por usuario y spot. */
    @PostMapping("/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @Valid @RequestBody request: ReviewCreateRequest,
        authentication: Authentication
    ): ReviewResponse = reviewService.create(request.spotId, request, authentication.name)

    /** USER/ADMIN + propiedad. */
    @PutMapping("/reviews/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: ReviewUpdateRequest,
        authentication: Authentication
    ): ReviewResponse = reviewService.update(id, request, authentication.name)

    /** USER/ADMIN + propiedad. */
    @DeleteMapping("/reviews/{id}")
    fun delete(@PathVariable id: Long, authentication: Authentication): ResponseEntity<Void> {
        reviewService.delete(id, authentication.name)
        return ResponseEntity.noContent().build()
    }
}
