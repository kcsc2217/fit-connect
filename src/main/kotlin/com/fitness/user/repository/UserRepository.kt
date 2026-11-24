package com.fitness.user.repository

import com.fitness.user.domain.User
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query

interface UserRepository : JpaRepository<User, Long> {

    /** SELECT ... FOR UPDATE. 같은 회원의 온보딩 동시 제출을 줄 세운다 (계획 009) */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    fun findByIdForUpdate(id: Long): User?
}
