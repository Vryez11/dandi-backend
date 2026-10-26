package com.dandi.nyummy.admin.config

import aws.sdk.kotlin.services.cloudfront.CloudFrontClient
import com.dandi.nyummy.infra.cdn.CdnClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * 어드민이 이미지를 교체할 때 CDN 캐시를 버리는 빈. 어드민 서비스(app.admin.enabled=true)에서만 만든다.
 *
 * 권한은 어드민 태스크 역할(nyummy-prod-admin-task-role)에만 있으므로 앱 서비스에는 만들지 않는다.
 */
@Configuration
@ConditionalOnAdminEnabled
class AdminCdnConfig {

    // CloudFront API는 리전 없는 전역 엔드포인트라 SDK에 us-east-1을 지정한다.
    // credentialsProvider는 S3와 같이 SDK 기본 체인(운영: ECS 태스크 역할 / 로컬: ~/.aws 프로필)을 쓴다.
    @Bean
    fun cloudFrontClient(): CloudFrontClient = CloudFrontClient {
        region = "us-east-1"
    }

    @Bean
    fun cdnClient(cloudFrontClient: CloudFrontClient, adminProperties: AdminProperties): CdnClient {
        val distributionId = adminProperties.cdnDistributionId?.takeIf { it.isNotBlank() }
            ?: error("어드민 서비스에는 app.admin.cdn-distribution-id(CDN_DISTRIBUTION_ID)가 필요합니다")

        return CdnClient(cloudFrontClient, distributionId)
    }
}
