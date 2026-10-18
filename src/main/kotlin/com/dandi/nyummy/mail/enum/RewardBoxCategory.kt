package com.dandi.nyummy.mail.enum

/**
 * 보상 상자 우편의 등급. [description]은 우편 제목으로 쓴다.
 */
enum class RewardBoxCategory(val description: String) {
    BRONZE("브론즈 상자"),
    SILVER("실버 상자"),
    GOLD("골드 상자"),
}
