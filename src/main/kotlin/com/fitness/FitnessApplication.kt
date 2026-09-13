package com.fitness

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class FitnessApplication

fun main(args: Array<String>) {
    runApplication<FitnessApplication>(*args)
}