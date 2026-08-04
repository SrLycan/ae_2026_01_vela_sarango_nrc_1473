package com.spotsapp.controllers

import com.spotsapp.dto.stats.StatsResponse
import com.spotsapp.services.StatsService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

/** RF-08 — público, como un leaderboard. Puntos/nivel calculados en el momento (ADR-003). */
@RestController
class StatsController(
    private val statsService: StatsService
) {

    @GetMapping("/profile/{username}/stats")
    fun getStats(@PathVariable username: String): StatsResponse = statsService.getStats(username)
}
