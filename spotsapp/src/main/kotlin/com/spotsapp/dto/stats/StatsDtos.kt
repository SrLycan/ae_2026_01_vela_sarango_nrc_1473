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
    val badge: String,
    // Rutas relativas (el cliente las combina con su base URL) hacia ProfileImageController.
    // null si el usuario no ha subido avatar/banner — el cliente usa su placeholder en ese caso.
    val avatarUrl: String? = null,
    val bannerUrl: String? = null
)
