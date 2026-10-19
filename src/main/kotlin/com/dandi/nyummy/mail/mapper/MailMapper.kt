package com.dandi.nyummy.mail.mapper

import com.dandi.nyummy.mail.dto.MailSummaryResponse
import com.dandi.nyummy.mail.entity.Mail

// 대표 아이템과 상자 등급은 해당 종류의 우편에만 채워지므로 category로 분기하지 않고 그대로 옮긴다.
fun Mail.toMailSummaryResponse(): MailSummaryResponse = MailSummaryResponse(
    mailId = this.id,
    category = this.category,
    title = this.title,
    content = this.content,
    itemId = this.representativeItemId,
    rewardBoxId = this.rewardBoxCategory?.id,
    isClaimed = this.isClaimed,
    expiresAt = this.expiresAt,
    createdAt = this.createdAt,
)
