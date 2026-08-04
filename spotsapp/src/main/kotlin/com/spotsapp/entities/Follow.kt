package com.spotsapp.entities

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(
    name = "follows",
    uniqueConstraints = [
        // Un usuario no puede seguir dos veces al mismo usuario
        UniqueConstraint(
            name = "uk_follows_follower_following",
            columnNames = ["follower_username", "following_username"]
        )
    ],
    indexes = [
        Index(name = "idx_follows_follower", columnList = "follower_username"),
        Index(name = "idx_follows_following", columnList = "following_username")
    ]
)
class Follow(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    /** Quien sigue. La regla "no seguirse a sí mismo" se valida en FollowService.follow() (Paso 5.5)
     *  y además está reforzada por un CHECK constraint a nivel de base de datos (V1__init.sql). */
    @Column(name = "follower_username", nullable = false, length = 120)
    val followerUsername: String,

    /** A quien se sigue. */
    @Column(name = "following_username", nullable = false, length = 120)
    val followingUsername: String,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)
