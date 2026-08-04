package com.spotsapp.controllers

import com.spotsapp.dto.category.CategoryCreateRequest
import com.spotsapp.dto.category.CategoryResponse
import com.spotsapp.dto.category.CategoryUpdateRequest
import com.spotsapp.services.CategoryService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * RF-02. Rol filtrado por SecurityConfig (Fase 6): GET público, POST/PUT/DELETE solo ADMIN.
 */
@RestController
@RequestMapping("/categories")
class CategoryController(
    private val categoryService: CategoryService
) {

    @GetMapping
    fun listAll(): List<CategoryResponse> = categoryService.listAll()

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): CategoryResponse = categoryService.getById(id)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: CategoryCreateRequest): CategoryResponse =
        categoryService.create(request)

    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @Valid @RequestBody request: CategoryUpdateRequest): CategoryResponse =
        categoryService.update(id, request)

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        categoryService.delete(id)
        return ResponseEntity.noContent().build()
    }
}
