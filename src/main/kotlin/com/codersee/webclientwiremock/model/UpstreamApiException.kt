package com.codersee.webclientwiremock.model

import org.springframework.http.HttpStatusCode

data class UpstreamApiException(
    val msg: String,
    val statusCode: HttpStatusCode,
) : RuntimeException(msg)
