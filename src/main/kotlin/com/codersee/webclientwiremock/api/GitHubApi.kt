package com.codersee.webclientwiremock.api

import com.codersee.webclientwiremock.model.GitHubRepoResponse
import com.codersee.webclientwiremock.model.PageableGitHubResponse
import com.codersee.webclientwiremock.model.UpstreamApiException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitExchangeOrNull

@Component
class GitHubApi(
  private val webClient: WebClient,
) {

  suspend fun listRepositoriesByUsername(
    username: String,
    page: Int,
    perPage: Int,
  ): PageableGitHubResponse<GitHubRepoResponse>? =
    webClient.get()
      .uri("/users/$username/repos?page=$page&per_page=$perPage")
      .awaitExchangeOrNull(::mapToPageableResponse)

  private suspend inline fun <reified T> mapToPageableResponse(clientResponse: ClientResponse): PageableGitHubResponse<T>? {
    val hasNext = checkIfMorePagesToFetch(clientResponse)

    return when (val statusCode = clientResponse.statusCode()) {
      HttpStatus.OK ->
        PageableGitHubResponse(
          items = clientResponse.awaitBody<List<T>>(),
          hasMoreItems = hasNext,
        )

      HttpStatus.NOT_FOUND -> null

      else -> throw UpstreamApiException(
        msg = "GitHub API request failed.",
        statusCode = statusCode,
      )
    }
  }

  private fun checkIfMorePagesToFetch(clientResponse: ClientResponse) =
    clientResponse.headers()
      .header("link")
      .firstOrNull()
      ?.contains("next")
      ?: false
}
