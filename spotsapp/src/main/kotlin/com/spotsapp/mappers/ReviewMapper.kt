package com.spotsapp.mappers

import com.spotsapp.dto.review.ReviewCreateRequest
import com.spotsapp.dto.review.ReviewResponse
import com.spotsapp.dto.review.ReviewUpdateRequest
import com.spotsapp.entities.Review
import com.spotsapp.entities.Spot
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class ReviewMapper {

    fun toEntity(request: ReviewCreateRequest, spot: Spot, username: String): Review =
        Review(
            spot = spot,
            username = username,
            rating = request.rating,
            comment = request.comment
        )

    fun applyUpdate(review: Review, request: ReviewUpdateRequest) {
        review.rating = request.rating
        review.comment = request.comment
        review.updatedAt = Instant.now()
    }

    fun toResponse(review: Review): ReviewResponse =
        ReviewResponse(
            id = requireNotNull(review.id),
            spotId = requireNotNull(review.spot.id),
            username = review.username,
            rating = review.rating,
            comment = review.comment,
            createdAt = review.createdAt,
            updatedAt = review.updatedAt
        )
}
