package com.spotsapp.mappers

import com.spotsapp.dto.follow.FollowResponse
import com.spotsapp.entities.Follow
import org.springframework.stereotype.Component

@Component
class FollowMapper {

    /** Para GET /follows/me/following — muestra a quién sigue el usuario actual. */
    fun toFollowingResponse(follow: Follow): FollowResponse =
        FollowResponse(
            username = follow.followingUsername,
            followedAt = follow.createdAt
        )

    /** Para GET /follows/me/followers — muestra quién sigue al usuario actual. */
    fun toFollowerResponse(follow: Follow): FollowResponse =
        FollowResponse(
            username = follow.followerUsername,
            followedAt = follow.createdAt
        )
}
