package com.spotsapp.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.presigner.S3Presigner

/**
 * Bean del cliente de presigned URLs de S3, usado por MediaService (ADR-001).
 * Las credenciales se resuelven con la cadena por defecto del SDK (rol de la
 * tarea/instancia en AWS, o variables de entorno AWS_ACCESS_KEY_ID/AWS_SECRET_ACCESS_KEY
 * en local/Docker) — no se configuran explícitamente aquí.
 */
@Configuration
class S3Config {

    @Value("\${aws.s3.region:us-east-1}")
    private lateinit var region: String

    @Bean
    fun s3Presigner(): S3Presigner =
        S3Presigner.builder()
            .region(Region.of(region))
            .build()
}
