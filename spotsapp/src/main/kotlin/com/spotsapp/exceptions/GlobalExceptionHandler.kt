package com.spotsapp.exceptions

import jakarta.servlet.http.HttpServletRequest
import org.hibernate.TransientPropertyValueException
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.orm.jpa.JpaSystemException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

/**
 * Punto único de traducción excepción -> respuesta HTTP para toda la API.
 * Los services solo lanzan las excepciones de este paquete (o dejan pasar las de
 * validación de Spring); este handler decide el código y arma el ErrorResponse.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    // 404 — recurso no encontrado
    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleNotFound(ex: ResourceNotFoundException, request: HttpServletRequest): ResponseEntity<ErrorResponse> =
        build(HttpStatus.NOT_FOUND, ex.message ?: "Recurso no encontrado", request)

    // 403 — sin permiso sobre el recurso (propiedad o rol)
    @ExceptionHandler(ForbiddenOperationException::class)
    fun handleForbidden(ex: ForbiddenOperationException, request: HttpServletRequest): ResponseEntity<ErrorResponse> =
        build(HttpStatus.FORBIDDEN, ex.message ?: "Operación no permitida", request)

    // 409 — reseña duplicada (constraint único spot+usuario)
    @ExceptionHandler(DuplicateReviewException::class)
    fun handleDuplicateReview(ex: DuplicateReviewException, request: HttpServletRequest): ResponseEntity<ErrorResponse> =
        build(HttpStatus.CONFLICT, ex.message ?: "Reseña duplicada", request)

    // 409 — cualquier otra violación de constraint único a nivel de BD que se escape
    // de una validación explícita en el service (ej. Follow duplicado, Category.name repetido).
    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrity(ex: DataIntegrityViolationException, request: HttpServletRequest): ResponseEntity<ErrorResponse> {
        log.warn("Violación de integridad de datos en {}: {}", request.requestURI, ex.message)
        return build(HttpStatus.CONFLICT, "El recurso ya existe o viola una restricción única", request)
    }

    // 400 — regla de negocio incumplida
    @ExceptionHandler(BusinessRuleException::class)
    fun handleBusinessRule(ex: BusinessRuleException, request: HttpServletRequest): ResponseEntity<ErrorResponse> =
        build(HttpStatus.BAD_REQUEST, ex.message ?: "Regla de negocio incumplida", request)

    // 400 — errores de validación de @Valid en los *CreateRequest/*UpdateRequest
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException, request: HttpServletRequest): ResponseEntity<ErrorResponse> {
        val fieldErrors = ex.bindingResult.fieldErrors.associate {
            it.field to (it.defaultMessage ?: "valor inválido")
        }
        return build(
            HttpStatus.BAD_REQUEST,
            "Error de validación en los campos enviados",
            request,
            fieldErrors
        )
    }

    // 400 — tipo de parámetro incorrecto en path/query (ej. /spots/abc en vez de /spots/1)
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(ex: MethodArgumentTypeMismatchException, request: HttpServletRequest): ResponseEntity<ErrorResponse> =
        build(HttpStatus.BAD_REQUEST, "Parámetro '${ex.name}' con formato inválido", request)

    // 500 — JpaSystemException (ej. entidad detached referenciada al persistir)
    @ExceptionHandler(JpaSystemException::class)
    fun handleJpaSystem(ex: JpaSystemException, request: HttpServletRequest): ResponseEntity<ErrorResponse> {
        val cause = ex.rootCause
        log.warn("Error JPA en {}: {} — causa: {}", request.requestURI, ex.message, cause?.message)

        if (cause is TransientPropertyValueException) {
            return build(
                HttpStatus.BAD_REQUEST,
                "El recurso referenciado no está disponible en esta sesión. Intenta nuevamente.",
                request
            )
        }

        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Error de base de datos al procesar la solicitud", request)
    }

    // 500 — fallback para cualquier excepción no controlada explícitamente
    @ExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception, request: HttpServletRequest): ResponseEntity<ErrorResponse> {
        log.error("Error no controlado en {}", request.requestURI, ex)
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error interno inesperado", request)
    }

    private fun build(
        status: HttpStatus,
        message: String,
        request: HttpServletRequest,
        fieldErrors: Map<String, String>? = null
    ): ResponseEntity<ErrorResponse> {
        val body = ErrorResponse(
            status = status.value(),
            error = status.reasonPhrase,
            message = message,
            path = request.requestURI,
            fieldErrors = fieldErrors
        )
        return ResponseEntity.status(status).body(body)
    }
}
