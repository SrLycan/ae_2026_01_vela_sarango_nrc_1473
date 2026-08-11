package com.spotsapp.services

import com.spotsapp.dto.stats.StatsResponse
import com.spotsapp.entities.enums.ProfileImageType
import com.spotsapp.repositories.ProfileImageRepository
import com.spotsapp.repositories.ReviewRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Gamificación (RF-08). Puntos, nivel y medalla se calculan en el momento a partir de
 * SUM(pointsReward) de los spots reseñados por el usuario — sin tabla propia (ADR-003).
 */
@Service
@Transactional(readOnly = true)
class StatsService(
    private val reviewRepository: ReviewRepository,
    private val profileImageRepository: ProfileImageRepository
) {

    /** Umbrales de puntos -> (nivel, medalla). Lógica de aplicación, no datos en BD (ADR-003). */
    private val levelThresholds: List<Pair<Int, String>> = listOf(
        50 to "Explorador Novato",
        150 to "Explorador",
        300 to "Aventurero",
        600 to "Veterano"
    )
    private val maxLevelBadge = "Leyenda"

    fun getStats(username: String): StatsResponse {
        val totalPoints = reviewRepository.sumPointsRewardByUsername(username)
        val spotsReviewed = reviewRepository.findByUsername(username).size
        val (level, badge) = resolveLevel(totalPoints)

        val avatarUrl = if (profileImageRepository.existsByUsernameAndImageType(username, ProfileImageType.AVATAR))
            "/profile/$username/avatar" else null
        val bannerUrl = if (profileImageRepository.existsByUsernameAndImageType(username, ProfileImageType.BANNER))
            "/profile/$username/banner" else null

        return StatsResponse(
            username = username,
            totalPoints = totalPoints,
            spotsReviewed = spotsReviewed,
            level = level,
            badge = badge,
            avatarUrl = avatarUrl,
            bannerUrl = bannerUrl
        )
    }

    private fun resolveLevel(totalPoints: Int): Pair<Int, String> {
        levelThresholds.forEachIndexed { index, (threshold, badge) ->
            if (totalPoints < threshold) {
                return (index + 1) to badge
            }
        }
        return (levelThresholds.size + 1) to maxLevelBadge
    }
}
