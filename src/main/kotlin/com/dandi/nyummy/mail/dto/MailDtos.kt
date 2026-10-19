@file:Suppress("ktlint:standard:filename")

package com.dandi.nyummy.mail.dto

import com.dandi.nyummy.mail.enum.MailCategory
import java.time.Instant

data class MailsResponse(val mails: List<MailSummaryResponse>, val nextCursor: Long?, val hasNext: Boolean)

/**
 * 우편 목록의 한 건. 이미지 URL은 내려주지 않고, 앱이 아래 id로 CDN 경로를 조립한다.
 *
 * @property itemId ITEM 우편의 대표 아이템 id(`items/{itemId}.png`). 다른 종류는 null
 * @property rewardBoxId REWARD_BOX 우편의 상자 번호(`reward-boxes/{rewardBoxId}.png`). 다른 종류는 null
 */
data class MailSummaryResponse(
    val mailId: Long,
    val category: MailCategory,
    val title: String,
    val content: String,
    val itemId: Long?,
    val rewardBoxId: Int?,
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
