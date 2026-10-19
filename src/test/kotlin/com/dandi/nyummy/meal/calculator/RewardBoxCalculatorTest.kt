package com.dandi.nyummy.meal.calculator

import com.dandi.nyummy.mail.enum.RewardBoxCategory
import com.dandi.nyummy.meal.config.RewardBoxProperties
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RewardBoxCalculatorTest {

    private val rewardBoxes = mapOf(
        RewardBoxCategory.BRONZE to RewardBoxProperties(rate = 40, minCoinQuantity = 100, maxCoinQuantity = 199),
        RewardBoxCategory.SILVER to RewardBoxProperties(rate = 40, minCoinQuantity = 200, maxCoinQuantity = 349),
        RewardBoxCategory.GOLD to RewardBoxProperties(rate = 20, minCoinQuantity = 350, maxCoinQuantity = 500),
    )

    private val random = mockk<Random>()

    // 등급을 정하는 굴림만 고정하고, 코인 수는 범위의 최솟값을 돌려준다.
    private fun pickRewardBoxCategory(
        roll: Int,
        rewardBoxes: Map<RewardBoxCategory, RewardBoxProperties> = this.rewardBoxes,
    ): RewardBoxCategory {
        every { random.nextInt(100) } returns roll
        every { random.nextInt(any(), any()) } answers { firstArg() }
        return pickRewardBox(rewardBoxes, random).category
    }

    // pickRewardBox

    @Test
    fun `굴린 값이 0이거나 39면 브론즈다`() {
        assertEquals(RewardBoxCategory.BRONZE, pickRewardBoxCategory(0))
        assertEquals(RewardBoxCategory.BRONZE, pickRewardBoxCategory(39))
    }

    @Test
    fun `굴린 값이 40이거나 79면 실버다`() {
        assertEquals(RewardBoxCategory.SILVER, pickRewardBoxCategory(40))
        assertEquals(RewardBoxCategory.SILVER, pickRewardBoxCategory(79))
    }

    @Test
    fun `굴린 값이 80이거나 99면 골드다`() {
        assertEquals(RewardBoxCategory.GOLD, pickRewardBoxCategory(80))
        assertEquals(RewardBoxCategory.GOLD, pickRewardBoxCategory(99))
    }

    @Test
    fun `rate가 0인 등급은 뽑히지 않는다`() {
        val noSilverRewardBoxes = mapOf(
            RewardBoxCategory.BRONZE to RewardBoxProperties(rate = 50, minCoinQuantity = 100, maxCoinQuantity = 199),
            RewardBoxCategory.SILVER to RewardBoxProperties(rate = 0, minCoinQuantity = 200, maxCoinQuantity = 349),
            RewardBoxCategory.GOLD to RewardBoxProperties(rate = 50, minCoinQuantity = 350, maxCoinQuantity = 500),
        )

        assertEquals(RewardBoxCategory.BRONZE, pickRewardBoxCategory(49, noSilverRewardBoxes))
        assertEquals(RewardBoxCategory.GOLD, pickRewardBoxCategory(50, noSilverRewardBoxes))
    }

    @Test
    fun `코인 수는 등급 범위의 양 끝을 포함해 뽑는다`() {
        every { random.nextInt(100) } returns 80
        every { random.nextInt(350, 501) } returns 500

        val result = pickRewardBox(rewardBoxes, random)

        assertEquals(500, result.coinQuantity)
        verify(exactly = 1) { random.nextInt(350, 501) }
    }

    @Test
    fun `고정 시드로 10만 번 뽑으면 등급 비율이 rate와 1포인트 안에서 같고 코인 수는 늘 등급 범위 안이다`() {
        val seededRandom = Random(42)
        val counts = mutableMapOf<RewardBoxCategory, Int>()

        repeat(TRIAL_COUNT) {
            val result = pickRewardBox(rewardBoxes, seededRandom)
            val rewardBox = rewardBoxes.getValue(result.category)
            assertTrue(result.coinQuantity in rewardBox.minCoinQuantity..rewardBox.maxCoinQuantity)
            counts.merge(result.category, 1, Int::plus)
        }

        rewardBoxes.forEach { (category, rewardBox) ->
            val actualRate = counts.getValue(category) * 100.0 / TRIAL_COUNT
            assertTrue(abs(actualRate - rewardBox.rate) < 1.0, "$category: 기대 ${rewardBox.rate}, 실제 $actualRate")
        }
    }

    companion object {
        private const val TRIAL_COUNT = 100_000
    }
}
