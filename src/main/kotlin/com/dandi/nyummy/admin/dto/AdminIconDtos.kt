@file:Suppress("ktlint:standard:filename")

package com.dandi.nyummy.admin.dto

data class AdminIconsResponse(val icons: List<AdminIconResponse>)

/**
 * @property imageUrl 앱과 같은 규칙(CDN 주소 + `/icons/{iconId}.png`)으로 만든 미리보기 URL
 */
data class AdminIconResponse(val iconId: Long, val name: String, val imageUrl: String)
