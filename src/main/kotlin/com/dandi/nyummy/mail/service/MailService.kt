package com.dandi.nyummy.mail.service

import com.dandi.nyummy.mail.entity.Mail
import com.dandi.nyummy.mail.enum.MailCategory
import com.dandi.nyummy.mail.enum.RewardBoxCategory
import com.dandi.nyummy.mail.repository.MailRepository
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class MailService(private val mailRepository: MailRepository) {

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
}
