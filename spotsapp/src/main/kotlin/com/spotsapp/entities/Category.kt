package com.spotsapp.entities

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(
    name = "categories",
    uniqueConstraints = [UniqueConstraint(name = "uk_categories_name", columnNames = ["name"])]
)
class Category(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, length = 60)
    var name: String,

    @Column(length = 255)
    var description: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    val spots: MutableList<Spot> = mutableListOf()
)
