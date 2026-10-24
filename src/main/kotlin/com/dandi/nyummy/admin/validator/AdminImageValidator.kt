package com.dandi.nyummy.admin.validator

import com.dandi.nyummy.admin.config.ConditionalOnAdminEnabled
import com.dandi.nyummy.exception.BusinessException
import com.dandi.nyummy.exception.errorcode.S3ErrorCode
import org.apache.tika.Tika
import org.springframework.stereotype.Component

/**
 * 어드민이 올리는 음식 아이콘·마이룸 아이템 이미지 검증.
 *
 * 앱은 `https://cdn.nyummy.co.kr/{icons|items}/{id}.png`로 URL을 조립하므로 PNG만 받는다.
 * 브라우저가 보낸 Content-Type이나 파일 이름은 믿지 않고 실제 내용에서 형식을 감지한다
 * ([com.dandi.nyummy.image.service.ImageService.confirmUpload]와 같은 방식).
 */
@Component
@ConditionalOnAdminEnabled
class AdminImageValidator {

    companion object {
        private const val PNG_CONTENT_TYPE = "image/png"
    }

    private val tika = Tika()

    /**
     * @param bytes 업로드된 파일 내용
     * @param maxFileSizeBytes 허용하는 최대 크기(byte, 포함)
     * @throws BusinessException [S3ErrorCode.FILE_SIZE_EXCEEDED] 비었거나 [maxFileSizeBytes]를 넘을 경우
     * @throws BusinessException [S3ErrorCode.UNSUPPORTED_CONTENT_TYPE] 실제 내용이 PNG가 아닐 경우
     */
    fun validateImage(bytes: ByteArray, maxFileSizeBytes: Long) {
        if (bytes.isEmpty() || bytes.size > maxFileSizeBytes) {
            throw BusinessException(S3ErrorCode.FILE_SIZE_EXCEEDED)
        }

        if (tika.detect(bytes) != PNG_CONTENT_TYPE) {
            throw BusinessException(S3ErrorCode.UNSUPPORTED_CONTENT_TYPE)
        }
    }
}
