package com.spotsapp

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SpotsAppApplication

fun main(args: Array<String>) {
	runApplication<SpotsAppApplication>(*args)
}
