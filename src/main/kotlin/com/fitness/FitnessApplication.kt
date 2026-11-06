package com.fitness

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

// 기본 계정(UserDetailsService)은 쓰지 않는다: 우리 인증은 JWT 필터가 한다 (계획 006)
@SpringBootApplication(exclude = [UserDetailsServiceAutoConfiguration::class])
@ConfigurationPropertiesScan
class FitnessApplication

fun main(args: Array<String>) {
    runApplication<FitnessApplication>(*args)
}