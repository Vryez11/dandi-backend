package com.dandi.nyummy.streak.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant
import java.time.ZoneId

/**
 * 식사 기록의 연속 일수. 사용자당 하나이고, 첫 식사 기록 때 만든다.
 *
 * [lastMealAt]은 마지막으로 식사를 기록한(생성한) 서버 시각이다. 날짜로 저장하지 않고, 쓰는 쪽이 넘긴 시간대로
 * 그때그때 날짜를 계산한다. [com.dandi.nyummy.cat.entity.Cat.lastMealAt]과 이름이 같지만 그쪽은 분석이 끝났을 때 갱신한다.
 */
@Entity
@EntityListeners(AuditingEntityListener::class)
@Table(name = "streaks")
class Streak(
    @Column(name = "user_id", nullable = false, unique = true)
    val userId: Long,

    @Column(name = "last_meal_at", nullable = false)
    var lastMealAt: Instant,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    val id: Long = 0L

    // 첫 기록 때 만드는 행이라 1일부터 시작한다.
    @Column(name = "streak_days", nullable = false)
    var streakDays: Int = 1

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()

    /**
     * [mealAt]에 식사를 기록한 것으로 연속 일수를 갱신한다.
     *
     * 마지막 기록과 같은 날이면 일수는 그대로 두고, 바로 다음 날이면 하루 늘리고, 하루 이상 비었으면 1일부터 다시 센다.
     * 마지막 기록보다 이른 시각이 오면(서버 시계가 되돌아간 경우) 아무것도 바꾸지 않는다. 마지막 기록 시각이 뒤로 가면
     * 다음 기록이 이미 센 날을 한 번 더 세게 된다.
     */
    fun updateStreakDays(mealAt: Instant, zone: ZoneId) {
        if (mealAt.isBefore(lastMealAt)) {
            return
        }

        val lastMealDate = lastMealAt.atZone(zone).toLocalDate()
        val mealDate = mealAt.atZone(zone).toLocalDate()

        if (mealDate == lastMealDate.plusDays(1)) {
            streakDays += 1
        } else if (mealDate.isAfter(lastMealDate.plusDays(1))) {
            streakDays = 1
        }
        lastMealAt = mealAt
    }

    /**
     * [now]에 보여 줄 연속 일수를 계산한다.
     *
     * 마지막 기록일이 오늘이나 어제면 아직 이어지고 있으므로 [streakDays]를, 그보다 전이면 끊겼으므로 0을 반환한다.
     * 어제까지 인정하는 이유는 오늘 아직 기록하지 않은 사용자가 어제까지 이어 온 일수를 잃은 것처럼 보이지 않게 하려는 것이다.
     */
    fun calculateStreakDays(now: Instant, zone: ZoneId): Int {
        val lastMealDate = lastMealAt.atZone(zone).toLocalDate()
        val today = now.atZone(zone).toLocalDate()

        return if (lastMealDate.isBefore(today.minusDays(1))) 0 else streakDays
    }
}
