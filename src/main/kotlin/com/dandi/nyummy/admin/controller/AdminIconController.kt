package com.dandi.nyummy.admin.controller

import com.dandi.nyummy.admin.config.AdminPaths
import com.dandi.nyummy.admin.config.ConditionalOnAdminEnabled
import com.dandi.nyummy.admin.dto.AdminIconResponse
import com.dandi.nyummy.admin.dto.AdminIconsResponse
import com.dandi.nyummy.admin.dto.UpdateIconNameRequest
import com.dandi.nyummy.admin.service.AdminIconService
import com.dandi.nyummy.config.openapi.ApiErrorCodes
import com.dandi.nyummy.exception.errorcode.AdminErrorCode
import com.dandi.nyummy.exception.errorcode.CdnErrorCode
import com.dandi.nyummy.exception.errorcode.S3ErrorCode
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@Tag(name = "Admin Icon", description = "어드민 음식 아이콘 관리 API (어드민 서비스 전용)")
@RestController
@RequestMapping("${AdminPaths.PREFIX}/icons")
@SecurityRequirements
@ConditionalOnAdminEnabled
class AdminIconController(private val adminIconService: AdminIconService) {

    @Operation(
        summary = "음식 아이콘 목록 조회",
        description = "음식 아이콘 전체를 id 순서로 조회한다. 페이지를 나누지 않는다. " +
            "imageUrl은 앱과 같은 규칙(CDN 주소 + /icons/{iconId}.png)으로 만든 미리보기 주소다.",
    )
    @GetMapping
    fun getIcons(): AdminIconsResponse = adminIconService.getIcons()

    @Operation(
        summary = "음식 아이콘 만들기",
        description = "음식 아이콘 행을 만들고 이미지를 S3 icons/{iconId}.png에 올린다. 이미지는 PNG, 512KB 이하다. " +
            "이름은 앞뒤 공백을 다듬어 저장하고, 같은 이름이 있으면 거절한다. " +
            "업로드에 실패하면 만든 행을 지운다. 새 음식 아이콘은 최대 1시간 뒤부터 식사 분석에 쓰인다.",
    )
    @ApiErrorCodes(
        admin = [AdminErrorCode.DUPLICATE_ICON_NAME],
        s3 = [S3ErrorCode.FILE_SIZE_EXCEEDED, S3ErrorCode.UNSUPPORTED_CONTENT_TYPE, S3ErrorCode.OBJECT_UPLOAD_FAILED],
    )
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createIcon(
        @Parameter(description = "음식 아이콘 이름 (100자 이하)")
        @RequestParam
        @NotBlank
        @Size(max = 100)
        name: String,
        @Parameter(description = "PNG 이미지 (512KB 이하)") @RequestPart("image") image: MultipartFile,
    ): ResponseEntity<AdminIconResponse> {
        val response = adminIconService.createIcon(name, image)

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response)
    }

    @Operation(
        summary = "음식 아이콘 이름 수정",
        description = "음식 아이콘 이름만 바꾼다. 이미지와 지난 식사에는 영향이 없다. " +
            "앞뒤 공백을 다듬어 저장하고, 다른 음식 아이콘이 같은 이름을 쓰면 거절한다(자기 이름 그대로는 허용). " +
            "새 이름은 최대 1시간 뒤부터 식사 분석에 쓰인다.",
    )
    @ApiErrorCodes(admin = [AdminErrorCode.ICON_NOT_FOUND, AdminErrorCode.DUPLICATE_ICON_NAME])
    @PatchMapping("/{iconId}")
    fun updateIconName(
        @Parameter(description = "음식 아이콘 ID") @PathVariable("iconId") iconId: Long,
        @Valid @RequestBody request: UpdateIconNameRequest,
    ): AdminIconResponse = adminIconService.updateIconName(iconId, request)

    @Operation(
        summary = "음식 아이콘 이미지 교체",
        description = "S3 icons/{iconId}.png를 새 이미지로 덮어쓰고 CDN 캐시를 버린다. 이미지는 PNG, 512KB 이하다. " +
            "DB와 imageUrl은 바뀌지 않는다. CDN 반영은 보통 수 초~수 분, 이미 받아 둔 앱 기기는 최대 1시간 뒤 바뀐다. " +
            "CDN 무효화에 실패하면(api.cdn.invalidationFailed) 이미지는 이미 바뀐 상태이므로 같은 파일로 다시 교체한다.",
    )
    @ApiErrorCodes(
        admin = [AdminErrorCode.ICON_NOT_FOUND],
        s3 = [S3ErrorCode.FILE_SIZE_EXCEEDED, S3ErrorCode.UNSUPPORTED_CONTENT_TYPE, S3ErrorCode.OBJECT_UPLOAD_FAILED],
        cdn = [CdnErrorCode.INVALIDATION_FAILED],
    )
    @PutMapping("/{iconId}/image", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun updateIconImage(
        @Parameter(description = "음식 아이콘 ID") @PathVariable("iconId") iconId: Long,
        @Parameter(description = "PNG 이미지 (512KB 이하)") @RequestPart("image") image: MultipartFile,
    ): AdminIconResponse = adminIconService.updateIconImage(iconId, image)
}
