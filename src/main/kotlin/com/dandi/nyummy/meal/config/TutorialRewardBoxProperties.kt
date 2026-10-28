package com.dandi.nyummy.meal.config

/**
 * 튜토리얼 식사 보상 상자에 넣을 아이템과 코인 수. 아이템은 [itemIds] 하나에 1개씩 넣는다.
 */
data class TutorialRewardBoxProperties(val itemIds: List<Long>, val coinQuantity: Int) {
    // 같은 아이템을 두 번 적으면 상자에 한 개가 더 들어가므로 앱이 뜰 때 막는다.
    init {
        require(itemIds.toSet().size == itemIds.size) {
            "app.meal.tutorial-reward-box.item-ids에 같은 itemId가 두 번 있습니다: $itemIds"
        }
        require(coinQuantity >= 1) { "app.meal.tutorial-reward-box.coin-quantity는 1 이상이어야 합니다: $coinQuantity" }
    }
}
