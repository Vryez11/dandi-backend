package com.dandi.nyummy.streak.service

import com.dandi.nyummy.streak.dto.StreakResponse
import com.dandi.nyummy.streak.entity.Streak
import com.dandi.nyummy.streak.repository.StreakRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@Service
class StreakService(private val streakRepository: StreakRepository, private val clock: Clock) {
    companion object {
        // 식사 기록의 하루 경계(MealAttemptService)와 같은 기준으로 날짜를 나눈다.
        // TODO: 사용자별 timezone에 맞게 계산
        private val MEAL_ZONE: ZoneId = ZoneId.of("Asia/Seoul")
    }

    /**
     * 사용자가 지금 식사를 기록한 것으로 연속 일수를 갱신한다. 첫 기록이면 1일짜리 행을 만든다.
     *
     * 기록 시각은 서버 시각이다. 식사의 [com.dandi.nyummy.meal.entity.Meal.mealAt]은 EXIF 촬영 시각일 수 있어
     * 기기 시계를 따라가므로 쓰지 않는다.
     *
     * 같은 사용자의 기록은 호출하는 쪽이 줄 세워야 한다. 동시에 들어오면 첫 행 INSERT가 겹치거나(유니크 제약 위반)
     * 한쪽의 일수 갱신이 유실된다. [com.dandi.nyummy.meal.service.MealTransactionService.createMeal]은 프로필 행 잠금으로 줄 세운다.
     *
     * 행이 있으면 변경 감지로 저장하므로 트랜잭션이 있어야 반영된다. 혼자 불려도 갱신이 조용히 사라지지 않게
     * 트랜잭션을 걸고, 호출하는 쪽 트랜잭션이 있으면 거기에 합류한다.
     *
     * @param userId 식사를 기록한 사용자 ID
     */
    @Transactional
    fun updateStreak(userId: Long) {
        val now = Instant.now(clock)
        val streak = streakRepository.findByUserId(userId)

        if (streak == null) {
            streakRepository.save(Streak(userId = userId, lastMealAt = now))
            return
        }

        streak.updateStreakDays(now, MEAL_ZONE)
    }

    /**
     * 사용자의 연속 기록 현황을 조회한다.
     *
     * 행은 첫 식사 기록 때 만들어지므로, 없으면 아직 기록한 적 없는 정상 상태로 보고 0일을 반환한다.
     *
     * @param userId 조회하는 사용자 ID
     * @return 오늘이나 어제까지 이어진 연속 일수를 담은 [StreakResponse]
     */
    @Transactional(readOnly = true)
    fun getStreak(userId: Long): StreakResponse {
        val streakDays = streakRepository.findByUserId(userId)
            ?.calculateStreakDays(Instant.now(clock), MEAL_ZONE)
            ?: 0

        // TODO: 연속 일수 목표 보상이 정해지면 recordsUntilNextReward 계산하기
        return StreakResponse(streakDays = streakDays, recordsUntilNextReward = 0)
    }
}
