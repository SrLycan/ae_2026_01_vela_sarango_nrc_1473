package com.spotsapp.utils

/**
 * Redondea coordenadas a 6 decimales (~11 cm de precisión en el ecuador) antes de guardarlas.
 * Sin esto, dos peticiones que representan "el mismo" punto real (una desde Places
 * Autocomplete, otra arrastrando el pin del mapa) podrían diferir en el último decimal y el
 * chequeo de "spot duplicado en la misma ubicación" (SpotService) nunca los detectaría como
 * iguales por comparación exacta de Double.
 */
fun Double.roundToCoordinatePrecision(): Double {
    val factor = 1_000_000.0 // 6 decimales
    return Math.round(this * factor) / factor
}

/**
 * Distancia entre dos puntos (lat/lng en grados) en metros, fórmula de Haversine.
 * Usada por SpotService para "¿esto ya es la misma ubicación?" con un radio de tolerancia
 * en vez de exigir coordenadas idénticas — dos pines cerca uno del otro (ej. el mismo lugar
 * pero uno tocado en el mapa y el otro elegido por el buscador de direcciones) deben contar
 * como la misma ubicación aunque sus decimales no coincidan exacto.
 */
fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadiusMeters = 6_371_000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return earthRadiusMeters * c
}
