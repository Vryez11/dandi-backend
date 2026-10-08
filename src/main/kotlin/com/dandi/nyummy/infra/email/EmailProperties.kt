package com.dandi.nyummy.infra.email

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 구현체 선택(app.email.client)은 각 구현체의 @ConditionalOnProperty가 읽으므로 여기 두지 않는다.
 *
 * [fromAddress]는 구현체마다 달라야 한다 — SES는 인증된 도메인 주소, SMTP는 로그인한 계정 주소만 From으로 쓸 수 있다.
 */
@ConfigurationProperties(prefix = "app.email")
class EmailProperties(val fromAddress: String, val fromName: String)
