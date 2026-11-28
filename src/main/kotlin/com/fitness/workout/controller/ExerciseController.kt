package com.fitness.workout.controller

import com.fitness.workout.dto.ExerciseResponse
import com.fitness.workout.service.ExerciseService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "운동 기록")
@RestController
@RequestMapping("/api/v1/exercises")
class ExerciseController(private val exerciseService: ExerciseService) {

    @Operation(summary = "종목 사전 (활성 종목, 부위·id 순)", security = [SecurityRequirement(name = "bearer")])
    @GetMapping
    fun list(): List<ExerciseResponse> = exerciseService.findAllActive().map(ExerciseResponse::from)
}
