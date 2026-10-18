package com.dandi.nyummy.coin.service

import com.dandi.nyummy.coin.repository.CoinRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CoinService(private val coinRepository: CoinRepository) {
    companion object {
        private val logger = LoggerFactory.getLogger(CoinService::class.java)
    }

    /**
     * 사용자의 코인 잔액을 조회한다.
     *
     * coins 행은 가입 트랜잭션에서 함께 생성되고, 그 전에 가입한 사용자는 V0.41이 채웠으므로 없을 수 없다.
     * 없다면 데이터가 깨진 것이므로 추적할 수 있게 userId를 남기고, 화면이 깨지지 않도록 0을 반환한다.
     *
     * @param userId 조회하는 사용자 ID
     * @return 코인 잔액
     */
    @Transactional(readOnly = true)
    fun getBalance(userId: Long): Int {
        val coin = coinRepository.findByUserId(userId)
            ?: run {
                logger.error("가입 시 생성되어야 할 코인 행이 없습니다: userId={}", userId)
                return 0
            }

        return coin.balance
    }
}
