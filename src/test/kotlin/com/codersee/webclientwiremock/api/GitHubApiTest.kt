package com.codersee.webclientwiremock.api

import com.codersee.webclientwiremock.model.GitHubOwnerResponse
import com.codersee.webclientwiremock.model.GitHubRepoResponse
import com.codersee.webclientwiremock.model.PageableGitHubResponse
import com.codersee.webclientwiremock.model.UpstreamApiException
import com.codersee.webclientwiremock.util.getResponseBodyAsString
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.context.TestPropertySource
import java.util.UUID

private const val TEST_KEY = "TEST_KEY"
private const val TEST_PORT = 8082
private const val TEST_VERSION = "2022-11-28"

@AutoConfigureWireMock(port = TEST_PORT)
@TestPropertySource(
  properties = [
    "api.github.url=http://localhost:${TEST_PORT}",
    "api.github.key=$TEST_KEY",
    "api.github.version=$TEST_VERSION",
  ],
)
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
class GitHubApiTest {
  @Autowired
  private lateinit var wireMockServer: WireMockServer

  @Autowired
  private lateinit var gitHubApi: GitHubApi

  private val page = 1
  private val perPage = 2
  private val username = UUID.randomUUID().toString()

  @Test
  fun `Given 404 NOT FOUND response When fetching repository by username Then should return null`() = runTest {
    // Given
    wireMockServer.stubFor(
      WireMock.get(WireMock.urlEqualTo("/users/$username/repos?page=$page&per_page=$perPage"))
        .withHeader("Authorization", WireMock.equalTo("Bearer $TEST_KEY"))
        .withHeader("X-GitHub-Api-Version", WireMock.equalTo(TEST_VERSION))
        .withHeader("Accept", WireMock.equalTo("application/vnd.github+json"))
        .willReturn(
          WireMock.aResponse()
            .withStatus(HttpStatus.NOT_FOUND.value())
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8")
            .withBody(
              getResponseBodyAsString("/responses/external/github/list_github_repositories_404_NOT_FOUND.json"),
            ),
        ),
    )

    // When
    val result = gitHubApi.listRepositoriesByUsername(username, page, perPage)

    // Then
    assertNull(result)
  }

