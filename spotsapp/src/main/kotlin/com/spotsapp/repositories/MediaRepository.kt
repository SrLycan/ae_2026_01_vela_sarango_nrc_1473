package com.spotsapp.repositories

import com.spotsapp.entities.Media
import org.springframework.data.jpa.repository.JpaRepository

interface MediaRepository : JpaRepository<Media, Long> {

    fun findBySpotId(spotId: Long): List<Media>

    fun existsByIdAndSpotOwnerUsername(id: Long, ownerUsername: String): Boolean
}
