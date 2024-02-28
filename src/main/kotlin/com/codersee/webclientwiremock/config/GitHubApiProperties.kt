package com.codersee.webclientwiremock.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("api.github")
data class GitHubApiProperties(
    val url: String,
    val key: String,
    val version: String,
)
