package com.dandi.nyummy.admin.service

import com.dandi.nyummy.admin.config.AdminProperties
import com.dandi.nyummy.admin.config.ConditionalOnAdminEnabled
import com.dandi.nyummy.admin.dto.AdminRoomItemResponse
import com.dandi.nyummy.admin.dto.AdminRoomItemsResponse
import com.dandi.nyummy.admin.dto.UpdateRoomItemRequest
import com.dandi.nyummy.admin.enum.AdminImagePurpose
import com.dandi.nyummy.admin.mapper.toAdminRoomItemResponse
import com.dandi.nyummy.admin.validator.AdminImageValidator
import com.dandi.nyummy.exception.BusinessException
import com.dandi.nyummy.exception.errorcode.AdminErrorCode
import com.dandi.nyummy.exception.errorcode.CdnErrorCode
import com.dandi.nyummy.exception.errorcode.S3ErrorCode
import com.dandi.nyummy.infra.cdn.CdnClient
import com.dandi.nyummy.infra.storage.s3.S3StorageClient
import com.dandi.nyummy.item.entity.Item
import com.dandi.nyummy.item.repository.ItemRepository
import com.dandi.nyummy.room.entity.RoomItem
import com.dandi.nyummy.room.enum.RoomItemCategory
import com.dandi.nyummy.room.repository.RoomItemRepository
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
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
    private val itemRepository: ItemRepository,
    private val roomItemRepository: RoomItemRepository,
    private val adminRoomItemTransactionService: AdminRoomItemTransactionService,
    private val adminImageValidator: AdminImageValidator,
    private val s3StorageClient: S3StorageClient,
    private val cdnClient: CdnClient,
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

    /**
     * 마이룸 아이템의 이름과 가격을 받은 값으로 통째로 바꾼다(PUT). price가 null이면 비매품이 된다.
     * 카테고리는 이미 가진 사용자의 방 배치가 깨질 수 있어 바꾸지 않는다.
     *
     * 이름은 보유 아이템 목록·우편 보상(받은 우편 포함)에서 items를 읽어 보여 주므로 사용자에게 바로 바뀐다.
     *
     * @throws BusinessException [AdminErrorCode.ROOM_ITEM_NOT_FOUND] 아이템이 없거나 마이룸 아이템이 아닐 경우
     * @throws BusinessException [AdminErrorCode.DUPLICATE_ROOM_ITEM_NAME] 다른 아이템이 같은 이름을 쓸 경우.
     *   자기 이름 그대로는 허용한다
     */
    @Transactional
    fun updateRoomItem(itemId: Long, request: UpdateRoomItemRequest): AdminRoomItemResponse {
        val (roomItem, item) = getRoomItemAndItem(itemId)

        val name = request.name.trim()
        if (itemRepository.existsByNameAndIdNot(name, itemId)) {
            throw BusinessException(AdminErrorCode.DUPLICATE_ROOM_ITEM_NAME)
        }

        item.updateName(name)
        item.updatePrice(request.price)

        return item.toAdminRoomItemResponse(roomItem.category, createImageUrl(itemId))
    }

    /**
     * 마이룸 아이템 이미지를 바꾼다. 같은 키(`items/{itemId}.png`)에 덮어쓰고 CDN 캐시를 버린다.
     *
     * DB는 바뀌지 않는다. 옛 이미지는 버킷 버전 관리로 이전 버전에 남는다. CDN은 무효화가 엣지에 반영되면
     * (보통 수 초~수 분) 새 이미지를 내려주지만, 이미 받아 둔 기기는 Cache-Control max-age(1시간)가 지나야 다시 받는다.
     * 기본 지급 아이템(예: 1번 배경)을 바꾸면 그 아이템을 가진 모든 사용자의 방이 바뀐다.
     *
     * 무효화가 실패하면 S3는 이미 바뀐 상태다. 같은 파일로 다시 부르면 덮어쓰기와 무효화를 다시 하므로 안전하다.
     *
     * @throws BusinessException [AdminErrorCode.ROOM_ITEM_NOT_FOUND] 아이템이 없거나 마이룸 아이템이 아닐 경우
     * @throws BusinessException [S3ErrorCode.FILE_SIZE_EXCEEDED] 비었거나 5MB를 넘을 경우
     * @throws BusinessException [S3ErrorCode.UNSUPPORTED_CONTENT_TYPE] PNG가 아닐 경우
     * @throws BusinessException [S3ErrorCode.OBJECT_UPLOAD_FAILED] S3 업로드에 실패한 경우(아무것도 바뀌지 않음)
     * @throws BusinessException [CdnErrorCode.INVALIDATION_FAILED] CDN 무효화에 실패한 경우(S3는 바뀜)
     */
    fun updateRoomItemImage(itemId: Long, image: MultipartFile): AdminRoomItemResponse {
        val (roomItem, item) = getRoomItemAndItem(itemId)

        val bytes = image.bytes
        adminImageValidator.validateImage(bytes, IMAGE_PURPOSE.maxFileSizeBytes)

        val objectKey = IMAGE_PURPOSE.createObjectKey(itemId)
        runBlocking {
            s3StorageClient.uploadObject(
                objectKey,
                bytes,
                AdminImagePurpose.CONTENT_TYPE,
                AdminImagePurpose.CACHE_CONTROL,
            )
            cdnClient.createInvalidation(listOf("/$objectKey"))
        }

        return item.toAdminRoomItemResponse(roomItem.category, createImageUrl(itemId))
    }

    // room_items와 items가 둘 다 있어야 마이룸 아이템이다. items만 있으면 다른 종류의 아이템이므로 없는 것으로 본다.
    private fun getRoomItemAndItem(itemId: Long): Pair<RoomItem, Item> {
        val roomItem = roomItemRepository.findByIdOrNull(itemId)
            ?: throw BusinessException(AdminErrorCode.ROOM_ITEM_NOT_FOUND)
        val item = itemRepository.findByIdOrNull(itemId)
            ?: throw BusinessException(AdminErrorCode.ROOM_ITEM_NOT_FOUND)

        return roomItem to item
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
