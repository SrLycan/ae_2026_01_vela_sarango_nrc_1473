package com.spotsapp.dto.follow

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.Instant

/** Representa una entrada en /follows/me/following o /follows/me/followers. */
data class FollowResponse(
    val username: String,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    val followedAt: Instant
)
