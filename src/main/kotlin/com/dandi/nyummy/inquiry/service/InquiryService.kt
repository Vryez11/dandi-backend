package com.dandi.nyummy.inquiry.service

import com.dandi.nyummy.exception.BusinessException
import com.dandi.nyummy.exception.errorcode.InquiryErrorCode
import com.dandi.nyummy.inquiry.dto.CreateInquiryRequest
import com.dandi.nyummy.inquiry.dto.InquiryResponse
import com.dandi.nyummy.inquiry.mapper.toInquiry
import com.dandi.nyummy.inquiry.mapper.toInquiryResponse
import com.dandi.nyummy.inquiry.repository.InquiryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant

@Service
class InquiryService(private val inquiryRepository: InquiryRepository, private val clock: Clock) {

    /**
     * 사용자의 문의를 생성한다.
     *
     * 이미지(imageKey)와 앱 버전(appVersion)은 아직 받지 않는다.
     * 이미지는 presigned URL 발급이 추상화된 뒤에, 앱 버전은 공통 헤더가 구현된 뒤에 추가한다.
     */
    @Transactional
    fun createInquiry(userId: Long, request: CreateInquiryRequest) {
        inquiryRepository.save(request.toInquiry(userId))
    }

    /**
     * 문의 하나의 상세 내용을 조회한다.
     *
     * 답변이 달린 문의를 처음 조회하면 답변 읽음 시각을 기록한다. home의 안 읽은 답변 여부가 이 값으로 계산되므로,
     * 조회 API지만 쓰기 트랜잭션으로 연다.
     *
     * @throws BusinessException [InquiryErrorCode.INQUIRY_NOT_FOUND] 문의가 없거나, 삭제됐거나, 다른 사용자의 문의인 경우
     */
    @Transactional
    fun getInquiry(userId: Long, inquiryId: Long): InquiryResponse {
        val inquiry = inquiryRepository.getInquiryByIdAndDeletedAtIsNull(inquiryId)
            ?: throw BusinessException(InquiryErrorCode.INQUIRY_NOT_FOUND)

        inquiry.validateOwnership(userId)
        inquiry.updateAnswerReadAt(Instant.now(clock))

        return inquiry.toInquiryResponse()
    }
}
