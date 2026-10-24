package com.dandi.nyummy.admin.service

import com.dandi.nyummy.admin.config.AdminProperties
import com.dandi.nyummy.admin.config.ConditionalOnAdminEnabled
import com.dandi.nyummy.admin.dto.AdminRoomItemResponse
import com.dandi.nyummy.admin.dto.AdminRoomItemsResponse
import com.dandi.nyummy.admin.enum.AdminImagePurpose
import com.dandi.nyummy.admin.mapper.toAdminRoomItemResponse
import com.dandi.nyummy.admin.validator.AdminImageValidator
import com.dandi.nyummy.exception.BusinessException
import com.dandi.nyummy.exception.errorcode.AdminErrorCode
import com.dandi.nyummy.exception.errorcode.S3ErrorCode
import com.dandi.nyummy.infra.storage.s3.S3StorageClient
import com.dandi.nyummy.room.enum.RoomItemCategory
import com.dandi.nyummy.room.repository.RoomItemRepository
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

/**
 * 어드민의 마이룸 아이템(상품) 관리. 상품 정의는 items, 마이룸 전용 속성(카테고리)은 room_items에 있다.
 *
 * 이미지는 `items/{itemId}.png`에 두고, 앱은 CDN 주소에 이 경로를 붙여 URL을 조립한다.
 */
@Service
@ConditionalOnAdminEnabled
class AdminRoomItemService(
    private val roomItemRepository: RoomItemRepository,
    private val adminRoomItemTransactionService: AdminRoomItemTransactionService,
    private val adminImageValidator: AdminImageValidator,
    private val s3StorageClient: S3StorageClient,
    private val adminProperties: AdminProperties,
) {

    companion object {
        private val logger = LoggerFactory.getLogger(AdminRoomItemService::class.java)

        private val IMAGE_PURPOSE = AdminImagePurpose.ROOM_ITEM
    }

    /**
     * 마이룸 아이템을 id 순서로 조회한다. [category]가 null이면 모든 카테고리를 조회한다.
     */
    @Transactional(readOnly = true)
    fun getRoomItems(category: RoomItemCategory?): AdminRoomItemsResponse = AdminRoomItemsResponse(
        roomItems = roomItemRepository.findRoomItems(category)
            .map { it.toAdminRoomItemResponse(createImageUrl(it.itemId)) },
    )

    /**
     * 마이룸 아이템(items + room_items)을 만들고 이미지를 `items/{itemId}.png`에 올린다.
     *
     * 행을 먼저 만들어 id를 받아야 객체 키가 정해진다. 업로드가 실패하면 만든 행을 지워 이미지 없는 아이템이
     * 남지 않게 한다. 새 키라 CDN 무효화는 필요 없다.
     *
     * 만들어도 사용자에게 바로 보이지는 않는다. 기본 지급 설정이나 상점·보상에 넣어야 사용자에게 간다.
     *
     * @param name 아이템 이름. 앞뒤 공백을 다듬어 저장하고 중복도 다듬은 값으로 검사한다
     * @param price null이면 상점에서 팔지 않는 아이템(보상 전용)
     * @throws BusinessException [S3ErrorCode.FILE_SIZE_EXCEEDED] 비었거나 5MB를 넘을 경우
     * @throws BusinessException [S3ErrorCode.UNSUPPORTED_CONTENT_TYPE] PNG가 아닐 경우
     * @throws BusinessException [AdminErrorCode.DUPLICATE_ROOM_ITEM_NAME] 같은 이름의 아이템이 이미 있을 경우
     * @throws BusinessException [S3ErrorCode.OBJECT_UPLOAD_FAILED] S3 업로드에 실패한 경우
     */
    fun createRoomItem(
        name: String,
        price: Int?,
        category: RoomItemCategory,
        image: MultipartFile,
    ): AdminRoomItemResponse {
        val bytes = image.bytes
        adminImageValidator.validateImage(bytes, IMAGE_PURPOSE.maxFileSizeBytes)

        val roomItem = adminRoomItemTransactionService.createRoomItem(name.trim(), price, category)

        try {
            runBlocking {
                s3StorageClient.uploadObject(
                    IMAGE_PURPOSE.createObjectKey(roomItem.itemId),
                    bytes,
                    AdminImagePurpose.CONTENT_TYPE,
                    AdminImagePurpose.CACHE_CONTROL,
                )
            }
        } catch (e: Exception) {
            deleteRoomItemAfterUploadFailure(roomItem.itemId)
            throw e
        }

        return roomItem.toAdminRoomItemResponse(createImageUrl(roomItem.itemId))
    }

    // 보상 삭제까지 실패하면 업로드 실패를 그대로 알리고 남은 행은 로그로 남긴다. 아무도 참조하지 않는 행이다.
    private fun deleteRoomItemAfterUploadFailure(itemId: Long) {
        try {
            adminRoomItemTransactionService.deleteRoomItem(itemId)
        } catch (e: Exception) {
            logger.error("이미지 업로드에 실패한 마이룸 아이템 행을 지우지 못했습니다: itemId={}", itemId, e)
        }
    }

    private fun createImageUrl(itemId: Long): String = IMAGE_PURPOSE.createImageUrl(adminProperties.cdnBaseUrl, itemId)
}
