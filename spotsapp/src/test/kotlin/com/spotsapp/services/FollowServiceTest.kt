package com.spotsapp.services

import com.spotsapp.entities.Follow
import com.spotsapp.exceptions.BusinessRuleException
import com.spotsapp.exceptions.ResourceNotFoundException
import com.spotsapp.mappers.FollowMapper
import com.spotsapp.repositories.FollowRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FollowServiceTest {

    private val followRepository = mockk<FollowRepository>()
    private val followMapper = FollowMapper()
    private val service = FollowService(followRepository, followMapper)

    @Test
    fun `follow lanza BusinessRuleException si el usuario intenta seguirse a si mismo`() {
        assertFailsWith<BusinessRuleException> { service.follow("ricardo", "ricardo") }
    }

    @Test
    fun `follow lanza BusinessRuleException si ya sigue al usuario`() {
        every { followRepository.existsByFollowerUsernameAndFollowingUsername("ricardo", "ana") } returns true

        assertFailsWith<BusinessRuleException> { service.follow("ricardo", "ana") }
        verify(exactly = 0) { followRepository.save(any()) }
    }

    @Test
    fun `follow guarda el follow cuando es valido`() {
        every { followRepository.existsByFollowerUsernameAndFollowingUsername("ricardo", "ana") } returns false
        every { followRepository.save(any()) } returns Follow(id = 1L, followerUsername = "ricardo", followingUsername = "ana")

        service.follow("ricardo", "ana")

        verify(exactly = 1) { followRepository.save(any()) }
    }

    @Test
    fun `unfollow lanza ResourceNotFoundException si no existia el follow`() {
        every { followRepository.findByFollowerUsernameAndFollowingUsername("ricardo", "ana") } returns null

        assertFailsWith<ResourceNotFoundException> { service.unfollow("ricardo", "ana") }
    }

    @Test
    fun `unfollow elimina el follow cuando existe`() {
        val follow = Follow(id = 1L, followerUsername = "ricardo", followingUsername = "ana")
        every { followRepository.findByFollowerUsernameAndFollowingUsername("ricardo", "ana") } returns follow
        every { followRepository.deleteByFollowerUsernameAndFollowingUsername("ricardo", "ana") } returns Unit

        service.unfollow("ricardo", "ana")

        verify(exactly = 1) { followRepository.deleteByFollowerUsernameAndFollowingUsername("ricardo", "ana") }
    }

    @Test
    fun `getFollowing retorna los usuarios seguidos`() {
        val follow = Follow(id = 1L, followerUsername = "ricardo", followingUsername = "ana")
        every { followRepository.findByFollowerUsername("ricardo") } returns listOf(follow)

        val responses = service.getFollowing("ricardo")

        assertEquals(1, responses.size)
        assertEquals("ana", responses.first().username)
    }

    @Test
    fun `getFollowers retorna los seguidores`() {
        val follow = Follow(id = 1L, followerUsername = "luis", followingUsername = "ricardo")
        every { followRepository.findByFollowingUsername("ricardo") } returns listOf(follow)

        val responses = service.getFollowers("ricardo")

        assertEquals(1, responses.size)
        assertEquals("luis", responses.first().username)
    }
}
