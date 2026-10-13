package com.dandi.nyummy.home.mapper

import com.dandi.nyummy.user.dto.HomeUserResponse
import com.dandi.nyummy.user.entity.Profile

fun Profile.toHomeUserResponse() = HomeUserResponse(coin = this.coin)
