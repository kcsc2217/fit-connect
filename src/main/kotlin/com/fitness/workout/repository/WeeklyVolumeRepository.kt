package com.fitness.workout.repository

import com.fitness.workout.domain.WeeklyVolume
import com.fitness.workout.domain.WeeklyVolumeId
import org.springframework.data.jpa.repository.JpaRepository

interface WeeklyVolumeRepository : JpaRepository<WeeklyVolume, WeeklyVolumeId>
