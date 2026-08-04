package com.spotsapp.services

import com.spotsapp.dto.spot.SpotApproveRequest
import com.spotsapp.dto.spot.SpotUpdateRequest
import com.spotsapp.entities.Category
import com.spotsapp.entities.Spot
import com.spotsapp.entities.enums.Rarity
import com.spotsapp.entities.enums.SpotStatus
import com.spotsapp.exceptions.BusinessRuleException
import com.spotsapp.exceptions.ForbiddenOperationException
import com.spotsapp.exceptions.ResourceNotFoundException
import com.spotsapp.mappers.SpotMapper
import com.spotsapp.repositories.CategoryRepository
import com.spotsapp.repositories.SpotRepository
import io.mockk.every
import io.mockk.mockk
import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SpotServiceTest {

    private val spotRepository = mockk<SpotRepository>()
    private val categoryRepository = mockk<CategoryRepository>()
    private val spotMapper = SpotMapper()
    private val service = SpotService(spotRepository, categoryRepository, spotMapper)

    private val category = Category(id = 1L, name = "Miradores")

    private fun pendingSpot(ownerUsername: String = "ricardo") = Spot(
        id = 10L,
        name = "Mirador X",
        description = "Vista increíble",
        latitude = -0.18,
        longitude = -78.47,
        address = "Calle 1",
        category = category,
        ownerUsername = ownerUsername,
        status = SpotStatus.PENDING
    )

    @Test
    fun `update lanza ForbiddenOperationException si el usuario no es el propietario`() {
        every { spotRepository.findById(10L) } returns Optional.of(pendingSpot(ownerUsername = "ricardo"))

        val request = SpotUpdateRequest(
            name = "Nuevo nombre", description = "desc", latitude = 0.0, longitude = 0.0,
            address = "dir", categoryId = 1L
        )

        assertFailsWith<ForbiddenOperationException> { service.update(10L, request, "otro-usuario") }
    }

    @Test
    fun `approve cambia el spot a APPROVED y asigna rarity y pointsReward`() {
        val spot = pendingSpot()
        every { spotRepository.findById(10L) } returns Optional.of(spot)
        every { spotRepository.save(any()) } answers { firstArg() }

        val response = service.approve(10L, SpotApproveRequest(rarity = Rarity.EPIC, pointsReward = 150))

        assertEquals(SpotStatus.APPROVED, response.status)
        assertEquals(Rarity.EPIC, response.rarity)
        assertEquals(150, response.pointsReward)
    }

    @Test
    fun `approve lanza BusinessRuleException si el spot ya no esta PENDING`() {
        val spot = pendingSpot().apply { status = SpotStatus.APPROVED }
        every { spotRepository.findById(10L) } returns Optional.of(spot)

        assertFailsWith<BusinessRuleException> {
            service.approve(10L, SpotApproveRequest(rarity = Rarity.RARE, pointsReward = 50))
        }
    }

    @Test
    fun `create lanza ResourceNotFoundException si la categoria no existe`() {
        every { categoryRepository.findById(99L) } returns Optional.empty()

        val request = com.spotsapp.dto.spot.SpotCreateRequest(
            name = "Spot", description = "desc", latitude = 0.0, longitude = 0.0,
            address = "dir", categoryId = 99L
        )

        assertFailsWith<ResourceNotFoundException> { service.create(request, "ricardo") }
    }
}
