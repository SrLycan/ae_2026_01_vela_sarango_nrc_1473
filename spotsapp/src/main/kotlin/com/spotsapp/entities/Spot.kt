package com.spotsapp.entities

import com.spotsapp.entities.enums.Rarity
import com.spotsapp.entities.enums.SpotStatus
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(
    name = "spots",
    indexes = [
        Index(name = "idx_spots_status", columnList = "status"),
        Index(name = "idx_spots_owner_username", columnList = "owner_username")
    ]
)
class Spot(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, length = 120)
    var name: String,

    @Column(nullable = false, length = 1000)
    var description: String,

    @Column(nullable = false)
    var latitude: Double,

    @Column(nullable = false)
    var longitude: Double,

    @Column(nullable = false, length = 255)
    var address: String,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false, foreignKey = ForeignKey(name = "fk_spots_category"))
    var category: Category,

    /** Username/sub de Cognito del creador. No hay FK a tabla local — ver ADR-002. */
    @Column(name = "owner_username", nullable = false, length = 120)
    val ownerUsername: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: SpotStatus = SpotStatus.PENDING,

    /** Asignada solo al aprobar (SpotService.approve()). Null mientras esté PENDING/REJECTED. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    var rarity: Rarity? = null,

    /** Asignados solo al aprobar. Usados por StatsService para el cálculo de puntos (ADR-003). */
    @Column(name = "points_reward")
    var pointsReward: Int? = null,

    @Column(name = "rejection_reason", length = 500)
    var rejectionReason: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    @OneToMany(mappedBy = "spot", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    val media: MutableList<Media> = mutableListOf(),

    @OneToMany(mappedBy = "spot", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    val reviews: MutableList<Review> = mutableListOf()
)
