package com.dandi.nyummy.auth.repository

import com.dandi.nyummy.auth.entity.Code
import org.springframework.data.jpa.repository.JpaRepository

interface CodeRepository : JpaRepository<Code, Long> {

    fun findByEmail(email: String): Code?
}
