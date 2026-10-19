package com.dandi.nyummy.mail.repository

import com.dandi.nyummy.mail.entity.Mail
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MailRepository : JpaRepository<Mail, Long>
