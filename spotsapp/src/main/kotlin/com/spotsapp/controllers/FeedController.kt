package com.spotsapp.controllers

import com.spotsapp.dto.spot.SpotResponse
import com.spotsapp.services.FeedService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/** RF-08 — spots recientes APPROVED de las cuentas que sigue el usuario actual. */
@RestController
class FeedController(
    private val feedService: FeedService
) {

    @GetMapping("/feed")
    fun getFeed(
        authentication: Authentication,
        @PageableDefault(size = 20, sort = ["createdAt"], direction = Sort.Direction.DESC) pageable: Pageable
    ): Page<SpotResponse> = feedService.getFeed(authentication.name, pageable)
}
