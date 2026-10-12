package com.dandi.nyummy.inquiry.controller

import com.dandi.nyummy.inquiry.dto.CreateInquiryRequest
import com.dandi.nyummy.inquiry.dto.InquiriesResponse
import com.dandi.nyummy.inquiry.dto.InquiryResponse
import com.dandi.nyummy.inquiry.dto.UpdateInquiryRequest
import com.dandi.nyummy.inquiry.service.InquiryService
import com.dandi.nyummy.security.AuthUser
import com.dandi.nyummy.security.CurrentUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Inquiry", description = "문의 생성 · 조회 · 수정 · 삭제 API")
@RestController
@RequestMapping("/api/v1/inquiries")
class InquiryController(private val inquiryService: InquiryService) {

    @Operation(
        summary = "문의하기",
        description = "문의 유형(BUG_REPORT · QUESTION · SUGGESTION)과 제목, 내용으로 문의를 생성한다. " +
            "이미지는 선택이며, 이미지 업로드 URL 발급 API에 purpose=INQUIRY로 받은 imageKey로 업로드한 뒤 imageKey를 함께 보낸다.",
    )
    @ApiResponse(
        responseCode = "400",
        description = "제목·내용이 비었거나 길이 제한(100 · 2000자)을 넘었거나, " +
            "imageKey가 요청자의 문의 경로가 아니거나, 업로드된 이미지가 없거나, 이미지 크기·형식이 허용되지 않습니다.",
    )
    @ApiResponse(responseCode = "409", description = "이미 문의에 사용된 imageKey입니다.")
    @PostMapping
    fun createInquiry(
        @CurrentUser user: AuthUser,
        @Valid @RequestBody request: CreateInquiryRequest,
    ): ResponseEntity<Void> {
        inquiryService.createInquiry(user.userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @Operation(
        summary = "문의 목록 조회",
        description = "문의 목록을 최신순으로 조회한다. 무한 스크롤용 cursor 방식이다. " +
            "첫 요청은 cursor 없이 보내고, 다음 요청부터는 직전 응답의 nextCursor를 cursor로 보낸다. " +
            "hasNext가 false면 마지막 묶음이며 nextCursor는 null이다.",
    )
    @ApiResponse(responseCode = "400", description = "size가 1~50 범위를 벗어났습니다.")
    @GetMapping
    fun getInquiries(
        @CurrentUser user: AuthUser,
        @Parameter(description = "직전 응답의 nextCursor. 첫 요청에서는 생략한다.") @RequestParam(required = false) cursor: Long?,
        @Parameter(description = "한 번에 조회할 개수 (1~50, 기본 10)")
        @RequestParam(defaultValue = "10")
        @Min(1)
        @Max(50)
        size: Int,
    ): InquiriesResponse = inquiryService.getInquiries(user.userId, cursor, size)

    @Operation(
        summary = "문의 상세 조회",
        description = "문의 하나의 질문과 답변을 조회한다. 답변이 달린 문의를 처음 조회하면 답변을 읽은 것으로 기록한다. " +
            "이미지가 있으면 imageUrl에 일정 시간 동안만 유효한 조회 URL이 들어 있고, 없으면 null이다.",
    )
    @ApiResponse(responseCode = "404", description = "문의가 없거나 접근할 수 없습니다.")
    @GetMapping("/{inquiryId}")
    fun getInquiry(
        @CurrentUser user: AuthUser,
        @Parameter(description = "문의 ID") @PathVariable("inquiryId") inquiryId: Long,
    ): InquiryResponse = inquiryService.getInquiry(user.userId, inquiryId)

    @Operation(
        summary = "문의 수정",
        description = "문의 유형, 제목, 내용, 이미지를 수정하고 수정된 문의를 반환한다. 답변이 등록된 문의는 수정할 수 없다. " +
            "imageKey는 null이면 이미지 삭제, 기존과 같으면 유지, 다르면 새 이미지로 교체한다. " +
            "이미지를 유지하려면 기존 imageKey를 그대로 보내야 한다.",
    )
    @ApiResponse(
        responseCode = "400",
        description = "제목·내용이 비었거나 길이 제한(100 · 2000자)을 넘었거나, " +
            "새 imageKey가 요청자의 문의 경로가 아니거나, 업로드된 이미지가 없거나, 이미지 크기·형식이 허용되지 않습니다.",
    )
    @ApiResponse(responseCode = "404", description = "문의가 없거나 접근할 수 없습니다.")
    @ApiResponse(responseCode = "409", description = "답변이 등록된 문의이거나, 새 imageKey가 이미 문의에 사용됐습니다.")
    @PutMapping("/{inquiryId}")
    fun updateInquiry(
        @CurrentUser user: AuthUser,
        @Parameter(description = "문의 ID") @PathVariable("inquiryId") inquiryId: Long,
        @Valid @RequestBody request: UpdateInquiryRequest,
    ): InquiryResponse = inquiryService.updateInquiry(user.userId, inquiryId, request)

    @Operation(summary = "문의 삭제", description = "문의를 삭제한다. 답변이 등록된 문의도 삭제할 수 있다.")
    @ApiResponse(responseCode = "204", description = "삭제 성공")
    @ApiResponse(responseCode = "404", description = "문의가 없거나 접근할 수 없습니다.")
    @DeleteMapping("/{inquiryId}")
    fun deleteInquiry(
        @CurrentUser user: AuthUser,
        @Parameter(description = "문의 ID") @PathVariable("inquiryId") inquiryId: Long,
    ): ResponseEntity<Void> {
        inquiryService.deleteInquiry(user.userId, inquiryId)
        return ResponseEntity.noContent().build()
    }
}
