@file:Suppress("ktlint:standard:filename")

package com.dandi.nyummy.admin.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UpdateIconNameRequest(
    @field:NotBlank(message = "음식 아이콘 이름은 필수입니다.")
    @field:Size(max = 100, message = "음식 아이콘 이름은 100자 이하여야 합니다.")
    val name: String,
)

data class AdminIconsResponse(val icons: List<AdminIconResponse>)

/**
 * @property imageUrl 앱과 같은 규칙(CDN 주소 + `/icons/{iconId}.png`)으로 만든 미리보기 URL
 */
data class AdminIconResponse(val iconId: Long, val name: String, val imageUrl: String)
