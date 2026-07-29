package com.dandi.nyummy.infra.aws.s3.dto

data class S3ObjectContent(val bytes: ByteArray, val contentType: String?)
