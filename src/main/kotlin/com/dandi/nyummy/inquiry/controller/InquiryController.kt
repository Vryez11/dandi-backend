package com.dandi.nyummy.inquiry.controller

import com.dandi.nyummy.inquiry.dto.CreateInquiryRequest
import com.dandi.nyummy.inquiry.dto.InquiryResponse
import com.dandi.nyummy.inquiry.service.InquiryService
import com.dandi.nyummy.security.AuthUser
import com.dandi.nyummy.security.CurrentUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Inquiry", description = "문의 생성 · 조회 · 수정 · 삭제 API")
@RestController
@RequestMapping("/api/v1/inquiries")
class InquiryController(private val inquiryService: InquiryService) {

    @Operation(
        summary = "문의하기",
        description = "문의 유형(BUG_REPORT · QUESTION · SUGGESTION)과 제목, 내용으로 문의를 생성한다.",
    )
    @ApiResponse(responseCode = "400", description = "제목·내용이 비었거나 길이 제한(100 · 2000자)을 넘었습니다.")
    @PostMapping
    fun createInquiry(
        @CurrentUser user: AuthUser,
        @Valid @RequestBody request: CreateInquiryRequest,
    ): ResponseEntity<Void> {
        inquiryService.createInquiry(user.userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @Operation(
        summary = "문의 상세 조회",
        description = "문의 하나의 질문과 답변을 조회한다. 답변이 달린 문의를 처음 조회하면 답변을 읽은 것으로 기록한다.",
    )
    @ApiResponse(responseCode = "404", description = "문의가 없거나, 삭제됐거나, 다른 사용자의 문의입니다.")
    @GetMapping("/{inquiryId}")
    fun getInquiry(
        @CurrentUser user: AuthUser,
        @Parameter(description = "문의 ID") @PathVariable("inquiryId") inquiryId: Long,
    ): InquiryResponse = inquiryService.getInquiry(user.userId, inquiryId)
}
