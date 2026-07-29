package com.dandi.nyummy.infra.aws.s3.dto

import aws.smithy.kotlin.runtime.net.url.Url

data class S3UploadResult(val url: Url, val key: String)
