package com.dandi.nyummy.admin.controller

import com.dandi.nyummy.admin.config.AdminPaths
import com.dandi.nyummy.admin.config.ConditionalOnAdminEnabled
import com.dandi.nyummy.admin.dto.AdminRoomItemResponse
import com.dandi.nyummy.admin.dto.AdminRoomItemsResponse
import com.dandi.nyummy.admin.dto.UpdateRoomItemRequest
import com.dandi.nyummy.admin.service.AdminRoomItemService
import com.dandi.nyummy.config.openapi.ApiErrorCodes
import com.dandi.nyummy.exception.errorcode.AdminErrorCode
import com.dandi.nyummy.exception.errorcode.CdnErrorCode
import com.dandi.nyummy.exception.errorcode.S3ErrorCode
import com.dandi.nyummy.room.enum.RoomItemCategory
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

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

    @Operation(
        summary = "마이룸 아이템 만들기",
        description = "마이룸 아이템(items + room_items)을 만들고 이미지를 S3 items/{itemId}.png에 올린다. 이미지는 PNG, 5MB 이하다. " +
            "이름은 앞뒤 공백을 다듬어 저장하고, 같은 이름의 아이템이 있으면 거절한다. price를 비우면 비매품(보상 전용)이다. " +
            "업로드에 실패하면 만든 행을 지운다. 만들어도 사용자에게 바로 보이지는 않는다(기본 지급·상점·보상에 넣어야 한다).",
    )
    @ApiErrorCodes(
        admin = [AdminErrorCode.DUPLICATE_ROOM_ITEM_NAME],
        s3 = [S3ErrorCode.FILE_SIZE_EXCEEDED, S3ErrorCode.UNSUPPORTED_CONTENT_TYPE, S3ErrorCode.OBJECT_UPLOAD_FAILED],
    )
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createRoomItem(
        @Parameter(description = "아이템 이름 (100자 이하)")
        @RequestParam
        @NotBlank
        @Size(max = 100)
        name: String,
        @Parameter(description = "가격(코인, 0 이상). 비우면 비매품(보상 전용)")
        @RequestParam(required = false)
        @PositiveOrZero
        price: Int?,
        @Parameter(description = "카테고리") @RequestParam category: RoomItemCategory,
        @Parameter(description = "PNG 이미지 (5MB 이하)") @RequestPart("image") image: MultipartFile,
    ): ResponseEntity<AdminRoomItemResponse> {
        val response = adminRoomItemService.createRoomItem(name, price, category, image)

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response)
    }

    @Operation(
        summary = "마이룸 아이템 이름 · 가격 수정",
        description = "이름과 가격을 보낸 값으로 통째로 바꾼다. 둘 다 보내야 하고, price가 null이면(키를 빼도 null로 받는다) " +
            "비매품(보상 전용)이 된다. 이름은 앞뒤 공백을 다듬어 저장하고, 다른 아이템이 같은 이름을 쓰면 거절한다(자기 이름 그대로는 허용). " +
            "카테고리와 이미지는 바꾸지 않는다. 이름은 사용자의 보유 아이템 목록·우편 보상에 바로 반영된다.",
    )
    @ApiErrorCodes(admin = [AdminErrorCode.ROOM_ITEM_NOT_FOUND, AdminErrorCode.DUPLICATE_ROOM_ITEM_NAME])
    @PutMapping("/{itemId}")
    fun updateRoomItem(
        @Parameter(description = "아이템 ID") @PathVariable("itemId") itemId: Long,
        @Valid @RequestBody request: UpdateRoomItemRequest,
    ): AdminRoomItemResponse = adminRoomItemService.updateRoomItem(itemId, request)

    @Operation(
        summary = "마이룸 아이템 이미지 교체",
        description = "S3 items/{itemId}.png를 새 이미지로 덮어쓰고 CDN 캐시를 버린다. 이미지는 PNG, 5MB 이하다. " +
            "DB와 imageUrl은 바뀌지 않는다. CDN 반영은 보통 수 초~수 분, 이미 받아 둔 앱 기기는 최대 1시간 뒤 바뀐다. " +
            "기본 지급 아이템을 바꾸면 그 아이템을 가진 모든 사용자의 방이 바뀐다. " +
            "CDN 무효화에 실패하면(api.cdn.invalidationFailed) 이미지는 이미 바뀐 상태이므로 같은 파일로 다시 교체한다.",
    )
    @ApiErrorCodes(
        admin = [AdminErrorCode.ROOM_ITEM_NOT_FOUND],
        s3 = [S3ErrorCode.FILE_SIZE_EXCEEDED, S3ErrorCode.UNSUPPORTED_CONTENT_TYPE, S3ErrorCode.OBJECT_UPLOAD_FAILED],
        cdn = [CdnErrorCode.INVALIDATION_FAILED],
    )
    @PutMapping("/{itemId}/image", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun updateRoomItemImage(
        @Parameter(description = "아이템 ID") @PathVariable("itemId") itemId: Long,
        @Parameter(description = "PNG 이미지 (5MB 이하)") @RequestPart("image") image: MultipartFile,
    ): AdminRoomItemResponse = adminRoomItemService.updateRoomItemImage(itemId, image)
}
