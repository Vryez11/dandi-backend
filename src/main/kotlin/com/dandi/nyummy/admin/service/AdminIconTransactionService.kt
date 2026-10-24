package com.dandi.nyummy.admin.service

import com.dandi.nyummy.admin.config.ConditionalOnAdminEnabled
import com.dandi.nyummy.exception.BusinessException
import com.dandi.nyummy.exception.errorcode.AdminErrorCode
import com.dandi.nyummy.meal.entity.Icon
import com.dandi.nyummy.meal.repository.IconRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 음식 아이콘의 DB 변경만 짧은 트랜잭션으로 묶는다. S3 업로드는 [AdminIconService]가 트랜잭션 밖에서 한다.
 */
@Service
@ConditionalOnAdminEnabled
class AdminIconTransactionService(private val iconRepository: IconRepository) {

    /**
     * 음식 아이콘 행을 만든다. 이미지 URL에 id가 들어가므로 저장해 id를 받은 뒤 같은 트랜잭션에서 URL을 채운다.
     *
     * @param name 다듬은 이름. 같은 이름이 있으면 거절한다(Gemini가 이름을 보고 음식 아이콘을 고른다)
     * @param createImageUrl id로 이미지 URL을 만드는 규칙. 앱과 같은 규칙을 한 곳에만 두려고 받는다
     * @throws BusinessException [AdminErrorCode.DUPLICATE_ICON_NAME] 같은 이름이 이미 있을 경우
     */
    @Transactional
    fun createIcon(name: String, createImageUrl: (Long) -> String): Icon {
        if (iconRepository.existsByName(name)) {
            throw BusinessException(AdminErrorCode.DUPLICATE_ICON_NAME)
        }

        // IDENTITY라 save 시점에 INSERT가 나가 id가 정해진다. URL은 커밋 때 UPDATE로 반영된다.
        val icon = iconRepository.save(Icon(name = name, imageUrl = ""))
        icon.updateImageUrl(createImageUrl(icon.id))

        return icon
    }

    /**
     * 이미지 업로드에 실패한 음식 아이콘 행을 지운다. 방금 만들어 아무도 참조하지 않으므로 물리 삭제한다.
     * 어드민 API로 여는 삭제가 아니라 [AdminIconService.createIcon]의 실패 보상용이다.
     */
    @Transactional
    fun deleteIcon(iconId: Long) {
        iconRepository.deleteById(iconId)
    }
}
