package com.spotsapp.controllers

import com.spotsapp.dto.follow.FollowResponse
import com.spotsapp.services.FollowService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/** RF-07 — seguimiento social entre usuarios. Todo requiere USER/ADMIN autenticado. */
@RestController
@RequestMapping("/follows")
class FollowController(
    private val followService: FollowService
) {

    @PostMapping("/{username}")
    @ResponseStatus(HttpStatus.CREATED)
    fun follow(@PathVariable username: String, authentication: Authentication) {
        followService.follow(authentication.name, username)
    }

    @DeleteMapping("/{username}")
    fun unfollow(@PathVariable username: String, authentication: Authentication): ResponseEntity<Void> {
        followService.unfollow(authentication.name, username)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/me/following")
    fun getFollowing(authentication: Authentication): List<FollowResponse> =
        followService.getFollowing(authentication.name)

    @GetMapping("/me/followers")
    fun getFollowers(authentication: Authentication): List<FollowResponse> =
        followService.getFollowers(authentication.name)
}
