package com.spotsapp.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.web.SecurityFilterChain

/**
 * Reglas de autorización por endpoint — ver /docs/matriz-endpoints.md para la tabla completa
 * (RNF-01). La API es stateless (RNF-02): sin sesión HTTP, CSRF deshabilitado (no hay cookies
 * de sesión que proteger), todo se autentica vía JWT de Cognito en el header Authorization.
 *
 * "Propiedad" sobre un recurso (ej. editar solo tu propio Spot) NO se valida acá — eso lo hacen
 * los services (Fase 5) porque depende del dato, no de la ruta. Acá solo se filtra por rol.
 */
@Configuration
class SecurityConfig {

    @Bean
    fun securityFilterChain(http: HttpSecurity, decoder: JwtDecoder): SecurityFilterChain {
        http {
            csrf { disable() }
            sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }

            authorizeHttpRequests {
                // --- Category (RF-02) ---
                authorize(HttpMethod.GET, "/categories/**", permitAll)
                authorize(HttpMethod.POST, "/categories", hasRole("ADMIN"))
                authorize(HttpMethod.PUT, "/categories/**", hasRole("ADMIN"))
                authorize(HttpMethod.DELETE, "/categories/**", hasRole("ADMIN"))

                // --- Spot (RF-03, RF-04) — /spots/me y /spots/pending antes que /spots/{id} por especificidad ---
                authorize(HttpMethod.GET, "/spots/me", hasAnyRole("USER", "ADMIN"))
                authorize(HttpMethod.GET, "/spots/pending", hasRole("ADMIN"))
                authorize(HttpMethod.PATCH, "/spots/*/approve", hasRole("ADMIN"))
                authorize(HttpMethod.PATCH, "/spots/*/reject", hasRole("ADMIN"))
                authorize(HttpMethod.GET, "/spots/**", permitAll)
                authorize(HttpMethod.POST, "/spots", hasAnyRole("USER", "ADMIN"))
                authorize(HttpMethod.PUT, "/spots/**", hasAnyRole("USER", "ADMIN"))
                authorize(HttpMethod.DELETE, "/spots/**", hasAnyRole("USER", "ADMIN"))

                // --- Media (RF-05) --- "/**" cubre /spots/{id}/media y las sub-rutas
                // /presign y /confirm del flujo de subida en dos pasos (ADR-001).
                authorize(HttpMethod.GET, "/spots/*/media/**", permitAll)
                authorize(HttpMethod.POST, "/spots/*/media/**", hasAnyRole("USER", "ADMIN"))
                authorize(HttpMethod.DELETE, "/media/**", hasAnyRole("USER", "ADMIN"))

                // --- Review (RF-06) ---
                authorize(HttpMethod.GET, "/spots/*/reviews", permitAll)
                authorize(HttpMethod.POST, "/reviews", hasAnyRole("USER", "ADMIN"))
                authorize(HttpMethod.PUT, "/reviews/**", hasAnyRole("USER", "ADMIN"))
                authorize(HttpMethod.DELETE, "/reviews/**", hasAnyRole("USER", "ADMIN"))

                // --- Follow (RF-07) ---
                authorize(HttpMethod.POST, "/follows/**", hasAnyRole("USER", "ADMIN"))
                authorize(HttpMethod.DELETE, "/follows/**", hasAnyRole("USER", "ADMIN"))
                authorize(HttpMethod.GET, "/follows/me/**", hasAnyRole("USER", "ADMIN"))

                // --- Feed y Stats (RF-08) ---
                authorize(HttpMethod.GET, "/feed", hasAnyRole("USER", "ADMIN"))
                authorize(HttpMethod.GET, "/profile/*/stats", permitAll)

                // --- Foto de perfil / banner (Postgres, no S3) ---
                authorize(HttpMethod.PUT, "/profile/me/avatar", hasAnyRole("USER", "ADMIN"))
                authorize(HttpMethod.PUT, "/profile/me/banner", hasAnyRole("USER", "ADMIN"))
                authorize(HttpMethod.GET, "/profile/*/avatar", permitAll)
                authorize(HttpMethod.GET, "/profile/*/banner", permitAll)

                // Cualquier otro endpoint no cubierto explícitamente por la matriz
                authorize(anyRequest, authenticated)
            }

            oauth2ResourceServer {
                jwt {
                    jwtDecoder = decoder
                    jwtAuthenticationConverter = jwtAuthenticationConverter()
                }
            }
        }
        return http.build()
    }

    /**
     * Traduce `cognito:groups` a `ROLE_*` (CognitoAuthoritiesConverter) y usa el claim
     * `username` de Cognito como nombre de principal, para que coincida con `ownerUsername`
     * / `username` en las entidades (ver ADR-002).
     */
    private fun jwtAuthenticationConverter(): JwtAuthenticationConverter {
        val converter = JwtAuthenticationConverter()
        converter.setJwtGrantedAuthoritiesConverter(CognitoAuthoritiesConverter())
        converter.setPrincipalClaimName("username")
        return converter
    }
}
