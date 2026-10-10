package com.dandi.nyummy.image.dto

import java.time.Instant

data class UploadUrlResponse(
    val uploadUrl: String,
    val imageKey: String,
    val uploadMethod: String,
    val uploadHeaders: Map<String, String>,
    val expiresAt: String,
)

/**
 * 확정 검증을 통과한 업로드 이미지. 컨트롤러에 노출되지 않는 서비스 내부 반환값이다.
 *
 * @property imageKey 확정된 객체 키
 * @property capturedAt EXIF 촬영 시각. 읽을 수 없으면 null이며, 대체값은 도메인이 정한다
 */
data class UploadedImage(val imageKey: String, val capturedAt: Instant?)
