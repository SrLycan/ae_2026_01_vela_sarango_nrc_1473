package com.spotsapp.entities

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(
    name = "reviews",
    uniqueConstraints = [
        // Una única reseña por usuario y spot — validado también en ReviewService.create() (Paso 5.4)
        UniqueConstraint(name = "uk_reviews_spot_username", columnNames = ["spot_id", "username"])
    ],
    indexes = [Index(name = "idx_reviews_spot_id", columnList = "spot_id")]
)
class Review(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "spot_id", nullable = false, foreignKey = ForeignKey(name = "fk_reviews_spot"))
    var spot: Spot,

    /** Username/sub de Cognito del autor de la reseña — ver ADR-002. */
    @Column(nullable = false, length = 120)
    val username: String,

    @Column(nullable = false)
    var rating: Int,

    @Column(length = 1000)
    var comment: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)
