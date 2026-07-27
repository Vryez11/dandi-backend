package com.dandi.nyummy.profile.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "profile")
class Profile(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column
    val userId: Long = 0,

    @Column
    val nickname: String? = null,

    @Column
    val birth: LocalDateTime? = null,

    @Column
    val gender: Byte? = null,

    @Column
    val height: Int? = null,

    @Column
    val weight: Int? = null,

    @Column
    val updatedAt: LocalDateTime? = null,

    @Column
    val lastLoginAt: LocalDateTime = LocalDateTime.now(),

    @Column
    val coin: Int = 0,
)
