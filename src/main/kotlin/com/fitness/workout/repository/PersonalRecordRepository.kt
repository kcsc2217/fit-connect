package com.fitness.workout.repository

import com.fitness.workout.domain.PersonalRecord
import com.fitness.workout.domain.PersonalRecordId
import org.springframework.data.jpa.repository.JpaRepository

interface PersonalRecordRepository : JpaRepository<PersonalRecord, PersonalRecordId>
