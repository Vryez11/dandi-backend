package com.dandi.nyummy.e2e.room

import com.dandi.nyummy.e2e.support.RequestSpecs
import com.dandi.nyummy.e2e.support.Smoke
import io.restassured.module.kotlin.extensions.Extract
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.hamcrest.Matchers.empty
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasItems
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class RoomE2eTest {
    // 조회만 하므로 prod에서도 돌린다. 모든 사용자는 가입할 때(기존 사용자는 채우기 SQL로) 기본 아이템을
    // 장착한 채로 받으므로 고정 계정에도 배경과 장착 아이템이 있다.
    @Smoke
    @Test
    fun `마이룸을 조회하면 모든 카테고리가 고정 순서로 오고 기본 아이템이 장착돼 있다`() {
        val zIndexes: List<Int> = Given {
            spec(RequestSpecs.createAuthorizedSpec())
        } When {
            get("/api/v1/rooms")
        } Then {
            statusCode(200)
            body("keySet()", hasItems("categories", "equippedRoomItems"))
            // 앱은 이 순서대로 탭을 그린다. 카테고리가 바뀌거나 순서가 달라지면 화면이 어긋난다.
            body("categories.category", equalTo(ROOM_ITEM_CATEGORIES))
            body(
                "categories[0].keySet()",
                hasItems("category", "categoryName", "ownedRoomItems", "nextCursor", "hasNext"),
            )
            body(
                "categories.find { it.category == 'BACKGROUND' }.ownedRoomItems[0].keySet()",
                hasItems("ownedRoomItemId", "name", "itemId", "isEquipped"),
            )
            body("equippedRoomItems", not(empty<Any>()))
            body(
                "equippedRoomItems[0].keySet()",
                hasItems("ownedRoomItemId", "category", "itemId", "x", "y", "zIndex", "scale"),
            )
        } Extract {
            path("equippedRoomItems.zIndex")
        }

        // 앱은 받은 순서대로 겹쳐 그리므로 zIndex 오름차순이어야 배경이 맨 뒤에 깔린다.
        assertEquals(zIndexes.sorted(), zIndexes, "장착 아이템이 zIndex 오름차순이 아닙니다: $zIndexes")
    }

    @Smoke
    @Test
    fun `한 카테고리의 보유 아이템 목록을 조회한다`() {
        Given {
            spec(RequestSpecs.createAuthorizedSpec())
            queryParam("category", "BED")
        } When {
            get("/api/v1/rooms/owned-items")
        } Then {
            statusCode(200)
            body("keySet()", hasItems("ownedRoomItems", "nextCursor", "hasNext"))
            body("ownedRoomItems[0].keySet()", hasItems("ownedRoomItemId", "name", "itemId", "isEquipped"))
        }
    }

    // 처음부터 다시 주면 앱이 첫 묶음을 뒤에 이어 붙여 중복이 생기므로 거부해야 한다.
    @Test
    fun `내 것이 아닌 커서로 보유 아이템 목록을 조회하면 거부한다`() {
        Given {
            spec(RequestSpecs.createAuthorizedSpec())
            queryParams(mapOf("category" to "BED", "cursor" to Long.MAX_VALUE))
        } When {
            get("/api/v1/rooms/owned-items")
        } Then {
            statusCode(400)
            body("code", equalTo("api.common.invalidInputValue"))
        }
    }

    companion object {
        private val ROOM_ITEM_CATEGORIES = listOf("BACKGROUND", "BED", "CAT_WHEEL", "CARPET", "ETC")
    }
}
