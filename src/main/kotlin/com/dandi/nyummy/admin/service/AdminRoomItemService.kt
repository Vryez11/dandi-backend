package com.dandi.nyummy.admin.service

import com.dandi.nyummy.admin.config.AdminProperties
import com.dandi.nyummy.admin.config.ConditionalOnAdminEnabled
import com.dandi.nyummy.admin.dto.AdminRoomItemsResponse
import com.dandi.nyummy.admin.enum.AdminImagePurpose
import com.dandi.nyummy.admin.mapper.toAdminRoomItemResponse
import com.dandi.nyummy.room.enum.RoomItemCategory
import com.dandi.nyummy.room.repository.RoomItemRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 어드민의 마이룸 아이템(상품) 관리. 상품 정의는 items, 마이룸 전용 속성(카테고리)은 room_items에 있다.
 *
 * 이미지는 `items/{itemId}.png`에 두고, 앱은 CDN 주소에 이 경로를 붙여 URL을 조립한다.
 */
@Service
@ConditionalOnAdminEnabled
class AdminRoomItemService(
    private val roomItemRepository: RoomItemRepository,
    private val adminProperties: AdminProperties,
) {

    companion object {
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

    private fun createImageUrl(itemId: Long): String = IMAGE_PURPOSE.createImageUrl(adminProperties.cdnBaseUrl, itemId)
}
