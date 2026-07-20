package com.dandi.nyummy.meal.entity

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

@Entity
@EntityListeners(AuditingEntityListener::class)
@Table(name = "meal")
class Meal(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column
    val name: String = "",

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
    val status: String? = null,

    @Column
    val imageKey: String? = null,

    @Column
    val mealAt: Instant? = null,

    @Column
    @CreatedDate
    var createdAt: Instant = Instant.now(),

    @Column
    @LastModifiedDate
    var updatedAt: Instant? = null,

    @Column
    val deletedAt: Instant? = null,

    @Column
    val isDeleted: Boolean = false,

    @Column
    val userId: Long = 0,

    @Column
    val iconId: Long = 0,
)