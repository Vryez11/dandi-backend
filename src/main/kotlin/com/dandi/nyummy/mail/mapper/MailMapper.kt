package com.dandi.nyummy.mail.mapper

import com.dandi.nyummy.mail.dto.MailSummaryResponse
import com.dandi.nyummy.mail.entity.Mail
import com.dandi.nyummy.mail.enum.MailCategory

// 코인 이미지는 지금 하나뿐이다. 결제에서 코인 이미지가 여럿이 되면 우편에 저장한 값으로 바꾼다.
private const val COIN_IMAGE_ID = 1L

fun Mail.toMailSummaryResponse(): MailSummaryResponse = MailSummaryResponse(
    mailId = this.id,
    category = this.category,
    title = this.title,
    content = this.content,
    imageId = when (this.category) {
        MailCategory.ITEM -> this.representativeItemId
        MailCategory.REWARD_BOX -> this.rewardBoxCategory?.id?.toLong()
        MailCategory.COIN -> COIN_IMAGE_ID
    },
    isClaimed = this.isClaimed,
    expiresAt = this.expiresAt,
    createdAt = this.createdAt,
)
