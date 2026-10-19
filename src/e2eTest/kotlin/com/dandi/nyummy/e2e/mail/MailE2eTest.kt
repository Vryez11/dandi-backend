package com.dandi.nyummy.e2e.mail

import com.dandi.nyummy.e2e.support.RequestSpecs
import com.dandi.nyummy.e2e.support.Smoke
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.hamcrest.Matchers.hasItems
import org.hamcrest.Matchers.instanceOf
import org.junit.jupiter.api.Test

class MailE2eTest {
    // 조회만 하므로 prod에서도 돌린다. 계정에 우편이 없어도 통과해야 하므로 원소 구조와 페이지 나누기는 보지 않는다.
    // 실제 우편이 맨 위에 생기는지는 식사 기록 보상으로 MealE2eTest가 확인한다.
    @Smoke
    @Test
    fun `우편 목록을 조회한다`() {
        Given {
            spec(RequestSpecs.createAuthorizedSpec())
        } When {
            get("/api/v1/mails")
        } Then {
            statusCode(200)
            body("keySet()", hasItems("mails", "nextCursor", "hasNext"))
            body("mails", instanceOf<Any>(List::class.java))
        }
    }
}
