package com.dandi.nyummy.infra.cdn

import aws.sdk.kotlin.services.cloudfront.CloudFrontClient
import aws.sdk.kotlin.services.cloudfront.createInvalidation
import aws.sdk.kotlin.services.cloudfront.model.CloudFrontException
import aws.sdk.kotlin.services.cloudfront.model.InvalidationBatch
import aws.sdk.kotlin.services.cloudfront.model.Paths
import com.dandi.nyummy.exception.BusinessException
import com.dandi.nyummy.exception.errorcode.CdnErrorCode
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * CloudFront 배포(cdn.nyummy.co.kr)의 엣지 캐시를 다룬다.
 *
 * 앱은 이미지 URL을 id로 조립하므로 같은 객체 키를 덮어쓰면 URL이 그대로다. 엣지는 캐시 기간 동안
 * 옛 사본을 내려주므로, 덮어쓴 경로는 [createInvalidation]으로 버려야 다음 요청이 S3 원본을 다시 가져간다.
 */
class CdnClient(private val cloudFrontClient: CloudFrontClient, private val distributionId: String) {

    companion object {
        private val logger = LoggerFactory.getLogger(CdnClient::class.java)
    }

    /**
     * 경로들의 엣지 캐시를 버린다. 요청이 접수되면 반환하고, 엣지에 반영되기까지는 보통 수 초~수 분 걸린다.
     *
     * 이미 사용자 기기에 받아 둔 사본은 버리지 못한다. 기기는 객체의 Cache-Control 기간이 지나야 다시 받는다.
     *
     * @param paths `/`로 시작하는 객체 경로 (예: `/icons/25.png`)
     * @throws BusinessException [CdnErrorCode.INVALIDATION_FAILED] CloudFront가 요청을 거부했을 경우
     */
    suspend fun createInvalidation(paths: List<String>) {
        try {
            cloudFrontClient.createInvalidation {
                distributionId = this@CdnClient.distributionId
                invalidationBatch = InvalidationBatch {
                    // 요청마다 달라야 새 무효화로 처리된다. 같은 값을 다시 보내면 이전 요청의 결과를 돌려준다.
                    callerReference = UUID.randomUUID().toString()
                    this.paths = Paths {
                        quantity = paths.size
                        items = paths
                    }
                }
            }
        } catch (e: CloudFrontException) {
            logger.error("CDN 캐시 무효화에 실패했습니다: paths={}", paths, e)
            throw BusinessException(CdnErrorCode.INVALIDATION_FAILED)
        }
    }
}
