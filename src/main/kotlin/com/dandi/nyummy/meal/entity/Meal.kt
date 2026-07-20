package com.dandi.nyummy.meal.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "meal")
class Meal(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column
    val userId: Long = 0,

    @Column
    var name: String = "",

    @Column
    val imageUrl: String? = null,

    @Column
    val carbs: Int? = null,

    @Column
    val protein: Int? = null,

    @Column
    val fat: Int? = null,

    @Column
    val score: Int? = null,

    @Column
    val calory: Int? = null,

    @Column
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column
    val mealAt: LocalDateTime? = null,

    @Column
    val updatedAt: LocalDateTime? = null,

    @Column
    val deletedAt: LocalDateTime? = null,

    @Column
    val status: String? = null,

    @Column
    var isDeleted: Boolean = false,
)
