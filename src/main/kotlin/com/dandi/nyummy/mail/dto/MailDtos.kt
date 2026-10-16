@file:Suppress("ktlint:standard:filename")

package com.dandi.nyummy.mail.dto

import com.dandi.nyummy.mail.enum.MailCategory
import java.time.Instant

data class MailsResponse(val mails: List<MailSummaryResponse>, val nextCursor: Long?, val hasNext: Boolean)

data class MailSummaryResponse(
    val mailId: Long,
    val category: MailCategory,
    val title: String,
    val content: String,
    val imageUrl: String,
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
