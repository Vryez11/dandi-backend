package com.dandi.nyummy.inquiry.controller

import com.dandi.nyummy.inquiry.dto.CreateInquiryRequest
import com.dandi.nyummy.inquiry.service.InquiryService
import com.dandi.nyummy.security.AuthUser
import com.dandi.nyummy.security.CurrentUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
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
}
