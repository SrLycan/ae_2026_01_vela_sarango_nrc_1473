package com.spotsapp.services

import com.spotsapp.dto.spot.SpotApproveRequest
import com.spotsapp.dto.spot.SpotCreateRequest
import com.spotsapp.dto.spot.SpotRejectRequest
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
import io.mockk.verify
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
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
    private val pageable = PageRequest.of(0, 10)
    private val activeStatuses = listOf(SpotStatus.PENDING, SpotStatus.APPROVED)

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
    fun `create guarda un spot PENDING cuando no hay duplicado cercano`() {
        every { spotRepository.findByOwnerUsernameAndStatusIn("ricardo", activeStatuses) } returns emptyList()
        every { categoryRepository.findById(1L) } returns Optional.of(category)
        every { spotRepository.save(any()) } answers {
            val s = firstArg<Spot>()
            Spot(
                id = 7L, name = s.name, description = s.description, latitude = s.latitude,
                longitude = s.longitude, address = s.address, category = s.category,
                ownerUsername = s.ownerUsername, status = s.status
            )
        }

        val request = SpotCreateRequest(
            name = "Mirador", description = "desc", latitude = -0.18, longitude = -78.47,
            address = "dir", categoryId = 1L
        )
        val response = service.create(request, "ricardo")

        assertEquals(7L, response.id)
        assertEquals("Mirador", response.name)
        assertEquals(SpotStatus.PENDING, response.status)
        assertEquals("ricardo", response.ownerUsername)
    }

    @Test
    fun `create lanza ForbiddenOperationException si el owner esta en blanco`() {
        val request = SpotCreateRequest(
            name = "Mirador", description = "desc", latitude = 0.0, longitude = 0.0,
            address = "dir", categoryId = 1L
        )

        assertFailsWith<ForbiddenOperationException> { service.create(request, "   ") }
    }

    @Test
    fun `create lanza BusinessRuleException si ya hay un spot activo cercano`() {
        val existing = Spot(
            id = 1L, name = "Otro", description = "d", latitude = -0.18, longitude = -78.47,
            address = "a", category = category, ownerUsername = "ricardo", status = SpotStatus.APPROVED
        )
        every { spotRepository.findByOwnerUsernameAndStatusIn("ricardo", activeStatuses) } returns listOf(existing)

        val request = SpotCreateRequest(
            name = "Mirador", description = "desc", latitude = -0.18, longitude = -78.47,
            address = "dir", categoryId = 1L
        )

        assertFailsWith<BusinessRuleException> { service.create(request, "ricardo") }
    }

    @Test
    fun `create no considera duplicado un spot activo lejano`() {
        val far = Spot(
            id = 1L, name = "Lejos", description = "d", latitude = 40.0, longitude = -78.0,
            address = "a", category = category, ownerUsername = "ricardo", status = SpotStatus.APPROVED
        )
        every { spotRepository.findByOwnerUsernameAndStatusIn("ricardo", activeStatuses) } returns listOf(far)
        every { categoryRepository.findById(1L) } returns Optional.of(category)
        every { spotRepository.save(any()) } answers {
            val s = firstArg<Spot>()
            Spot(
                id = 7L, name = s.name, description = s.description, latitude = s.latitude,
                longitude = s.longitude, address = s.address, category = s.category,
                ownerUsername = s.ownerUsername, status = s.status
            )
        }

        val request = SpotCreateRequest(
            name = "Mirador", description = "desc", latitude = -0.18, longitude = -78.47,
            address = "dir", categoryId = 1L
        )
        val response = service.create(request, "ricardo")

        assertEquals("Mirador", response.name)
    }

    @Test
    fun `create lanza ResourceNotFoundException si la categoria no existe`() {
        every { categoryRepository.findById(99L) } returns Optional.empty()
        every { spotRepository.findByOwnerUsernameAndStatusIn("ricardo", activeStatuses) } returns emptyList()

        val request = SpotCreateRequest(
            name = "Spot", description = "desc", latitude = 0.0, longitude = 0.0,
            address = "dir", categoryId = 99L
        )

        assertFailsWith<ResourceNotFoundException> { service.create(request, "ricardo") }
    }

    @Test
    fun `update actualiza el spot cuando el usuario es el dueno`() {
        val spot = pendingSpot()
        every { spotRepository.findById(10L) } returns Optional.of(spot)
        every { spotRepository.findByOwnerUsernameAndStatusIn("ricardo", activeStatuses) } returns emptyList()
        every { categoryRepository.findById(1L) } returns Optional.of(category)
        every { spotRepository.save(any()) } answers { firstArg() }

        val request = SpotUpdateRequest(
            name = "Nuevo nombre", description = "nueva desc", latitude = -0.18, longitude = -78.47,
            address = "Nueva dir", categoryId = 1L
        )
        val response = service.update(10L, request, "ricardo")

        assertEquals("Nuevo nombre", response.name)
        assertEquals("Nueva dir", response.address)
    }

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
    fun `update lanza BusinessRuleException si hay otro spot activo cercano`() {
        val spot = pendingSpot()
        every { spotRepository.findById(10L) } returns Optional.of(spot)
        val other = Spot(
            id = 20L, name = "Otro", description = "d", latitude = -0.18, longitude = -78.47,
            address = "a", category = category, ownerUsername = "ricardo", status = SpotStatus.PENDING
        )
        every { spotRepository.findByOwnerUsernameAndStatusIn("ricardo", activeStatuses) } returns listOf(spot, other)

        val request = SpotUpdateRequest(
            name = "Nuevo nombre", description = "desc", latitude = -0.18, longitude = -78.47,
            address = "dir", categoryId = 1L
        )

        assertFailsWith<BusinessRuleException> { service.update(10L, request, "ricardo") }
    }

    @Test
    fun `update lanza ResourceNotFoundException si el spot no existe`() {
        every { spotRepository.findById(99L) } returns Optional.empty()

        val request = SpotUpdateRequest(
            name = "Nuevo", description = "desc", latitude = 0.0, longitude = 0.0,
            address = "dir", categoryId = 1L
        )

        assertFailsWith<ResourceNotFoundException> { service.update(99L, request, "ricardo") }
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
    fun `reject cambia el spot a REJECTED con el motivo`() {
        val spot = pendingSpot()
        every { spotRepository.findById(10L) } returns Optional.of(spot)
        every { spotRepository.save(any()) } answers { firstArg() }

        val response = service.reject(10L, SpotRejectRequest(rejectionReason = "Foto poco clara"))

        assertEquals(SpotStatus.REJECTED, response.status)
        assertEquals("Foto poco clara", response.rejectionReason)
    }

    @Test
    fun `reject lanza BusinessRuleException si el spot ya no esta PENDING`() {
        val spot = pendingSpot().apply { status = SpotStatus.APPROVED }
        every { spotRepository.findById(10L) } returns Optional.of(spot)

        assertFailsWith<BusinessRuleException> {
            service.reject(10L, SpotRejectRequest(rejectionReason = "Motivo"))
        }
    }

    @Test
    fun `delete elimina el spot cuando el usuario es el dueno`() {
        val spot = pendingSpot()
        every { spotRepository.findById(10L) } returns Optional.of(spot)
        every { spotRepository.delete(any()) } returns Unit

        service.delete(10L, "ricardo", isAdmin = false)

        verify(exactly = 1) { spotRepository.delete(spot) }
    }

    @Test
    fun `delete lanza ForbiddenOperationException si no es el dueno ni admin`() {
        val spot = pendingSpot()
        every { spotRepository.findById(10L) } returns Optional.of(spot)

        assertFailsWith<ForbiddenOperationException> { service.delete(10L, "otro-usuario", isAdmin = false) }
    }

    @Test
    fun `delete permite a un admin borrar spots ajenos`() {
        val spot = pendingSpot()
        every { spotRepository.findById(10L) } returns Optional.of(spot)
        every { spotRepository.delete(any()) } returns Unit

        service.delete(10L, "admin", isAdmin = true)

        verify(exactly = 1) { spotRepository.delete(spot) }
    }

    @Test
    fun `getById retorna el spot`() {
        every { spotRepository.findById(10L) } returns Optional.of(pendingSpot())

        val response = service.getById(10L)

        assertEquals("Mirador X", response.name)
        assertEquals(SpotStatus.PENDING, response.status)
    }

    @Test
    fun `listApproved filtra por categoria y rareza`() {
        val page = PageImpl(listOf(pendingSpot().apply { status = SpotStatus.APPROVED }))
        every { spotRepository.findByStatusAndCategoryIdAndRarity(SpotStatus.APPROVED, 1L, Rarity.EPIC, pageable) } returns page

        val result = service.listApproved(1L, Rarity.EPIC, pageable)

        assertEquals(1, result.totalElements)
    }

    @Test
    fun `listApproved filtra solo por categoria`() {
        val page = PageImpl(listOf(pendingSpot().apply { status = SpotStatus.APPROVED }))
        every { spotRepository.findByStatusAndCategoryId(SpotStatus.APPROVED, 1L, pageable) } returns page

        val result = service.listApproved(1L, null, pageable)

        assertEquals(1, result.totalElements)
    }

    @Test
    fun `listApproved filtra solo por rareza`() {
        val page = PageImpl(listOf(pendingSpot().apply { status = SpotStatus.APPROVED }))
        every { spotRepository.findByStatusAndRarity(SpotStatus.APPROVED, Rarity.COMMON, pageable) } returns page

        val result = service.listApproved(null, Rarity.COMMON, pageable)

        assertEquals(1, result.totalElements)
    }

    @Test
    fun `listApproved sin filtros retorna todos los aprobados`() {
        val page = PageImpl(listOf(pendingSpot().apply { status = SpotStatus.APPROVED }))
        every { spotRepository.findByStatus(SpotStatus.APPROVED, pageable) } returns page

        val result = service.listApproved(null, null, pageable)

        assertEquals(1, result.totalElements)
    }

    @Test
    fun `listMine retorna los spots del usuario`() {
        val page = PageImpl(listOf(pendingSpot()))
        every { spotRepository.findByOwnerUsername("ricardo", pageable) } returns page

        val result = service.listMine("ricardo", pageable)

        assertEquals(1, result.totalElements)
        assertEquals("ricardo", result.content.first().ownerUsername)
    }

    @Test
    fun `listPending retorna los spots pendientes`() {
        val page = PageImpl(listOf(pendingSpot()))
        every { spotRepository.findByStatus(SpotStatus.PENDING, pageable) } returns page

        val result = service.listPending(pageable)

        assertEquals(1, result.totalElements)
        assertEquals(SpotStatus.PENDING, result.content.first().status)
    }
}
