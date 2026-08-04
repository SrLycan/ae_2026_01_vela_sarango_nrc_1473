package com.spotsapp.exceptions

/**
 * Se lanza cuando un usuario autenticado intenta una operación sobre un recurso que no le
 * pertenece (ej. editar/borrar un Spot ajeno, o un Media/Review ajenos), o cuando intenta
 * una acción que requiere un rol que no tiene y que no fue ya bloqueada por SecurityConfig.
 * Traducida a 403 por el GlobalExceptionHandler.
 */
class ForbiddenOperationException(message: String) : RuntimeException(message)
