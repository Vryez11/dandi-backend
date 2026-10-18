package com.dandi.nyummy.meal.calculator

import com.dandi.nyummy.mail.enum.RewardBoxCategory
import com.dandi.nyummy.meal.config.RewardBoxProperties
import com.dandi.nyummy.meal.dto.RewardBoxResult
import kotlin.random.Random

private const val FULL_RATE = 100

/**
 * 식사 보상 상자를 뽑는다. 등급을 rate로 뽑고, 그 등급의 코인 범위(양 끝 포함)에서 코인 수를 고르게 뽑는다.
 *
 * 0~99를 굴려 [RewardBoxCategory] 선언 순서대로 rate를 누적하다가 처음으로 굴린 값을 넘는 등급을 고른다.
 * rate가 40/40/20이면 0~39는 브론즈, 40~79는 실버, 80~99는 골드다.
 * 모든 등급이 있고 rate 합이 100이라는 것은 [com.dandi.nyummy.meal.config.MealProperties]가 앱이 뜰 때 보장한다.
 */
fun pickRewardBox(rewardBoxes: Map<RewardBoxCategory, RewardBoxProperties>, random: Random): RewardBoxResult {
    val roll = random.nextInt(FULL_RATE)
    var accumulatedRate = 0

    for (category in RewardBoxCategory.entries) {
        val rewardBox = rewardBoxes.getValue(category)
        accumulatedRate += rewardBox.rate
        if (roll < accumulatedRate) {
            val coinQuantity = random.nextInt(rewardBox.minCoinQuantity, rewardBox.maxCoinQuantity + 1)
            return RewardBoxResult(category, coinQuantity)
        }
    }

    error("상자 등급 rate의 합이 ${FULL_RATE}보다 작습니다: $accumulatedRate")
}
