package com.spotsapp.repositories

import com.spotsapp.entities.Review
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ReviewRepository : JpaRepository<Review, Long> {

    fun findBySpotId(spotId: Long): List<Review>

    fun findByUsername(username: String): List<Review>

    // Constraint único spot+usuario — validado también a nivel de BD (uk_reviews_spot_username)
    fun existsBySpotIdAndUsername(spotId: Long, username: String): Boolean

    fun findBySpotIdAndUsername(spotId: Long, username: String): Review?

    fun existsByIdAndUsername(id: Long, username: String): Boolean

    // StatsService.getTotalPoints() — SUM(pointsReward) de spots APPROVED reseñados por el usuario
    @Query(
        """
        SELECT COALESCE(SUM(r.spot.pointsReward), 0)
        FROM Review r
        WHERE r.username = :username
          AND r.spot.status = com.spotsapp.entities.enums.SpotStatus.APPROVED
        """
    )
    fun sumPointsRewardByUsername(@Param("username") username: String): Int
}
