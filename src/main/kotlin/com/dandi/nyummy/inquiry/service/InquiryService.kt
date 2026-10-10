package com.dandi.nyummy.inquiry.service

import com.dandi.nyummy.inquiry.dto.CreateInquiryRequest
import com.dandi.nyummy.inquiry.mapper.toInquiry
import com.dandi.nyummy.inquiry.repository.InquiryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class InquiryService(private val inquiryRepository: InquiryRepository) {

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
}
