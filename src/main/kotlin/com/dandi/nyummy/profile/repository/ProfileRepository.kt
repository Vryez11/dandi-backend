package com.dandi.nyummy.profile.repository

import com.dandi.nyummy.profile.entity.Profile
import org.springframework.data.jpa.repository.JpaRepository

interface ProfileRepository : JpaRepository<Profile, Long> {

    fun getProfileByUserId(userId: Long): Profile?
}