package com.spotsapp.services

import com.spotsapp.dto.review.ReviewCreateRequest
import com.spotsapp.entities.Category
import com.spotsapp.entities.Review
import com.spotsapp.entities.Spot
import com.spotsapp.entities.enums.SpotStatus
import com.spotsapp.exceptions.BusinessRuleException
import com.spotsapp.exceptions.DuplicateReviewException
import com.spotsapp.exceptions.ForbiddenOperationException
import com.spotsapp.mappers.ReviewMapper
import com.spotsapp.repositories.ReviewRepository
import com.spotsapp.repositories.SpotRepository
import io.mockk.every
import io.mockk.mockk
import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertFailsWith

class ReviewServiceTest {

    private val reviewRepository = mockk<ReviewRepository>()
    private val spotRepository = mockk<SpotRepository>()
    private val reviewMapper = ReviewMapper()
    private val service = ReviewService(reviewRepository, spotRepository, reviewMapper)

    private val category = Category(id = 1L, name = "Miradores")

    private fun spot(status: SpotStatus) = Spot(
        id = 10L, name = "Mirador X", description = "desc", latitude = 0.0, longitude = 0.0,
        address = "dir", category = category, ownerUsername = "duenio", status = status
    )

    @Test
    fun `create lanza BusinessRuleException si el spot no esta APPROVED`() {
        every { spotRepository.findById(10L) } returns Optional.of(spot(SpotStatus.PENDING))

        val request = ReviewCreateRequest(spotId = 10L, rating = 5, comment = "Excelente")

        assertFailsWith<BusinessRuleException> { service.create(10L, request, "ricardo") }
    }

    @Test
    fun `create lanza DuplicateReviewException si el usuario ya reseño el spot`() {
        every { spotRepository.findById(10L) } returns Optional.of(spot(SpotStatus.APPROVED))
        every { reviewRepository.existsBySpotIdAndUsername(10L, "ricardo") } returns true

        val request = ReviewCreateRequest(spotId = 10L, rating = 4, comment = null)

        assertFailsWith<DuplicateReviewException> { service.create(10L, request, "ricardo") }
    }

    @Test
    fun `delete lanza ForbiddenOperationException si la reseña no es del usuario`() {
        val review = Review(id = 1L, spot = spot(SpotStatus.APPROVED), username = "ricardo", rating = 5)
        every { reviewRepository.findById(1L) } returns Optional.of(review)

        assertFailsWith<ForbiddenOperationException> { service.delete(1L, "otro-usuario") }
    }
}
