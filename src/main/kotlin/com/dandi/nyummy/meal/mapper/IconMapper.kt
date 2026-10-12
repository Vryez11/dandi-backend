package com.dandi.nyummy.meal.mapper

import com.dandi.nyummy.meal.dto.IconSummaryResult
import com.dandi.nyummy.meal.entity.Icon

fun Icon.toIconSummaryResult(): IconSummaryResult = IconSummaryResult(
    id = id,
    name = name,
)
