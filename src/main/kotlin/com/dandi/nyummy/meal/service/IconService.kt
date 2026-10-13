package com.dandi.nyummy.meal.service

import com.dandi.nyummy.meal.dto.IconSummaryResult
import com.dandi.nyummy.meal.mapper.toIconSummaryResult
import com.dandi.nyummy.meal.repository.IconRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class IconService(private val iconRepository: IconRepository) {
    @Cacheable(cacheNames = ["icons"])
    @Transactional(readOnly = true)
    fun getIcons(): List<IconSummaryResult> = iconRepository.findAll()
        .map { it.toIconSummaryResult() }
}
