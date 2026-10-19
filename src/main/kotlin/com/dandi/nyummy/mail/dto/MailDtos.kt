@file:Suppress("ktlint:standard:filename")

package com.dandi.nyummy.mail.dto

import com.dandi.nyummy.mail.enum.MailCategory
import java.time.Instant

data class MailsResponse(val mails: List<MailSummaryResponse>, val nextCursor: Long?, val hasNext: Boolean)

/**
 * 우편 목록의 한 건. 이미지 URL은 내려주지 않고, 앱이 [category]로 경로를 고른 뒤 [imageId]를 붙여 CDN URL을 조립한다.
 *
 * @property imageId ITEM은 대표 아이템 id(`items/{imageId}.png`), REWARD_BOX는 상자 번호(`reward-boxes/{imageId}.png`),
 * COIN은 코인 이미지 번호(`coins/{imageId}.png`)
 */
data class MailSummaryResponse(
    val mailId: Long,
    val category: MailCategory,
    val title: String,
    val content: String,
    val imageId: Long?,
    val isClaimed: Boolean,
    val expiresAt: Instant?,
    val createdAt: Instant,
)

data class RewardResponse(
    val coin: RewardCoinResponse?,
    val items: List<RewardItemResponse>?,
    val rewardBox: RewardBoxResponse?,
)

data class TotalRewardResponse(val coin: RewardCoinResponse?, val items: List<RewardItemResponse>?)

data class RewardCoinResponse(val quantity: Int, val imageUrl: String)

data class RewardItemResponse(val itemId: Long, val name: String, val imageUrl: String, val quantity: Int)

data class RewardBoxResponse(val imageUrl: String, val animationUrl: String?)
