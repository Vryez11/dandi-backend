package com.dandi.nyummy.meal.config

/**
 * 보상 상자 한 등급의 뽑기 설정. [rate]는 이 등급이 뽑힐 확률(0~100 정수 퍼센트)이고,
 * 코인 수는 [minCoinQuantity]~[maxCoinQuantity](양 끝 포함)에서 고르게 뽑는다.
 */
data class RewardBoxProperties(val rate: Int, val minCoinQuantity: Int, val maxCoinQuantity: Int) {
    init {
        require(rate in 0..100) { "rate는 0~100이어야 합니다: $rate" }
        require(minCoinQuantity in 1..maxCoinQuantity) {
            "코인 범위가 올바르지 않습니다: $minCoinQuantity~$maxCoinQuantity"
        }
    }
}
