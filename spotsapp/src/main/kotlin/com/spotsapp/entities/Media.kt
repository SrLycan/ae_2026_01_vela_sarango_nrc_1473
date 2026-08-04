package com.spotsapp.entities

import com.spotsapp.entities.enums.MediaType
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(
    name = "media",
    indexes = [Index(name = "idx_media_spot_id", columnList = "spot_id")]
)
class Media(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "spot_id", nullable = false, foreignKey = ForeignKey(name = "fk_media_spot"))
    var spot: Spot,

    /** URL final en S3, confirmada tras la subida vía presigned URL — ver ADR-001. */
    @Column(nullable = false, length = 500)
    var url: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    var type: MediaType,

    @Column(name = "uploaded_by_username", nullable = false, length = 120)
    val uploadedByUsername: String,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)
