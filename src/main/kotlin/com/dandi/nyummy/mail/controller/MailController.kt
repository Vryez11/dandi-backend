package com.dandi.nyummy.mail.controller

import com.dandi.nyummy.mail.dto.MailSummaryResponse
import com.dandi.nyummy.mail.dto.MailsResponse
import com.dandi.nyummy.mail.enum.MailCategory
import com.dandi.nyummy.security.AuthUser
import com.dandi.nyummy.security.CurrentUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@Tag(name = "Mail", description = "우편 조회 · 받기 · 삭제 · 복구 API")
@RestController
@RequestMapping("/api/v1/mails")
class MailController {

    @Operation(
        summary = "우편 목록 조회",
        description = "우편 목록을 최신순으로 조회한다. 만료된 우편은 포함하지 않는다. 무한 스크롤용 cursor 방식이다. " +
            "첫 요청은 cursor 없이 보내고, 다음 요청부터는 직전 응답의 nextCursor를 cursor로 보낸다. " +
            "hasNext가 false면 마지막 묶음이며 nextCursor는 null이다. " +
            "expiresAt이 null이면 만료되지 않는 우편이다. 현재는 요청과 관계없이 고정 응답을 반환한다.",
    )
    @GetMapping
    fun getMails(
        @CurrentUser user: AuthUser,
        @Parameter(description = "직전 응답의 nextCursor. 첫 요청에서는 생략한다.") @RequestParam(required = false) cursor: Long?,
        @Parameter(description = "한 번에 조회할 개수 (1~50, 기본 10)")
        @RequestParam(defaultValue = "10")
        @Min(1)
        @Max(50)
        size: Int,
    ): MailsResponse = MailsResponse(
        mails = listOf(
            MailSummaryResponse(
                mailId = 4,
                category = MailCategory.REWARD_BOX,
                title = "보상 상자",
                content = "7일 연속 식사 기록 보상",
                imageUrl = "https://example.com/images/mail/reward-box.png",
                isClaimed = false,
                expiresAt = Instant.parse("2099-12-31T00:00:00Z"),
                createdAt = Instant.parse("2026-10-16T08:00:00Z"),
            ),
            MailSummaryResponse(
                mailId = 3,
                category = MailCategory.COIN,
                title = "코인 100개",
                content = "오늘의 식사 기록 보상",
                imageUrl = "https://example.com/images/mail/coin.png",
                isClaimed = false,
                expiresAt = Instant.parse("2099-12-31T00:00:00Z"),
                createdAt = Instant.parse("2026-10-16T04:00:00Z"),
            ),
            MailSummaryResponse(
                mailId = 2,
                category = MailCategory.ITEM,
                title = "생선 간식",
                content = "주간 퀘스트 달성 보상",
                imageUrl = "https://example.com/images/mail/item.png",
                isClaimed = false,
                expiresAt = null,
                createdAt = Instant.parse("2026-10-15T09:00:00Z"),
            ),
            MailSummaryResponse(
                mailId = 1,
                category = MailCategory.COIN,
                title = "코인 50개",
                content = "첫 식사 기록 보상",
                imageUrl = "https://example.com/images/mail/coin.png",
                isClaimed = true,
                expiresAt = null,
                createdAt = Instant.parse("2026-10-14T09:00:00Z"),
            ),
        ),
        nextCursor = null,
        hasNext = false,
    )
}
