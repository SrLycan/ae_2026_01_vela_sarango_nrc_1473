package com.spotsapp.services

import com.spotsapp.dto.spot.SpotResponse
import com.spotsapp.entities.enums.SpotStatus
import com.spotsapp.mappers.SpotMapper
import com.spotsapp.repositories.FollowRepository
import com.spotsapp.repositories.SpotRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Feed social (RF-08, GET /feed): spots APPROVED más recientes publicados por las cuentas
 * que sigue el usuario actual.
 */
@Service
@Transactional(readOnly = true)
class FeedService(
    private val followRepository: FollowRepository,
    private val spotRepository: SpotRepository,
    private val spotMapper: SpotMapper
) {

    fun getFeed(currentUsername: String, pageable: Pageable): Page<SpotResponse> {
        val followingUsernames = followRepository.findByFollowerUsername(currentUsername)
            .map { it.followingUsername }

        if (followingUsernames.isEmpty()) {
            return Page.empty(pageable)
        }

        return spotRepository
            .findByOwnerUsernameInAndStatusOrderByCreatedAtDesc(followingUsernames, SpotStatus.APPROVED, pageable)
            .map(spotMapper::toResponse)
    }
}
