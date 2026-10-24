package com.dandi.nyummy.admin.controller

import com.dandi.nyummy.admin.config.AdminPaths
import com.dandi.nyummy.admin.config.ConditionalOnAdminEnabled
import com.dandi.nyummy.admin.dto.AdminRoomItemsResponse
import com.dandi.nyummy.admin.service.AdminRoomItemService
import com.dandi.nyummy.room.enum.RoomItemCategory
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Admin Room Item", description = "어드민 마이룸 아이템 관리 API (어드민 서비스 전용)")
@RestController
@RequestMapping("${AdminPaths.PREFIX}/room-items")
@SecurityRequirements
@ConditionalOnAdminEnabled
class AdminRoomItemController(private val adminRoomItemService: AdminRoomItemService) {

    @Operation(
        summary = "마이룸 아이템 목록 조회",
        description = "마이룸 아이템(상품)을 id 순서로 조회한다. 페이지를 나누지 않는다. price가 null이면 비매품(보상 전용)이다. " +
            "imageUrl은 앱과 같은 규칙(CDN 주소 + /items/{itemId}.png)으로 만든 미리보기 주소다.",
    )
    @GetMapping
    fun getRoomItems(
        @Parameter(description = "카테고리. 생략하면 모든 카테고리") @RequestParam(required = false) category: RoomItemCategory?,
    ): AdminRoomItemsResponse = adminRoomItemService.getRoomItems(category)
}
