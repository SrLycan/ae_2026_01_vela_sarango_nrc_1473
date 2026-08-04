package com.spotsapp.entities.enums

/**
 * Rareza asignada por un ADMIN al aprobar un Spot (Paso 5.2 — SpotService.approve()).
 * Determina, junto con pointsReward, el peso del spot en la gamificación.
 */
enum class Rarity {
    COMMON,
    RARE,
    EPIC,
    LEGENDARY
}
