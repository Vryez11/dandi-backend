package com.dandi.nyummy.meal.entity

import com.dandi.nyummy.common.enum.Status
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
    var name: String = "",

    @Column
    var carbs: Int? = null,

    @Column
    var protein: Int? = null,

    @Column
    var fat: Int? = null,

    @Column
    var score: Int? = null,

    @Column
    var calory: Int? = null,

    @Column
    @Enumerated(EnumType.STRING)
    var status: Status,

    @Column
    val imageKey: String,

    @Column
    val mealAt: Instant,

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
) {
    fun updateNutrition(calory: Int, carbs: Int, protein: Int, fat: Int) {
        this.status = Status.COMPLETED
        this.calory = calory
        this.carbs = carbs
        this.protein = protein
        this.fat = fat
    }

    fun updateStatus(status: Status) {
        this.status = status
    }
}