package com.dandi.nyummy.meal.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

data class MonthlyCalendarRange(
    val startDate: LocalDate,
    val endDate: LocalDate,
) {
    companion object {
        fun calculate(yearMonth: YearMonth): MonthlyCalendarRange {

            val startDay = yearMonth.atDay(1)
            val endDay = yearMonth.atEndOfMonth()

            val startWith = startDay.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
            val endWith = endDay.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))

            return MonthlyCalendarRange(
                startDate = startWith,
                endDate = endWith,
            )
        }
    }
}