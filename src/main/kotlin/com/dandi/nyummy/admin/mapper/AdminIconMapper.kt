package com.dandi.nyummy.admin.mapper

import com.dandi.nyummy.admin.dto.AdminIconResponse
import com.dandi.nyummy.meal.entity.Icon

fun Icon.toAdminIconResponse(imageUrl: String): AdminIconResponse = AdminIconResponse(
    iconId = this.id,
    name = this.name,
    imageUrl = imageUrl,
)
