package com.dandi.nyummy.mail.service

import com.dandi.nyummy.mail.dto.MailsResponse
import com.dandi.nyummy.mail.entity.Mail
import com.dandi.nyummy.mail.enum.MailCategory
import com.dandi.nyummy.mail.enum.RewardBoxCategory
import com.dandi.nyummy.mail.mapper.toMailSummaryResponse
import com.dandi.nyummy.mail.repository.MailRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant

@Service
class MailService(private val mailRepository: MailRepository, private val clock: Clock) {

    /**
     * 보상 상자 우편을 만든다. 제목은 등급 이름이다.
     *
     * 무엇을·얼마나·언제 줄지는 호출하는 쪽이 정하고, 여기서는 우편함에 넣기만 한다.
     * 트랜잭션은 호출하는 쪽이 연다. 저장이 save 한 번뿐이라 혼자 불려도 save가 스스로 커밋한다.
     *
     * @param userId 우편을 받을 사용자 ID
     * @param rewardBoxCategory 상자 등급
     * @param coinQuantity 상자에 든 코인 수
     * @param content 보상을 얻은 경로 문구
     * @param expiresAt 우편 만료 시각
     * @return 저장된 [Mail]
     */
    fun createRewardBoxMail(
        userId: Long,
        rewardBoxCategory: RewardBoxCategory,
        coinQuantity: Int,
        content: String,
        expiresAt: Instant,
    ): Mail = mailRepository.save(
        Mail(
            userId = userId,
            category = MailCategory.REWARD_BOX,
            title = rewardBoxCategory.description,
            content = content,
            coinQuantity = coinQuantity,
            rewardBoxCategory = rewardBoxCategory,
            expiresAt = expiresAt,
        ),
    )

    /**
     * 사용자의 우편 목록을 최신순으로 [size]개씩 조회한다. 무한 스크롤용 cursor 방식이다.
     *
     * 첫 요청은 [cursor] 없이 보내고, 다음 요청부터는 직전 응답의 nextCursor(마지막 우편 id)를 보낸다.
     * 만료된 우편은 받은 우편이어도 보여주지 않는다. 만료는 조회하는 시각을 기준으로 판단한다.
     *
     * @param cursor 직전 묶음의 마지막 우편 id. null이면 가장 최신 우편부터 조회한다.
     */
    @Transactional(readOnly = true)
    fun getMails(userId: Long, cursor: Long?, size: Int): MailsResponse {
        val mails = mailRepository.findMailsByUserIdBeforeCursor(
            userId = userId,
            cursor = cursor ?: Long.MAX_VALUE,
            now = Instant.now(clock),
            pageable = PageRequest.of(0, size),
        )

        return MailsResponse(
            mails = mails.content.map { it.toMailSummaryResponse() },
            nextCursor = if (mails.hasNext()) mails.content.last().id else null,
            hasNext = mails.hasNext(),
        )
    }
}
