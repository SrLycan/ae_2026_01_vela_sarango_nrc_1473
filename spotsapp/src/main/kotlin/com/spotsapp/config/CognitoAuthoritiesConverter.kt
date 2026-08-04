package com.spotsapp.config

import org.slf4j.LoggerFactory
import org.springframework.core.convert.converter.Converter
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt

/**
 * Cognito expone los grupos de un usuario (ej. "ADMIN", "USER") en el claim `cognito:groups`
 * del JWT. Spring Security espera authorities con prefijo `ROLE_` para que `hasRole("ADMIN")`
 * funcione, así que este converter traduce cada grupo a `ROLE_<GRUPO_EN_MAYUSCULAS>`.
 *
 * Si el token no incluye el claim `cognito:groups` (usuario sin grupo asignado en Cognito),
 * se asigna el rol `ROLE_USER` por defecto para que el usuario pueda operar en la app.
 * Los administradores deben tener el grupo "ADMIN" en Cognito para acceder a endpoints
 * de moderación (ej. aprobar/rechazar spots).
 */
class CognitoAuthoritiesConverter : Converter<Jwt, Collection<GrantedAuthority>> {

    companion object {
        private val log = LoggerFactory.getLogger(CognitoAuthoritiesConverter::class.java)
        private const val GROUPS_CLAIM = "cognito:groups"
    }

    override fun convert(jwt: Jwt): Collection<GrantedAuthority> {
        val groups = jwt.getClaimAsStringList(GROUPS_CLAIM)
        if (groups.isNullOrEmpty()) {
            log.debug("JWT sin 'cognito:groups' — asignando ROLE_USER por defecto al usuario '{}'", jwt.subject)
            return listOf(SimpleGrantedAuthority("ROLE_USER"))
        }
        return groups.map { group -> SimpleGrantedAuthority("ROLE_${group.uppercase()}") }
    }
}
