package com.dandi.nyummy.admin.service

import com.dandi.nyummy.admin.config.AdminProperties
import com.dandi.nyummy.admin.config.ConditionalOnAdminEnabled
import com.dandi.nyummy.admin.dto.AdminIconResponse
import com.dandi.nyummy.admin.dto.AdminIconsResponse
import com.dandi.nyummy.admin.mapper.toAdminIconResponse
import com.dandi.nyummy.admin.validator.AdminImageValidator
import com.dandi.nyummy.exception.BusinessException
import com.dandi.nyummy.exception.errorcode.AdminErrorCode
import com.dandi.nyummy.exception.errorcode.S3ErrorCode
import com.dandi.nyummy.infra.storage.s3.S3StorageClient
import com.dandi.nyummy.meal.repository.IconRepository
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

/**
 * 어드민의 음식 아이콘 관리.
 *
 * 이미지는 `icons/{iconId}.png`에 두고, 앱은 CDN 주소에 이 경로를 붙여 URL을 조립한다.
 * 어드민 미리보기도 같은 규칙으로 만들어 어드민에서 보이는 그림이 앱에서 보이는 그림과 같게 한다.
 */
@Service
@ConditionalOnAdminEnabled
class AdminIconService(
    private val iconRepository: IconRepository,
    private val adminIconTransactionService: AdminIconTransactionService,
    private val adminImageValidator: AdminImageValidator,
    private val s3StorageClient: S3StorageClient,
    private val adminProperties: AdminProperties,
) {

    companion object {
        private val logger = LoggerFactory.getLogger(AdminIconService::class.java)

        private const val OBJECT_KEY_PREFIX = "icons"
        private const val MAX_FILE_SIZE_BYTES = 512 * 1024L
        private const val IMAGE_CONTENT_TYPE = "image/png"

        // CDN은 길게 들고 있다가 이미지를 바꿀 때만 무효화한다. 무효화가 닿지 않는 기기 캐시는 1시간으로 짧게 둔다.
        private const val CACHE_CONTROL = "public, max-age=3600, s-maxage=31536000"
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

    /**
     * 음식 아이콘 행을 만들고 이미지를 `icons/{iconId}.png`에 올린다.
     *
     * 행을 먼저 만들어 id를 받아야 객체 키가 정해진다. 업로드가 실패하면 만든 행을 지워 이미지 없는 음식 아이콘이
     * 남지 않게 한다. 새 키라 CDN 무효화는 필요 없다(첫 요청 때 CloudFront가 S3에서 가져간다).
     *
     * Gemini 프롬프트에는 [com.dandi.nyummy.meal.service.IconService]의 캐시가 만료된 뒤(최대 1시간) 들어간다.
     *
     * @param name 음식 아이콘 이름. 앞뒤 공백을 다듬어 저장하고 중복도 다듬은 값으로 검사한다
     * @throws BusinessException [S3ErrorCode.FILE_SIZE_EXCEEDED] 비었거나 512KB를 넘을 경우
     * @throws BusinessException [S3ErrorCode.UNSUPPORTED_CONTENT_TYPE] PNG가 아닐 경우
     * @throws BusinessException [AdminErrorCode.DUPLICATE_ICON_NAME] 같은 이름이 이미 있을 경우
     * @throws BusinessException [S3ErrorCode.OBJECT_UPLOAD_FAILED] S3 업로드에 실패한 경우
     */
    fun createIcon(name: String, image: MultipartFile): AdminIconResponse {
        val bytes = image.bytes
        adminImageValidator.validateImage(bytes, MAX_FILE_SIZE_BYTES)

        val icon = adminIconTransactionService.createIcon(name.trim(), ::createImageUrl)

        try {
            runBlocking {
                s3StorageClient.uploadObject(createObjectKey(icon.id), bytes, IMAGE_CONTENT_TYPE, CACHE_CONTROL)
            }
        } catch (e: Exception) {
            deleteIconAfterUploadFailure(icon.id)
            throw e
        }

        return icon.toAdminIconResponse(icon.imageUrl)
    }

    // 보상 삭제까지 실패하면 업로드 실패를 그대로 알리고 남은 행은 로그로 남긴다. 아무도 참조하지 않는 행이다.
    private fun deleteIconAfterUploadFailure(iconId: Long) {
        try {
            adminIconTransactionService.deleteIcon(iconId)
        } catch (e: Exception) {
            logger.error("이미지 업로드에 실패한 음식 아이콘 행을 지우지 못했습니다: iconId={}", iconId, e)
        }
    }

    private fun createObjectKey(iconId: Long): String = "$OBJECT_KEY_PREFIX/$iconId.png"

    private fun createImageUrl(iconId: Long): String = "${adminProperties.cdnBaseUrl}/${createObjectKey(iconId)}"
}
