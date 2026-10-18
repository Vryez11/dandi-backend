package com.dandi.nyummy.coin.service

import com.dandi.nyummy.coin.repository.CoinRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CoinService(private val coinRepository: CoinRepository) {

    /**
     * 사용자의 코인 잔액을 조회한다.
     *
     * coins 행이 없으면 0을 반환한다. 가입 시 coins 행을 만들기 전 버전으로 가입한 사용자
     * (V0.37 배포 뒤 가입자, 배포 중 구버전으로 가입한 사용자)는 행이 없을 수 있는데,
     * 지금까지 잔액을 바꾼 코드가 없어 이들의 실제 잔액은 0이다.
     *
     * @param userId 조회하는 사용자 ID
     * @return 코인 잔액
     */
    @Transactional(readOnly = true)
    fun getBalance(userId: Long): Int = coinRepository.findByUserId(userId)?.balance ?: 0
}
