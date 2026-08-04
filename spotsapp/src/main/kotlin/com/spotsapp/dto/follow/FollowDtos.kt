package com.spotsapp.dto.follow

import java.time.Instant

/** Representa una entrada en /follows/me/following o /follows/me/followers. */
data class FollowResponse(
    val username: String,
    val followedAt: Instant
)
