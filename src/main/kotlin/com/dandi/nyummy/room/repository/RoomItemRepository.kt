package com.dandi.nyummy.room.repository

import com.dandi.nyummy.room.dto.RoomItemResult
import com.dandi.nyummy.room.entity.RoomItem
import com.dandi.nyummy.room.enum.RoomItemCategory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface RoomItemRepository : JpaRepository<RoomItem, Long> {

    /**
     * 마이룸 아이템(상품)을 id 순서로 조회한다. [category]가 null이면 모든 카테고리를 조회한다.
     */
    @Query(
        """
        select new com.dandi.nyummy.room.dto.RoomItemResult(r.itemId, i.name, i.price, r.category, i.createdAt)
        from RoomItem as r
            join Item as i on i.id = r.itemId
        where (:category is null or r.category = :category)
        order by r.itemId asc
    """,
    )
    fun findRoomItems(@Param("category") category: RoomItemCategory?): List<RoomItemResult>
}
