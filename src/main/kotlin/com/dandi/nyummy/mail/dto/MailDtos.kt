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
