package com.codersee.webclientwiremock.model

data class PageableGitHubResponse<T>(
    val items: List<T>,
    val hasMoreItems: Boolean,
)
