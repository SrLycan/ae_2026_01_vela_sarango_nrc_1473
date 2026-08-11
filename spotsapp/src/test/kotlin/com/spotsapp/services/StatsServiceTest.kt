package com.spotsapp.services

import com.spotsapp.entities.Category
import com.spotsapp.entities.Review
import com.spotsapp.entities.Spot
import com.spotsapp.entities.enums.ProfileImageType
import com.spotsapp.repositories.ProfileImageRepository
import com.spotsapp.repositories.ReviewRepository
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals

class StatsServiceTest {

    private val reviewRepository = mockk<ReviewRepository>()
    private val profileImageRepository = mockk<ProfileImageRepository>()
    private val service = StatsService(reviewRepository, profileImageRepository)

    init {
        every { profileImageRepository.existsByUsernameAndImageType(any(), ProfileImageType.AVATAR) } returns false
        every { profileImageRepository.existsByUsernameAndImageType(any(), ProfileImageType.BANNER) } returns false
    }

    private fun review() = Review(
        id = 1L,
        spot = Spot(
            id = 1L, name = "Spot", description = "desc", latitude = 0.0, longitude = 0.0,
            address = "dir", category = Category(id = 1L, name = "Cat"), ownerUsername = "otro"
        ),
        username = "ricardo",
        rating = 5
    )

    @Test
    fun `getStats retorna nivel 1 cuando el usuario no tiene puntos`() {
        every { reviewRepository.sumPointsRewardByUsername("ricardo") } returns 0
        every { reviewRepository.findByUsername("ricardo") } returns emptyList()

        val stats = service.getStats("ricardo")

        assertEquals(0, stats.totalPoints)
        assertEquals(1, stats.level)
        assertEquals("Explorador Novato", stats.badge)
    }

    @Test
    fun `getStats retorna nivel Leyenda cuando supera el ultimo umbral`() {
        every { reviewRepository.sumPointsRewardByUsername("ricardo") } returns 750
        every { reviewRepository.findByUsername("ricardo") } returns listOf(review())

        val stats = service.getStats("ricardo")

        assertEquals(750, stats.totalPoints)
        assertEquals(5, stats.level)
        assertEquals("Leyenda", stats.badge)
        assertEquals(1, stats.spotsReviewed)
    }

    @Test
    fun `getStats retorna nivel intermedio segun el umbral correspondiente`() {
        every { reviewRepository.sumPointsRewardByUsername("ricardo") } returns 200
        every { reviewRepository.findByUsername("ricardo") } returns listOf(review())

        val stats = service.getStats("ricardo")

        assertEquals(3, stats.level)
        assertEquals("Aventurero", stats.badge)
    }
}
