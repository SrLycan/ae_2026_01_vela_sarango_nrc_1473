package com.spotsapp.exceptions

/**
 * Excepción genérica para reglas de negocio que no encajan en las otras categorías, ej.:
 * - Reseñar un Spot que no está APPROVED.
 * - Seguirse a sí mismo (FollowService.follow()).
 * - Registrar Media sobre un Spot que no está en un estado válido para eso.
 * Traducida a 400 por el GlobalExceptionHandler.
 */
class BusinessRuleException(message: String) : RuntimeException(message)
