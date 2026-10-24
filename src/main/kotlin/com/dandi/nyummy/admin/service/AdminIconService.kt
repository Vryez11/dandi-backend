package com.dandi.nyummy.admin.service

import com.dandi.nyummy.admin.config.AdminProperties
import com.dandi.nyummy.admin.config.ConditionalOnAdminEnabled
import com.dandi.nyummy.admin.dto.AdminIconsResponse
import com.dandi.nyummy.admin.mapper.toAdminIconResponse
import com.dandi.nyummy.meal.repository.IconRepository
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 어드민의 음식 아이콘 관리.
 *
 * 이미지는 `icons/{iconId}.png`에 두고, 앱은 CDN 주소에 이 경로를 붙여 URL을 조립한다.
 * 어드민 미리보기도 같은 규칙으로 만들어 어드민에서 보이는 그림이 앱에서 보이는 그림과 같게 한다.
 */
@Service
@ConditionalOnAdminEnabled
class AdminIconService(private val iconRepository: IconRepository, private val adminProperties: AdminProperties) {

    companion object {
        private const val OBJECT_KEY_PREFIX = "icons"
    }

    /**
     * 음식 아이콘 전체를 id 순서로 조회한다.
     *
     * Gemini 프롬프트용 [com.dandi.nyummy.meal.service.IconService.getIcons]는 캐시를 거치므로 쓰지 않는다.
     * 어드민에서는 방금 만든 음식 아이콘도 바로 보여야 한다.
     */
    @Transactional(readOnly = true)
    fun getIcons(): AdminIconsResponse = AdminIconsResponse(
        icons = iconRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))
            .map { it.toAdminIconResponse(createImageUrl(it.id)) },
    )

    private fun createObjectKey(iconId: Long): String = "$OBJECT_KEY_PREFIX/$iconId.png"

    private fun createImageUrl(iconId: Long): String = "${adminProperties.cdnBaseUrl}/${createObjectKey(iconId)}"
}
