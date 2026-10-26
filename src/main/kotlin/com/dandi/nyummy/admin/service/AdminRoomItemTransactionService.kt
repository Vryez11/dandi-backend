package com.dandi.nyummy.admin.service

import com.dandi.nyummy.admin.config.ConditionalOnAdminEnabled
import com.dandi.nyummy.exception.BusinessException
import com.dandi.nyummy.exception.errorcode.AdminErrorCode
import com.dandi.nyummy.item.entity.Item
import com.dandi.nyummy.item.repository.ItemRepository
import com.dandi.nyummy.room.dto.RoomItemResult
import com.dandi.nyummy.room.entity.RoomItem
import com.dandi.nyummy.room.enum.RoomItemCategory
import com.dandi.nyummy.room.repository.RoomItemRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 마이룸 아이템의 DB 변경만 짧은 트랜잭션으로 묶는다. S3 업로드는 [AdminRoomItemService]가 트랜잭션 밖에서 한다.
 */
@Service
@ConditionalOnAdminEnabled
class AdminRoomItemTransactionService(
    private val itemRepository: ItemRepository,
    private val roomItemRepository: RoomItemRepository,
) {

    /**
     * 상품 정의(items)와 마이룸 전용 속성(room_items)을 함께 만든다. room_items는 items와 같은 id를 쓴다.
     *
     * @param name 다듬은 이름. 앱에 보이는 이름이라 같은 이름의 아이템이 있으면 거절한다
     *   (items 전체에서 검사하므로 이후 다른 종류의 아이템과도 겹칠 수 없다)
     * @param price null이면 상점에서 팔지 않는 아이템(보상 전용)
     * @throws BusinessException [AdminErrorCode.DUPLICATE_ROOM_ITEM_NAME] 같은 이름이 이미 있을 경우
     */
    @Transactional
    fun createRoomItem(name: String, price: Int?, category: RoomItemCategory): RoomItemResult {
        if (itemRepository.existsByName(name)) {
            throw BusinessException(AdminErrorCode.DUPLICATE_ROOM_ITEM_NAME)
        }

        val item = itemRepository.save(Item(name = name, price = price))
        val roomItem = roomItemRepository.save(RoomItem(itemId = item.id, category = category))

        return RoomItemResult(
            itemId = item.id,
            name = item.name,
            price = item.price,
            category = roomItem.category,
            createdAt = item.createdAt,
        )
    }

    /**
     * 이미지 업로드에 실패한 마이룸 아이템 행을 지운다. 방금 만들어 아무도 참조하지 않으므로 물리 삭제한다.
     * room_items가 items를 FK로 가리키므로 room_items부터 지운다.
     * 어드민 API로 여는 삭제가 아니라 [AdminRoomItemService.createRoomItem]의 실패 보상용이다.
     */
    @Transactional
    fun deleteRoomItem(itemId: Long) {
        roomItemRepository.deleteById(itemId)
        itemRepository.deleteById(itemId)
    }
}
