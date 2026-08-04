package com.spotsapp.services

import com.spotsapp.dto.follow.FollowResponse
import com.spotsapp.entities.Follow
import com.spotsapp.exceptions.BusinessRuleException
import com.spotsapp.exceptions.ResourceNotFoundException
import com.spotsapp.mappers.FollowMapper
import com.spotsapp.repositories.FollowRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Seguimiento social entre usuarios (RF-07). "No seguirse a sí mismo" y "sin duplicados" se
 * validan acá y además están reforzados por constraints de BD (chk_follows_no_self_follow,
 * uk_follows_follower_following) como red de seguridad.
 */
@Service
@Transactional
class FollowService(
    private val followRepository: FollowRepository,
    private val followMapper: FollowMapper
) {

    fun follow(followerUsername: String, followingUsername: String) {
        if (followerUsername == followingUsername) {
            throw BusinessRuleException("Un usuario no puede seguirse a sí mismo")
        }
        if (followRepository.existsByFollowerUsernameAndFollowingUsername(followerUsername, followingUsername)) {
            throw BusinessRuleException("Ya sigues a '$followingUsername'")
        }
        followRepository.save(
            Follow(followerUsername = followerUsername, followingUsername = followingUsername)
        )
    }

    fun unfollow(followerUsername: String, followingUsername: String) {
        followRepository.findByFollowerUsernameAndFollowingUsername(followerUsername, followingUsername)
            ?: throw ResourceNotFoundException("No sigues a '$followingUsername'")
        followRepository.deleteByFollowerUsernameAndFollowingUsername(followerUsername, followingUsername)
    }

    @Transactional(readOnly = true)
    fun getFollowing(username: String): List<FollowResponse> =
        followRepository.findByFollowerUsername(username).map(followMapper::toFollowingResponse)

    @Transactional(readOnly = true)
    fun getFollowers(username: String): List<FollowResponse> =
        followRepository.findByFollowingUsername(username).map(followMapper::toFollowerResponse)
}
