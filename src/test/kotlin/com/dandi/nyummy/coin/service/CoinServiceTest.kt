package com.dandi.nyummy.coin.service

import com.dandi.nyummy.coin.entity.Coin
import com.dandi.nyummy.coin.repository.CoinRepository
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals

class CoinServiceTest {

    private val userId = 1L

    private val coinRepository = mockk<CoinRepository>()

    private val coinService = CoinService(coinRepository = coinRepository)

    // getBalance

    @Test
    fun `코인 행이 있으면 잔액을 반환한다`() {
        every { coinRepository.findByUserId(userId) } returns Coin(userId = userId).apply { balance = 300 }

        assertEquals(300, coinService.getBalance(userId))
    }

    @Test
    fun `코인 행이 없으면 0을 반환한다`() {
        every { coinRepository.findByUserId(userId) } returns null

        assertEquals(0, coinService.getBalance(userId))
    }
}
