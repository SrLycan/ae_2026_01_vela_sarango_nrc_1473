package com.spotsapp.repositories

import com.spotsapp.entities.Follow
import org.springframework.data.jpa.repository.JpaRepository

interface FollowRepository : JpaRepository<Follow, Long> {

    fun existsByFollowerUsernameAndFollowingUsername(followerUsername: String, followingUsername: String): Boolean

    fun findByFollowerUsernameAndFollowingUsername(followerUsername: String, followingUsername: String): Follow?

    // FollowService.getFollowing()
    fun findByFollowerUsername(followerUsername: String): List<Follow>

    // FollowService.getFollowers()
    fun findByFollowingUsername(followingUsername: String): List<Follow>

    fun countByFollowerUsername(followerUsername: String): Long

    fun countByFollowingUsername(followingUsername: String): Long

    fun deleteByFollowerUsernameAndFollowingUsername(followerUsername: String, followingUsername: String)
}
