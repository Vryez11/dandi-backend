package com.dandi.nyummy.infra.email

data class EmailMessage(val to: String, val subject: String, val text: String)
