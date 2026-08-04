package com.spotsapp.repositories

import com.spotsapp.entities.Category
import org.springframework.data.jpa.repository.JpaRepository

interface CategoryRepository : JpaRepository<Category, Long> {

    fun findByName(name: String): Category?

    fun existsByName(name: String): Boolean
}
