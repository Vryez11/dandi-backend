package com.dandi.nyummy.meal.service

import com.dandi.nyummy.meal.dto.DailyNutritionEvaluation
import com.dandi.nyummy.meal.dto.MealStatus
import com.dandi.nyummy.meal.dto.Nutrition
import com.dandi.nyummy.meal.entity.Meal
import com.dandi.nyummy.meal.repository.MealRepository
import com.dandi.nyummy.profile.entity.Profile
import com.dandi.nyummy.profile.repository.ProfileRepository
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.DateTimeException
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MealServiceTest {

    private val mealRepository = mock(MealRepository::class.java)
    private val profileRepository = mock(ProfileRepository::class.java)

    private val mealService = MealService(
        mealRepository = mealRepository,
        profileRepository = profileRepository,
        nutritionRecommendationCalculator = NutritionRecommendationCalculator(),
    )

    // 계산 편의를 위한 권장량: 90% / 150% 경계가 딱 떨어지는 값
    private val recommended = RecommendedDailyIntake(calory = 2000, carbs = 250, protein = 100, fat = 67)

    // Mockito의 any()/capture()는 null을 반환해서 코틀린 non-null 파라미터와 충돌한다.
    // 매처만 등록하고 더미 값을 돌려주는 우회 헬퍼 (mockito-kotlin이 하는 일과 동일)
    private fun anyDateTime(): LocalDateTime {
        ArgumentMatchers.any(LocalDateTime::class.java)
        return LocalDateTime.MIN
    }

    private fun capture(captor: ArgumentCaptor<LocalDateTime>): LocalDateTime {
        captor.capture()
        return LocalDateTime.MIN
    }

    private fun meal(
        calory: Int?,
        carbs: Int?,
        protein: Int?,
        fat: Int?,
        mealAt: LocalDateTime? = null,
    ) = Meal(userId = 1, name = "식사", calory = calory, carbs = carbs, protein = protein, fat = fat, mealAt = mealAt)

    // 기본 권장량(2000/250/100/67) 기준으로 4개 지표 모두 90~150% 안에 드는 식사
    private fun mealInRange(mealAt: LocalDateTime? = null) =
        meal(calory = 2400, carbs = 280, protein = 110, fat = 75, mealAt = mealAt)

    // ---------- calculateDailyNutritionEvaluation ----------

    @Test
    fun `식사 기록이 없으면 UNRECORDED를 반환한다`() {
        val result = mealService.calculateDailyNutritionEvaluation(emptyList(), recommended)

        assertEquals(DailyNutritionEvaluation.UNRECORDED, result)
    }

    @Test
    fun `모든 지표가 권장 범위 안이면 POSITIVE를 반환한다`() {
        val result = mealService.calculateDailyNutritionEvaluation(listOf(mealInRange()), recommended)

        assertEquals(DailyNutritionEvaluation.POSITIVE, result)
    }

    @Test
    fun `정확히 90퍼센트는 범위에 포함된다`() {
        // 칼로리 1800 = 2000의 90%, 나머지는 중간값
        val meals = listOf(meal(calory = 1800, carbs = 280, protein = 110, fat = 75))

        val result = mealService.calculateDailyNutritionEvaluation(meals, recommended)

        assertEquals(DailyNutritionEvaluation.POSITIVE, result)
    }

    @Test
    fun `정확히 150퍼센트는 범위에서 제외된다`() {
        // 칼로리 3000 = 2000의 150%
        val meals = listOf(meal(calory = 3000, carbs = 280, protein = 110, fat = 75))

        val result = mealService.calculateDailyNutritionEvaluation(meals, recommended)

        assertEquals(DailyNutritionEvaluation.NEGATIVE, result)
    }

    @Test
    fun `권장량에 못 미치면 NEGATIVE를 반환한다`() {
        // 칼로리 1500 = 2000의 75% (범위 미달)
        val meals = listOf(meal(calory = 1500, carbs = 280, protein = 110, fat = 75))

        val result = mealService.calculateDailyNutritionEvaluation(meals, recommended)

        assertEquals(DailyNutritionEvaluation.NEGATIVE, result)
    }

    @Test
    fun `한 지표만 벗어나도 NEGATIVE를 반환한다`() {
        // 단백질 200 = 100의 200%, 나머지는 전부 범위 안
        val meals = listOf(meal(calory = 2400, carbs = 280, protein = 200, fat = 75))

        val result = mealService.calculateDailyNutritionEvaluation(meals, recommended)

        assertEquals(DailyNutritionEvaluation.NEGATIVE, result)
    }

    @Test
    fun `여러 식사는 합산해서 판정한다`() {
        // 한 끼로는 미달이지만 세 끼 합치면 범위 안
        val meals = listOf(
            meal(calory = 800, carbs = 90, protein = 40, fat = 25),
            meal(calory = 800, carbs = 90, protein = 40, fat = 25),
            meal(calory = 800, carbs = 100, protein = 30, fat = 25),
        )

        val result = mealService.calculateDailyNutritionEvaluation(meals, recommended)

        assertEquals(DailyNutritionEvaluation.POSITIVE, result)
    }

    @Test
    fun `영양 정보가 전부 null인 기록은 UNRECORDED가 아니라 NEGATIVE다`() {
        // 기록 자체는 존재하므로 평가 대상 (null은 0으로 합산 → 전 지표 미달)
        val meals = listOf(meal(calory = null, carbs = null, protein = null, fat = null))

        val result = mealService.calculateDailyNutritionEvaluation(meals, recommended)

        assertEquals(DailyNutritionEvaluation.NEGATIVE, result)
    }

    // ---------- getMonthlyMeals ----------

    @Test
    fun `빈 달이면 모든 날짜가 UNRECORDED인 캘린더를 반환한다`() {
        `when`(mealRepository.getMealsByUserIdAndPeriod(anyLong(), anyDateTime(), anyDateTime()))
            .thenReturn(emptyList())
        `when`(profileRepository.getProfileByUserId(1L)).thenReturn(null)

        val result = mealService.getMonthlyMeals(userId = 1L, year = 2026, month = 7)

        assertEquals(2026, result.year)
        assertEquals(7, result.month)
        assertEquals(35, result.days.size)                                  // 6/28(일) ~ 8/1(토)
        assertEquals(LocalDate.of(2026, 6, 28), result.days.first().date)
        assertEquals(LocalDate.of(2026, 8, 1), result.days.last().date)
        assertTrue(result.days.all { it.dailyNutritionEvaluation == DailyNutritionEvaluation.UNRECORDED })

        // 날짜가 하루도 빠짐없이 오름차순인지
        result.days.zipWithNext().forEach { (prev, next) ->
            assertEquals(prev.date.plusDays(1), next.date)
        }
    }

    @Test
    fun `당월이 아닌 패딩 날짜는 isCurrentMonth가 false다`() {
        `when`(mealRepository.getMealsByUserIdAndPeriod(anyLong(), anyDateTime(), anyDateTime()))
            .thenReturn(emptyList())
        `when`(profileRepository.getProfileByUserId(1L)).thenReturn(null)

        val result = mealService.getMonthlyMeals(userId = 1L, year = 2026, month = 7)

        val (padding, currentMonth) = result.days.partition { !it.isCurrentMonth }
        assertEquals(4, padding.size)          // 6/28~30 + 8/1
        assertEquals(31, currentMonth.size)    // 7월 전체
        assertTrue(currentMonth.all { it.date.monthValue == 7 })
    }

    @Test
    fun `식사가 있는 날은 평가 결과가 반영된다`() {
        // 프로필 없음 → 기본 권장량(2000/250/100/67) 기준으로 판정됨
        val mealsOnJuly10 = listOf(mealInRange(mealAt = LocalDateTime.of(2026, 7, 10, 12, 0)))
        `when`(mealRepository.getMealsByUserIdAndPeriod(anyLong(), anyDateTime(), anyDateTime()))
            .thenReturn(mealsOnJuly10)
        `when`(profileRepository.getProfileByUserId(1L)).thenReturn(null)

        val result = mealService.getMonthlyMeals(userId = 1L, year = 2026, month = 7)

        val july10 = result.days.first { it.date == LocalDate.of(2026, 7, 10) }
        assertEquals(DailyNutritionEvaluation.POSITIVE, july10.dailyNutritionEvaluation)
        // 나머지 날짜는 전부 UNRECORDED
        assertTrue(result.days.filter { it.date != LocalDate.of(2026, 7, 10) }
            .all { it.dailyNutritionEvaluation == DailyNutritionEvaluation.UNRECORDED })
    }

    @Test
    fun `패딩 날짜의 식사도 평가되고 조회는 패딩 범위로 수행된다`() {
        // 6/28은 7월 캘린더의 패딩 날짜
        val mealsOnPaddingDay = listOf(mealInRange(mealAt = LocalDateTime.of(2026, 6, 28, 8, 0)))
        `when`(mealRepository.getMealsByUserIdAndPeriod(anyLong(), anyDateTime(), anyDateTime()))
            .thenReturn(mealsOnPaddingDay)
        `when`(profileRepository.getProfileByUserId(1L)).thenReturn(null)

        val result = mealService.getMonthlyMeals(userId = 1L, year = 2026, month = 7)

        val june28 = result.days.first { it.date == LocalDate.of(2026, 6, 28) }
        assertEquals(DailyNutritionEvaluation.POSITIVE, june28.dailyNutritionEvaluation)

        // 리포지토리 조회가 월 범위가 아니라 패딩 포함 범위(6/28~8/1)로 나갔는지 검증
        val captor = ArgumentCaptor.forClass(LocalDateTime::class.java)
        verify(mealRepository).getMealsByUserIdAndPeriod(eq(1L), capture(captor), capture(captor))
        val (start, end) = captor.allValues
        assertEquals(LocalDateTime.of(2026, 6, 28, 0, 0), start)
        assertTrue(!end.isBefore(LocalDateTime.of(2026, 8, 1, 23, 59, 59)), "종료 경계가 8/1 전체를 포함해야 함: $end")
    }

    @Test
    fun `mealAt이 없는 식사는 캘린더에서 제외된다`() {
        val mealWithoutMealAt = listOf(mealInRange(mealAt = null))
        `when`(mealRepository.getMealsByUserIdAndPeriod(anyLong(), anyDateTime(), anyDateTime()))
            .thenReturn(mealWithoutMealAt)
        `when`(profileRepository.getProfileByUserId(1L)).thenReturn(null)

        val result = mealService.getMonthlyMeals(userId = 1L, year = 2026, month = 7)

        assertTrue(result.days.all { it.dailyNutritionEvaluation == DailyNutritionEvaluation.UNRECORDED })
    }

    @Test
    fun `프로필이 있으면 개인화된 권장량으로 평가한다`() {
        // 남성 175cm/70kg → 권장 약 2301kcal. 1800kcal은 기본 권장량(2000)으로는 90%라 POSITIVE지만,
        // 개인화 권장량으로는 약 78%라 NEGATIVE가 되는 값 → 프로필이 실제로 반영되는지 구분 가능
        val profile = Profile(
            userId = 1L,
            birth = LocalDateTime.of(2001, 3, 15, 0, 0),
            gender = 0,
            height = 175,
            weight = 70,
        )
        val meals = listOf(meal(calory = 1800, carbs = 280, protein = 110, fat = 75,
            mealAt = LocalDateTime.of(2026, 7, 10, 12, 0)))
        `when`(mealRepository.getMealsByUserIdAndPeriod(anyLong(), anyDateTime(), anyDateTime()))
            .thenReturn(meals)
        `when`(profileRepository.getProfileByUserId(1L)).thenReturn(profile)

        val result = mealService.getMonthlyMeals(userId = 1L, year = 2026, month = 7)

        val july10 = result.days.first { it.date == LocalDate.of(2026, 7, 10) }
        assertEquals(DailyNutritionEvaluation.NEGATIVE, july10.dailyNutritionEvaluation)
    }

    // ---------- getDailyMeals ----------

    @Test
    fun `기록이 없는 날도 빈 목록과 0 합계, target을 반환한다`() {
        `when`(mealRepository.getMealsByUserIdAndPeriod(anyLong(), anyDateTime(), anyDateTime()))
            .thenReturn(emptyList())
        `when`(profileRepository.getProfileByUserId(1L)).thenReturn(null)

        val result = mealService.getDailyMeals(userId = 1L, year = 2026, month = 7, day = 12)

        assertEquals(LocalDate.of(2026, 7, 12), result.date)
        assertTrue(result.meals.isEmpty())
        assertEquals(Nutrition(calories = 0, carbs = 0, protein = 0, fat = 0), result.dailyNutrition.current)
        // 프로필이 없으면 기본 권장량이 target으로 내려간다 (기록이 없어도 target은 항상 존재)
        assertEquals(Nutrition(calories = 2000, carbs = 250, protein = 100, fat = 67), result.dailyNutrition.target)
    }

    @Test
    fun `여러 식사의 영양을 합산해 current로 반환한다`() {
        // API 스펙 예시와 동일한 합계: 1350/185/42/31
        val meals = listOf(
            meal(calory = 540, carbs = 60, protein = 40, fat = 12, mealAt = LocalDateTime.of(2026, 7, 12, 12, 36)),
            meal(calory = 810, carbs = 125, protein = 2, fat = 19, mealAt = LocalDateTime.of(2026, 7, 12, 18, 0)),
        )
        `when`(mealRepository.getMealsByUserIdAndPeriod(anyLong(), anyDateTime(), anyDateTime()))
            .thenReturn(meals)
        `when`(profileRepository.getProfileByUserId(1L)).thenReturn(null)

        val result = mealService.getDailyMeals(userId = 1L, year = 2026, month = 7, day = 12)

        assertEquals(2, result.meals.size)
        assertEquals(Nutrition(calories = 1350, carbs = 185, protein = 42, fat = 31), result.dailyNutrition.current)
    }

    @Test
    fun `식사 항목의 필드가 응답에 매핑된다`() {
        val poke = Meal(
            id = 7,
            userId = 1,
            name = "닭가슴살 포케",
            calory = 540,
            carbs = 60,
            protein = 40,
            fat = 12,
            mealAt = LocalDateTime.of(2026, 7, 12, 12, 36),
            status = "COMPLETED",
        )
        `when`(mealRepository.getMealsByUserIdAndPeriod(anyLong(), anyDateTime(), anyDateTime()))
            .thenReturn(listOf(poke))
        `when`(profileRepository.getProfileByUserId(1L)).thenReturn(null)

        val response = mealService.getDailyMeals(userId = 1L, year = 2026, month = 7, day = 12).meals.single()

        assertEquals(7L, response.mealId)
        assertEquals("닭가슴살 포케", response.name)
        assertEquals(LocalDateTime.of(2026, 7, 12, 12, 36), response.mealAt)
        assertEquals(540, response.calories)
        assertEquals(60, response.carbs)
        assertEquals(40, response.protein)
        assertEquals(12, response.fat)
        assertEquals(MealStatus.COMPLETED, response.status)
    }

    @Test
    fun `영양값이 null인 식사는 0으로 응답하고 합산에도 0으로 반영한다`() {
        val meals = listOf(
            meal(calory = null, carbs = null, protein = null, fat = null, mealAt = LocalDateTime.of(2026, 7, 12, 9, 0)),
            meal(calory = 500, carbs = 50, protein = 30, fat = 10, mealAt = LocalDateTime.of(2026, 7, 12, 12, 0)),
        )
        `when`(mealRepository.getMealsByUserIdAndPeriod(anyLong(), anyDateTime(), anyDateTime()))
            .thenReturn(meals)
        `when`(profileRepository.getProfileByUserId(1L)).thenReturn(null)

        val result = mealService.getDailyMeals(userId = 1L, year = 2026, month = 7, day = 12)

        assertEquals(0, result.meals.first().calories)
        assertEquals(Nutrition(calories = 500, carbs = 50, protein = 30, fat = 10), result.dailyNutrition.current)
    }

    @Test
    fun `조회 기간은 당일 하루 전체다`() {
        `when`(mealRepository.getMealsByUserIdAndPeriod(anyLong(), anyDateTime(), anyDateTime()))
            .thenReturn(emptyList())
        `when`(profileRepository.getProfileByUserId(1L)).thenReturn(null)

        mealService.getDailyMeals(userId = 1L, year = 2026, month = 7, day = 12)

        val captor = ArgumentCaptor.forClass(LocalDateTime::class.java)
        verify(mealRepository).getMealsByUserIdAndPeriod(eq(1L), capture(captor), capture(captor))
        val (start, end) = captor.allValues
        assertEquals(LocalDateTime.of(2026, 7, 12, 0, 0), start)
        assertTrue(!end.isBefore(LocalDateTime.of(2026, 7, 12, 23, 59, 59)), "종료 경계가 당일 전체를 포함해야 함: $end")
        assertTrue(end.isBefore(LocalDateTime.of(2026, 7, 13, 0, 0)), "종료 경계가 다음 날로 넘어가면 안 됨: $end")
    }

    @Test
    fun `프로필이 있으면 개인화된 권장량을 target으로 반환한다`() {
        // 남성 175cm/70kg, 2001-03-15생 → 조회일(2026-07-12) 기준 25세
        // BMR = 700 + 1093.75 - 125 + 5 = 1673.75, 권장 칼로리 = 1673.75 * 1.375 = 2301
        val profile = Profile(
            userId = 1L,
            birth = LocalDateTime.of(2001, 3, 15, 0, 0),
            gender = 0,
            height = 175,
            weight = 70,
        )
        `when`(mealRepository.getMealsByUserIdAndPeriod(anyLong(), anyDateTime(), anyDateTime()))
            .thenReturn(emptyList())
        `when`(profileRepository.getProfileByUserId(1L)).thenReturn(profile)

        val result = mealService.getDailyMeals(userId = 1L, year = 2026, month = 7, day = 12)

        assertEquals(Nutrition(calories = 2301, carbs = 287, protein = 115, fat = 76), result.dailyNutrition.target)
    }

    @Test
    fun `존재하지 않는 날짜면 예외가 발생한다`() {
        assertFailsWith<DateTimeException> {
            mealService.getDailyMeals(userId = 1L, year = 2026, month = 2, day = 30)
        }
    }

    // ---------- convertMealStatus ----------

    @Test
    fun `status 문자열을 MealStatus로 변환한다`() {
        assertEquals(MealStatus.COMPLETED, mealService.convertMealStatus("COMPLETED"))
        assertEquals(MealStatus.FAILED, mealService.convertMealStatus("FAILED"))
        assertEquals(MealStatus.ANALYSIS, mealService.convertMealStatus("ANALYSIS"))
    }

    @Test
    fun `알 수 없거나 null인 status는 UNKNOWN으로 변환한다`() {
        assertEquals(MealStatus.UNKNOWN, mealService.convertMealStatus(null))
        assertEquals(MealStatus.UNKNOWN, mealService.convertMealStatus("completed"))
        assertEquals(MealStatus.UNKNOWN, mealService.convertMealStatus("DELETED"))
    }
}
