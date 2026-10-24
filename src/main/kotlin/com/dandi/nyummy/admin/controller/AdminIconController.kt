package com.dandi.nyummy.admin.controller

import com.dandi.nyummy.admin.config.AdminPaths
import com.dandi.nyummy.admin.config.ConditionalOnAdminEnabled
import com.dandi.nyummy.admin.dto.AdminIconsResponse
import com.dandi.nyummy.admin.service.AdminIconService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Admin Icon", description = "어드민 음식 아이콘 관리 API (어드민 서비스 전용)")
@RestController
@RequestMapping("${AdminPaths.PREFIX}/icons")
@SecurityRequirements
@ConditionalOnAdminEnabled
class AdminIconController(private val adminIconService: AdminIconService) {

    @Operation(
        summary = "음식 아이콘 목록 조회",
        description = "음식 아이콘 전체를 id 순서로 조회한다. 페이지를 나누지 않는다. " +
            "imageUrl은 앱과 같은 규칙(CDN 주소 + /icons/{iconId}.png)으로 만든 미리보기 주소다.",
    )
    @GetMapping
    fun getIcons(): AdminIconsResponse = adminIconService.getIcons()
}
