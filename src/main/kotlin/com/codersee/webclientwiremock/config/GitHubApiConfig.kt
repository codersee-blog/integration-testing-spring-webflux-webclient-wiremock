package com.codersee.webclientwiremock.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
@EnableConfigurationProperties(GitHubApiProperties::class)
class GitHubApiConfig(
    private val gitHubApiProperties: GitHubApiProperties,
) {
    @Bean
    fun webClient(builder: WebClient.Builder): WebClient =
        builder
            .baseUrl(gitHubApiProperties.url)
            .defaultHeader("Authorization", "Bearer ${gitHubApiProperties.key}")
            .defaultHeader("X-GitHub-Api-Version", gitHubApiProperties.version)
            .defaultHeader("Accept", "application/vnd.github+json")
            .build()
}
