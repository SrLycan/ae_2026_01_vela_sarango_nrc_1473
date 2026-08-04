package com.spotsapp.services

import com.spotsapp.dto.review.ReviewCreateRequest
import com.spotsapp.dto.review.ReviewResponse
import com.spotsapp.dto.review.ReviewUpdateRequest
import com.spotsapp.entities.enums.SpotStatus
import com.spotsapp.exceptions.BusinessRuleException
import com.spotsapp.exceptions.DuplicateReviewException
import com.spotsapp.exceptions.ForbiddenOperationException
import com.spotsapp.exceptions.ResourceNotFoundException
import com.spotsapp.mappers.ReviewMapper
import com.spotsapp.repositories.ReviewRepository
import com.spotsapp.repositories.SpotRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Reseñas de spots (RF-06). Solo se puede reseñar un spot ya APPROVED, una única vez por
 * usuario y spot (constraint único reforzado en BD — uk_reviews_spot_username).
 */
@Service
@Transactional
class ReviewService(
    private val reviewRepository: ReviewRepository,
    private val spotRepository: SpotRepository,
    private val reviewMapper: ReviewMapper
) {

    fun create(spotId: Long, request: ReviewCreateRequest, username: String): ReviewResponse {
        val spot = spotRepository.findById(spotId).orElseThrow { ResourceNotFoundException.of("Spot", spotId) }

        if (spot.status != SpotStatus.APPROVED) {
            throw BusinessRuleException("Solo se pueden reseñar spots aprobados")
        }
        if (reviewRepository.existsBySpotIdAndUsername(spotId, username)) {
            throw DuplicateReviewException(spotId, username)
        }

        val review = reviewMapper.toEntity(request, spot, username)
        return reviewMapper.toResponse(reviewRepository.save(review))
    }

    fun update(reviewId: Long, request: ReviewUpdateRequest, username: String): ReviewResponse {
        val review = findEntityOrThrow(reviewId)
        requireOwner(review.username, username)

        reviewMapper.applyUpdate(review, request)
        return reviewMapper.toResponse(reviewRepository.save(review))
    }

    fun delete(reviewId: Long, username: String) {
        val review = findEntityOrThrow(reviewId)
        requireOwner(review.username, username)
        reviewRepository.delete(review)
    }

    @Transactional(readOnly = true)
    fun listBySpot(spotId: Long): List<ReviewResponse> =
        reviewRepository.findBySpotId(spotId).map(reviewMapper::toResponse)

    private fun requireOwner(reviewUsername: String, currentUsername: String) {
        if (reviewUsername != currentUsername) {
            throw ForbiddenOperationException("No puedes modificar una reseña que no te pertenece")
        }
    }

    private fun findEntityOrThrow(reviewId: Long) =
        reviewRepository.findById(reviewId).orElseThrow { ResourceNotFoundException.of("Review", reviewId) }
}
