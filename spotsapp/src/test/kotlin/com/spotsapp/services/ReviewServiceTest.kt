package com.spotsapp.services

import com.spotsapp.dto.review.ReviewCreateRequest
import com.spotsapp.dto.review.ReviewUpdateRequest
import com.spotsapp.entities.Category
import com.spotsapp.entities.Review
import com.spotsapp.entities.Spot
import com.spotsapp.entities.enums.SpotStatus
import com.spotsapp.exceptions.BusinessRuleException
import com.spotsapp.exceptions.DuplicateReviewException
import com.spotsapp.exceptions.ForbiddenOperationException
import com.spotsapp.exceptions.ResourceNotFoundException
import com.spotsapp.mappers.ReviewMapper
import com.spotsapp.repositories.ReviewRepository
import com.spotsapp.repositories.SpotRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertEquals
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
    fun `create guarda la resena cuando el spot esta APPROVED y no hay duplicado`() {
        every { spotRepository.findById(10L) } returns Optional.of(spot(SpotStatus.APPROVED))
        every { reviewRepository.existsBySpotIdAndUsername(10L, "ricardo") } returns false
        val reviewSlot = slot<Review>()
        every { reviewRepository.save(capture(reviewSlot)) } answers {
            Review(
                id = 1L, spot = reviewSlot.captured.spot, username = reviewSlot.captured.username,
                rating = reviewSlot.captured.rating, comment = reviewSlot.captured.comment
            )
        }

        val request = ReviewCreateRequest(spotId = 10L, rating = 5, comment = "Excelente")
        val response = service.create(10L, request, "ricardo")

        assertEquals(1L, response.id)
        assertEquals(10L, response.spotId)
        assertEquals("ricardo", response.username)
        assertEquals(5, response.rating)
        assertEquals("Excelente", response.comment)
    }

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
    fun `create lanza ResourceNotFoundException si el spot no existe`() {
        every { spotRepository.findById(99L) } returns Optional.empty()

        val request = ReviewCreateRequest(spotId = 99L, rating = 5, comment = null)

        assertFailsWith<ResourceNotFoundException> { service.create(99L, request, "ricardo") }
    }

    @Test
    fun `update modifica la resena cuando el usuario es el autor`() {
        val review = Review(id = 1L, spot = spot(SpotStatus.APPROVED), username = "ricardo", rating = 3, comment = "ok")
        every { reviewRepository.findById(1L) } returns Optional.of(review)
        every { reviewRepository.save(any()) } answers { firstArg() }

        val response = service.update(1L, ReviewUpdateRequest(rating = 5, comment = "mejor"), "ricardo")

        assertEquals(5, response.rating)
        assertEquals("mejor", response.comment)
    }

    @Test
    fun `update lanza ForbiddenOperationException si no es el autor`() {
        val review = Review(id = 1L, spot = spot(SpotStatus.APPROVED), username = "ricardo", rating = 3)
        every { reviewRepository.findById(1L) } returns Optional.of(review)

        assertFailsWith<ForbiddenOperationException> {
            service.update(1L, ReviewUpdateRequest(rating = 4, comment = null), "otro-usuario")
        }
    }

    @Test
    fun `update lanza ResourceNotFoundException si la resena no existe`() {
        every { reviewRepository.findById(99L) } returns Optional.empty()

        assertFailsWith<ResourceNotFoundException> {
            service.update(99L, ReviewUpdateRequest(rating = 4, comment = null), "ricardo")
        }
    }

    @Test
    fun `delete elimina la resena cuando el usuario es el autor`() {
        val review = Review(id = 1L, spot = spot(SpotStatus.APPROVED), username = "ricardo", rating = 3)
        every { reviewRepository.findById(1L) } returns Optional.of(review)
        every { reviewRepository.delete(any()) } returns Unit

        service.delete(1L, "ricardo", isAdmin = false)

        verify(exactly = 1) { reviewRepository.delete(review) }
    }

    @Test
    fun `delete permite a un admin borrar resenas ajenas`() {
        val review = Review(id = 1L, spot = spot(SpotStatus.APPROVED), username = "ricardo", rating = 3)
        every { reviewRepository.findById(1L) } returns Optional.of(review)
        every { reviewRepository.delete(any()) } returns Unit

        service.delete(1L, "admin", isAdmin = true)

        verify(exactly = 1) { reviewRepository.delete(review) }
    }

    @Test
    fun `delete lanza ForbiddenOperationException si la resena no es del usuario`() {
        val review = Review(id = 1L, spot = spot(SpotStatus.APPROVED), username = "ricardo", rating = 5)
        every { reviewRepository.findById(1L) } returns Optional.of(review)

        assertFailsWith<ForbiddenOperationException> { service.delete(1L, "otro-usuario", isAdmin = false) }
    }

    @Test
    fun `listBySpot retorna las resenas del spot`() {
        val review = Review(id = 1L, spot = spot(SpotStatus.APPROVED), username = "ricardo", rating = 5, comment = "genial")
        every { reviewRepository.findBySpotId(10L) } returns listOf(review)

        val responses = service.listBySpot(10L)

        assertEquals(1, responses.size)
        assertEquals(5, responses.first().rating)
    }
}
