package com.dandi.nyummy.meal.service

import com.dandi.nyummy.exception.BusinessException
import com.dandi.nyummy.exception.errorcode.UserErrorCode
import com.dandi.nyummy.mail.service.MailService
import com.dandi.nyummy.meal.dto.RewardBoxResult
import com.dandi.nyummy.meal.entity.Meal
import com.dandi.nyummy.meal.repository.MealRepository
import com.dandi.nyummy.user.repository.ProfileRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * 식사 기록에서 DB에 쓰는 부분만 짧은 트랜잭션으로 묶는다.
 * [MealService.createMeal]은 S3 호출 때문에 트랜잭션을 열지 않으므로, 같은 클래스 안에서 부르면 프록시를 거치지 않아
 * 트랜잭션이 걸리지 않는다. 그래서 별도 빈으로 둔다.
 */
@Service
class MealTransactionService(
    private val profileRepository: ProfileRepository,
    private val mealAttemptService: MealAttemptService,
    private val mealRepository: MealRepository,
    private val mailService: MailService,
) {
    companion object {
        private const val MEAL_REWARD_CONTENT = "식사 기록 보상"
    }

    /**
     * 식사와 보상 상자 우편을 한 트랜잭션으로 저장한다. 우편 저장이 실패하면 식사도 남기지 않는다.
     *
     * 하루 시도 횟수는 [MealService.createMeal]이 이미지 확정 전에 한 번 거르지만, 그 검사와 저장 사이에
     * 같은 사용자의 요청이 동시에 들어오면 둘 다 통과할 수 있다. 그래서 프로필 행을 잠가 같은 사용자의 기록을
     * 줄 세운 뒤 다시 센다. 잠금은 커밋하거나 롤백할 때 풀린다.
     *
     * 잠금보다 먼저 일반 조회를 하면 안 된다. REPEATABLE READ는 트랜잭션의 첫 일반 조회 시점 스냅샷을 계속 보므로,
     * 앞선 요청이 커밋한 식사를 못 보고 옛 횟수로 통과시킨다.
     *
     * @param meal 저장할 식사
     * @param rewardBox 이 식사의 보상으로 뽑은 상자
     * @param rewardMailExpiresAt 보상 상자 우편의 만료 시각
     * @return 저장된 [Meal]
     * @throws BusinessException [UserErrorCode.PROFILE_NOT_FOUND] 가입 시 만들어져야 할 프로필이 없는 경우
     * @throws BusinessException [com.dandi.nyummy.exception.errorcode.MealErrorCode.DAILY_COUNT_EXCEEDED] 다시 센 오늘 시도 횟수가 상한인 경우
     */
    @Transactional
    fun createMeal(meal: Meal, rewardBox: RewardBoxResult, rewardMailExpiresAt: Instant): Meal {
        profileRepository.findProfileWithLockByUserId(meal.userId)
            ?: throw BusinessException(UserErrorCode.PROFILE_NOT_FOUND)
        mealAttemptService.validateDailyCount(meal.userId)

        mealRepository.save(meal)
        mailService.createRewardBoxMail(
            userId = meal.userId,
            rewardBoxCategory = rewardBox.category,
            coinQuantity = rewardBox.coinQuantity,
            content = MEAL_REWARD_CONTENT,
            expiresAt = rewardMailExpiresAt,
        )
        return meal
    }
}
