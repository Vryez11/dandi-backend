package com.dandi.nyummy.meal.calculator

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

fun MonthlyCalendarRangeCalculate(yearMonth: YearMonth): List<LocalDate> {

    val startDay = yearMonth.atDay(1)
    val endDay = yearMonth.atEndOfMonth()

    val startWith = startDay.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
    val endWith = endDay.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))

    return listOf(startWith, endWith)
}