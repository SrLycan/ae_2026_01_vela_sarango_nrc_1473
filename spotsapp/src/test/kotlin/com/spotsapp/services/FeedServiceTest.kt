package com.spotsapp.services

import com.spotsapp.entities.Category
import com.spotsapp.entities.Follow
import com.spotsapp.entities.Spot
import com.spotsapp.entities.enums.SpotStatus
import com.spotsapp.mappers.SpotMapper
import com.spotsapp.repositories.FollowRepository
import com.spotsapp.repositories.SpotRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FeedServiceTest {

    private val followRepository = mockk<FollowRepository>()
    private val spotRepository = mockk<SpotRepository>()
    private val spotMapper = SpotMapper()
    private val service = FeedService(followRepository, spotRepository, spotMapper)

    private val pageable = PageRequest.of(0, 10)

    @Test
    fun `getFeed retorna pagina vacia si el usuario no sigue a nadie`() {
        every { followRepository.findByFollowerUsername("ricardo") } returns emptyList()

        val result = service.getFeed("ricardo", pageable)

        assertTrue(result.isEmpty)
        verify(exactly = 0) { spotRepository.findByOwnerUsernameInAndStatusOrderByCreatedAtDesc(any(), any(), any()) }
    }

    @Test
    fun `getFeed retorna spots de las cuentas seguidas`() {
        val category = Category(id = 1L, name = "Miradores")
        val spot = Spot(
            id = 5L, name = "Mirador Y", description = "desc", latitude = 0.0, longitude = 0.0,
            address = "dir", category = category, ownerUsername = "ana", status = SpotStatus.APPROVED
        )

        every { followRepository.findByFollowerUsername("ricardo") } returns
            listOf(Follow(id = 1L, followerUsername = "ricardo", followingUsername = "ana"))
        every {
            spotRepository.findByOwnerUsernameInAndStatusOrderByCreatedAtDesc(listOf("ana"), SpotStatus.APPROVED, pageable)
        } returns PageImpl(listOf(spot))

        val result = service.getFeed("ricardo", pageable)

        assertEquals(1, result.totalElements)
        assertEquals("ana", result.content.first().ownerUsername)
    }
}
