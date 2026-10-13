package com.dandi.nyummy.infra.storage.s3.dto

data class S3ObjectContentResult(val bytes: ByteArray, val contentType: String?)

data class S3UploadResult(val url: String, val key: String, val uploadHeaders: Map<String, String>)
