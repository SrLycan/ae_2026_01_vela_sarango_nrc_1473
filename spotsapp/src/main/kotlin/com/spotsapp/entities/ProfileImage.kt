package com.spotsapp.entities

import com.spotsapp.entities.enums.ProfileImageType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant

/**
 * Foto de perfil (AVATAR) o banner (BANNER) de un usuario, guardada directamente en Postgres
 * como bytea — a diferencia de la media de spots (Media.kt), que se sube a S3. Se eligió BYTEA
 * en vez de S3 porque son imágenes pequeñas (recortadas/comprimidas en el cliente) y así se evita
 * depender de credenciales/bucket de AWS para un caso de uso simple de "una imagen por usuario".
 *
 * Un usuario tiene a lo sumo una fila por tipo de imagen (uk_profile_images_username_type):
 * subir una nueva reemplaza (UPDATE) la anterior en vez de acumular histórico.
 */
@Entity
@Table(
    name = "profile_images",
    uniqueConstraints = [UniqueConstraint(name = "uk_profile_images_username_type", columnNames = ["username", "image_type"])]
)
class ProfileImage(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @Column(name = "username", nullable = false, length = 120)
    val username: String = "",

    @Enumerated(EnumType.STRING)
    @Column(name = "image_type", nullable = false, length = 10)
    val imageType: ProfileImageType = ProfileImageType.AVATAR,

    @Column(name = "content_type", nullable = false, length = 100)
    val contentType: String = "image/jpeg",

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "image_data", nullable = false)
    val imageData: ByteArray = ByteArray(0),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now()
)