  @Test
  fun `Given 401 UAUTHORIZED response When fetching repository by username Then should throw UpstreamApiException`() =
    runTest {
      // Given
      wireMockServer.stubFor(
        WireMock.get(WireMock.urlEqualTo("/users/$username/repos?page=$page&per_page=$perPage"))
          .withHeader("Authorization", WireMock.equalTo("Bearer $TEST_KEY"))
          .withHeader("X-GitHub-Api-Version", WireMock.equalTo(TEST_VERSION))
          .withHeader("Accept", WireMock.equalTo("application/vnd.github+json"))
          .willReturn(
            WireMock.aResponse()
              .withStatus(HttpStatus.UNAUTHORIZED.value())
              .withHeader(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8")
              .withBody(
                getResponseBodyAsString("/responses/external/github/list_github_repositories_401_UNAUTHORIZED.json"),
              ),
          ),
      )

      // When
      val exception = assertThrows<UpstreamApiException> {
        gitHubApi.listRepositoriesByUsername(username, page, perPage)
      }

      // Then
      assertNotNull(exception)

      assertEquals("GitHub API request failed.", exception.msg)
      assertEquals(HttpStatus.UNAUTHORIZED, exception.statusCode)
    }

  @Test
  fun `Given 200 OK response with empty list When fetching repository by username Then should return repository with correct properties and has next false`() =
    runTest {
      // Given
      val linkHeader = """<https://api.github.com/user/64011387/repos?page=3&per_page=2>; rel="prev","""

      wireMockServer.stubFor(
        WireMock.get(WireMock.urlEqualTo("/users/$username/repos?page=$page&per_page=$perPage"))
          .withHeader("Authorization", WireMock.equalTo("Bearer $TEST_KEY"))
          .withHeader("X-GitHub-Api-Version", WireMock.equalTo(TEST_VERSION))
          .withHeader("Accept", WireMock.equalTo("application/vnd.github+json"))
          .willReturn(
            WireMock.aResponse()
              .withStatus(HttpStatus.OK.value())
              .withHeader(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8")
              .withHeader(HttpHeaders.LINK, linkHeader)
              .withBody(
                getResponseBodyAsString("/responses/external/github/list_github_repositories_200_OK_empty_list.json"),
              ),
          ),
      )

      // When
      val result = gitHubApi.listRepositoriesByUsername(username, page, perPage)

      // Then
      val expected =
        PageableGitHubResponse(
          items = emptyList<GitHubRepoResponse>(),
          hasMoreItems = false,
        )

      assertEquals(expected, result)
    }

  @Test
  fun `Given 200 OK response with payload When fetching repository by username Then should return repository with correct properties and has next true`() =
    runTest {
      // Given
      val linkHeader = """<https://api.github.com/user/64011387/repos?page=3&per_page=2>; rel="next","""

      wireMockServer.stubFor(
        WireMock.get(WireMock.urlEqualTo("/users/$username/repos?page=$page&per_page=$perPage"))
          .withHeader("Authorization", WireMock.equalTo("Bearer $TEST_KEY"))
          .withHeader("X-GitHub-Api-Version", WireMock.equalTo(TEST_VERSION))
          .withHeader("Accept", WireMock.equalTo("application/vnd.github+json"))
          .willReturn(
            WireMock.aResponse()
              .withStatus(HttpStatus.OK.value())
              .withHeader(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8")
              .withHeader(HttpHeaders.LINK, linkHeader)
              .withBody(
                getResponseBodyAsString("/responses/external/github/list_github_repositories_200_OK_page_1.json"),
              ),
          ),
      )

      // When
      val result = gitHubApi.listRepositoriesByUsername(username, page, perPage)

      // Then
      val expected =
        PageableGitHubResponse(
          items =
          listOf(
            GitHubRepoResponse(
              fork = false,
              name = "controlleradvice-vs-restcontrolleradvice",
              owner = GitHubOwnerResponse(login = "codersee-blog"),
            ),
            GitHubRepoResponse(
              fork = false,
              name = "freecodecamp-spring-boot-kotlin-excel",
              owner = GitHubOwnerResponse(login = "codersee-blog"),
            ),
          ),
          hasMoreItems = true,
        )

      assertEquals(expected, result)
    }

  @Test
  fun `Given 200 OK response with payload When fetching repository by username Then should return repository with correct properties and has next false`() =
    runTest {
      // Given
      val linkHeader = """<https://api.github.com/user/64011387/repos?page=3&per_page=2>; rel="prev","""

      wireMockServer.stubFor(
        WireMock.get(WireMock.urlEqualTo("/users/$username/repos?page=$page&per_page=$perPage"))
          .withHeader("Authorization", WireMock.equalTo("Bearer $TEST_KEY"))
          .withHeader("X-GitHub-Api-Version", WireMock.equalTo(TEST_VERSION))
          .withHeader("Accept", WireMock.equalTo("application/vnd.github+json"))
          .willReturn(
            WireMock.aResponse()
              .withStatus(HttpStatus.OK.value())
              .withHeader(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8")
              .withHeader(HttpHeaders.LINK, linkHeader)
              .withBody(
                getResponseBodyAsString("/responses/external/github/list_github_repositories_200_OK_page_1.json"),
              ),
          ),
      )

      // When
      val result = gitHubApi.listRepositoriesByUsername(username, page, perPage)

      // Then
      val expected =
        PageableGitHubResponse(
          items =
          listOf(
            GitHubRepoResponse(
              fork = false,
              name = "controlleradvice-vs-restcontrolleradvice",
              owner = GitHubOwnerResponse(login = "codersee-blog"),
            ),
            GitHubRepoResponse(
              fork = false,
              name = "freecodecamp-spring-boot-kotlin-excel",
              owner = GitHubOwnerResponse(login = "codersee-blog"),
            ),
          ),
          hasMoreItems = false,
        )

      assertEquals(expected, result)
    }

}