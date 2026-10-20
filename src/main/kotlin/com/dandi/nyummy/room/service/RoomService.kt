package com.dandi.nyummy.room.service

import com.dandi.nyummy.exception.BusinessException
import com.dandi.nyummy.exception.errorcode.CommonErrorCode
import com.dandi.nyummy.room.config.RoomProperties
import com.dandi.nyummy.room.dto.EquippedRoomItemResponse
import com.dandi.nyummy.room.dto.OwnedRoomItemsResponse
import com.dandi.nyummy.room.dto.RoomItemCategoryResponse
import com.dandi.nyummy.room.dto.RoomResponse
import com.dandi.nyummy.room.dto.UpdateEquippedRoomItemsRequest
import com.dandi.nyummy.room.enum.RoomItemCategory
import com.dandi.nyummy.room.mapper.toEquippedRoomItemResponse
import com.dandi.nyummy.room.mapper.toOwnedRoomItem
import com.dandi.nyummy.room.mapper.toOwnedRoomItemResponse
import com.dandi.nyummy.room.repository.OwnedRoomItemRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RoomService(
    private val ownedRoomItemRepository: OwnedRoomItemRepository,
    private val roomProperties: RoomProperties,
) {

    /**
     * 가입한 사용자에게 기본 아이템을 장착한 채로 지급한다. 가입 트랜잭션 안에서 호출되어, 지급이 실패하면 가입도 실패한다.
     *
     * 아이템·좌표는 설정(app.room.default-room-items)에서 읽는다. 아이템 행(items·room_items)이 없는 DB에서는
     * 외래 키 제약에 걸려 가입이 실패하므로, 새 환경에는 기본 아이템 행을 먼저 넣는다.
     */
    @Transactional
    fun createDefaultOwnedRoomItems(userId: Long) {
        ownedRoomItemRepository.saveAll(roomProperties.defaultRoomItems.map { it.toOwnedRoomItem(userId) })
    }

    /**
     * 카테고리별 보유 아이템의 첫 묶음과 장착한 아이템을 조회한다.
     *
     * 카테고리는 보유 아이템이 없어도 enum 순서대로 모두 내려간다. 카테고리마다 [getOwnedRoomItems]와 같은 쿼리를
     * 따로 날린다. 앱이 카테고리를 오가며 다음 묶음을 받을 때도 같은 쿼리를 쓰므로, 전체를 한 번에 읽어 나누지 않는다.
     */
    @Transactional(readOnly = true)
    fun getRoom(userId: Long, size: Int): RoomResponse = RoomResponse(
        categories = RoomItemCategory.entries.map { category ->
            val ownedRoomItems = getOwnedRoomItems(userId, category, null, size)
            RoomItemCategoryResponse(
                category = category,
                categoryName = category.description,
                ownedRoomItems = ownedRoomItems.ownedRoomItems,
                nextCursor = ownedRoomItems.nextCursor,
                hasNext = ownedRoomItems.hasNext,
            )
        },
        equippedRoomItems = getEquippedRoomItems(userId),
    )

    /**
     * 한 카테고리의 보유 아이템을 장착한 것 먼저, 같은 장착 여부 안에서는 최신순으로 [size]개씩 조회한다.
     *
     * 정렬이 장착 여부와 id 두 값이라 [cursor]만으로는 위치를 알 수 없어, 커서 아이템을 먼저 읽어 장착 여부를 얻는다.
     * 스크롤 중에 지운 아이템도 정렬 위치는 남아 있으므로 삭제된 커서 아이템도 그대로 쓴다.
     * 커서 아이템이 없거나 다른 사용자의 것이면 처음부터 조회하지 않고 거부한다. 처음부터 주면 앱이 첫 묶음을
     * 뒤에 이어 붙여 중복이 생기고, 다른 사용자의 id가 있는지도 드러나지 않게 같은 응답으로 묶는다.
     *
     * @param cursor 직전 묶음의 마지막 ownedRoomItemId. null이면 처음부터 조회한다.
     * @throws BusinessException [CommonErrorCode.INVALID_INPUT_VALUE] 커서 아이템이 없거나 다른 사용자의 것인 경우
     */
    @Transactional(readOnly = true)
    fun getOwnedRoomItems(userId: Long, category: RoomItemCategory, cursor: Long?, size: Int): OwnedRoomItemsResponse {
        val cursorItem = cursor?.let {
            ownedRoomItemRepository.findOwnedRoomItemByIdAndUserId(it, userId)
                ?: throw BusinessException(CommonErrorCode.INVALID_INPUT_VALUE)
        }

        val ownedRoomItems = ownedRoomItemRepository.findOwnedRoomItemsByUserIdBeforeCursor(
            userId = userId,
            category = category,
            cursorIsEquipped = cursorItem?.isEquipped ?: true,
            cursorId = cursorItem?.id ?: Long.MAX_VALUE,
            pageable = PageRequest.of(0, size),
        )

        return OwnedRoomItemsResponse(
            ownedRoomItems = ownedRoomItems.content.map { it.toOwnedRoomItemResponse() },
            nextCursor = if (ownedRoomItems.hasNext()) ownedRoomItems.content.last().ownedRoomItemId else null,
            hasNext = ownedRoomItems.hasNext(),
        )
    }

    /**
     * 장착한 아이템을 그리는 순서(zIndex 오름차순)로 조회한다. 마이룸 조회와 홈이 함께 쓴다.
     */
    @Transactional(readOnly = true)
    fun getEquippedRoomItems(userId: Long): List<EquippedRoomItemResponse> =
        ownedRoomItemRepository.findEquippedRoomItemsByUserId(userId).map { it.toEquippedRoomItemResponse() }

    fun updateEquippedRoomItems(userId: Long, request: UpdateEquippedRoomItemsRequest) {
        // 해피케이스: 저장하지 않고 성공만 응답
    }

    fun deleteOwnedRoomItem(userId: Long, ownedRoomItemId: Long) {
        // 해피케이스: 저장하지 않고 성공만 응답
    }
}
