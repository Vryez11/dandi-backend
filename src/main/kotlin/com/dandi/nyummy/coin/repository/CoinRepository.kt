package com.dandi.nyummy.coin.repository

import com.dandi.nyummy.coin.entity.Coin
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CoinRepository : JpaRepository<Coin, Long>
