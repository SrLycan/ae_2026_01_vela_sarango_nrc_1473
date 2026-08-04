package com.spotsapp.dto.stats

/**
 * GET /profile/{username}/stats — calculado en el momento, sin tabla propia (ADR-003).
 * `level` y `badge` se derivan de `totalPoints` mediante umbrales fijos en StatsService.
 */
data class StatsResponse(
    val username: String,
    val totalPoints: Int,
    val spotsReviewed: Int,
    val level: Int,
    val badge: String
)
