package com.dandi.nyummy.mail.mapper

import com.dandi.nyummy.mail.dto.MailSummaryResponse
import com.dandi.nyummy.mail.entity.Mail
import com.dandi.nyummy.mail.enum.MailCategory

fun Mail.toMailSummaryResponse(): MailSummaryResponse = MailSummaryResponse(
    mailId = this.id,
    category = this.category,
    title = this.title,
    content = this.content,
    imageId = when (this.category) {
        MailCategory.ITEM -> this.representativeItemId
        MailCategory.REWARD_BOX -> this.rewardBoxCategory?.id?.toLong()
        MailCategory.COIN -> null
    },
    isClaimed = this.isClaimed,
    expiresAt = this.expiresAt,
    createdAt = this.createdAt,
)
