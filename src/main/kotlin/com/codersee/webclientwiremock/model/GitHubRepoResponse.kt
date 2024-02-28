package com.codersee.webclientwiremock.model

data class GitHubRepoResponse(
    val fork: Boolean,
    val name: String,
    val owner: GitHubOwnerResponse,
)
