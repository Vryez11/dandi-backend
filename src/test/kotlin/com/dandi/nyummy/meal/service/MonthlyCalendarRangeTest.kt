package com.dandi.nyummy.meal.service

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlin.test.Test
import kotlin.test.assertEquals

class MonthlyCalendarRangeTest {

    @Test
    fun `수요일에 시작하는 달은 앞뒤 패딩을 포함한다`() {
        // 2026년 7월: 7/1(수) ~ 7/31(금)
        val range = MonthlyCalendarRange.calculate(YearMonth.of(2026, 7))

        assertEquals(LocalDate.of(2026, 6, 28), range.startDate) // 이전 일요일
        assertEquals(LocalDate.of(2026, 8, 1), range.endDate)    // 다음 토요일
    }

    @Test
    fun `일요일에 시작하고 토요일에 끝나는 달은 패딩이 없다`() {
        // 2026년 2월: 2/1(일) ~ 2/28(토)
        val range = MonthlyCalendarRange.calculate(YearMonth.of(2026, 2))

        assertEquals(LocalDate.of(2026, 2, 1), range.startDate)
        assertEquals(LocalDate.of(2026, 2, 28), range.endDate)
    }

    @Test
    fun `연 경계를 넘는 패딩을 처리한다`() {
        // 2026년 1월: 1/1(목) 시작 → 앞 패딩이 2025년 12월로 넘어감
        val range = MonthlyCalendarRange.calculate(YearMonth.of(2026, 1))

        assertEquals(LocalDate.of(2025, 12, 28), range.startDate)
        assertEquals(LocalDate.of(2026, 1, 31), range.endDate)
    }

    @Test
    fun `범위 일수는 항상 7의 배수다`() {
        // 일~토 꽉 찬 주 단위이므로 어떤 달이든 7의 배수여야 한다
        for (month in 1..12) {
            val range = MonthlyCalendarRange.calculate(YearMonth.of(2026, month))
            val days = ChronoUnit.DAYS.between(range.startDate, range.endDate) + 1

            assertEquals(0, (days % 7).toInt(), "2026년 ${month}월: ${days}일은 7의 배수가 아님")
        }
    }
}
