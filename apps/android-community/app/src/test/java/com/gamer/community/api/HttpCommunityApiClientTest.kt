package com.gamer.community.api

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class HttpCommunityApiClientTest {
    @Test
    fun decodesFeedJson() {
        val json = """
            {
              "items": [
                {
                  "id": "post-live-001",
                  "authorId": "user-demo-001",
                  "petId": "pet-live-001",
                  "title": "Live feed",
                  "body": "Remote body",
                  "reactionCount": 18,
                  "createdAt": "2026-06-05T00:00:00.000Z",
                  "ignored": true
                }
              ],
              "nextCursor": "page-2"
            }
        """.trimIndent()

        val feed = HttpCommunityApiClient.decodeFeed(json)

        assertEquals(1, feed.items.size)
        assertEquals("Live feed", feed.items[0].title)
        assertEquals("page-2", feed.nextCursor)
    }

    @Test
    fun decodesWalletJson() {
        val json = """
            {
              "userId": "user-demo-001",
              "balance": 123,
              "currencyCode": "petcoin",
              "ledgerEntries": []
            }
        """.trimIndent()

        val wallet = HttpCommunityApiClient.decodeWallet(json)

        assertEquals("user-demo-001", wallet.userId)
        assertEquals(123, wallet.balance)
        assertEquals("petcoin", wallet.currencyCode)
    }

    @Test
    fun decodesApprovedPetsJson() {
        val json = """
            {
              "items": [
                {
                  "petId": "pet-stardust-001",
                  "displayName": "Stardust Dragon",
                  "ownerUserId": "user-demo-001",
                  "source": {"kind": "fantasy-pet-rule"},
                  "assets": {
                    "previewPath": "previews/overall-showcase.png",
                    "motionSheetCount": 2,
                    "exportArtifactPath": "exports/stardust.zip"
                  },
                  "totalScore": 86
                }
              ]
            }
        """.trimIndent()

        val response = HttpCommunityApiClient.decodeApprovedPets(json)

        assertEquals("Stardust Dragon", response.items[0].displayName)
        assertEquals(2, response.items[0].assets.motionSheetCount)
        assertEquals("exports/stardust.zip", response.items[0].assets.exportArtifactPath)
    }

    @Test
    fun decodesApprovedPetPackageJson() {
        val json = """
            {
              "petId": "pet-stardust-001",
              "displayName": "Stardust Dragon",
              "ownerUserId": "user-demo-001",
              "package": {
                "exportArtifactPath": "exports/stardust-package.zip",
                "status": "available"
              },
              "assets": {
                "previewPath": "previews/overall-showcase.png",
                "motionSheetCount": 2
              },
              "source": {
                "kind": "fantasy-pet-rule",
                "runId": "stardust-run-001",
                "statePath": "D:/workspace4Codex/fantasy-pet-rule/runs/stardust/state.json"
              },
              "submissionId": "submission-local-001",
              "importDraftId": "import-draft-local-001",
              "scoreReportId": "score-import-draft-local-001"
            }
        """.trimIndent()

        val descriptor = HttpCommunityApiClient.decodeApprovedPetPackage(json)

        assertEquals("pet-stardust-001", descriptor.petId)
        assertEquals("Stardust Dragon", descriptor.displayName)
        assertEquals("exports/stardust-package.zip", descriptor.`package`.exportArtifactPath)
        assertEquals("available", descriptor.`package`.status)
        assertEquals("stardust-run-001", descriptor.source.runId)
        assertEquals("submission-local-001", descriptor.submissionId)
    }

    @Test
    fun invalidJsonBecomesFailure() {
        val result = HttpCommunityApiClient.decodeCatching("not-json") {
            HttpCommunityApiClient.decodeFeed(it)
        }

        assertTrue(result is ApiCallResult.Failure)
    }

    @Test
    fun decodeCatchingRethrowsCancellation() {
        try {
            HttpCommunityApiClient.decodeCatching("{}") {
                throw CancellationException("cancelled")
            }
            fail("CancellationException should be rethrown")
        } catch (error: CancellationException) {
            assertEquals("cancelled", error.message)
        }
    }

    @Test
    fun getFeedReturnsFailureForNonSuccessStatus() = runTest {
        TestServer(status = 500, responseBody = "server error").use { server ->
            val result = HttpCommunityApiClient(server.baseUrl).getFeed()

            assertEquals(ApiCallResult.Failure("http_500"), result)
        }
    }

    @Test
    fun getApprovedPetPackageRequestsEncodedPetPackagePath() = runTest {
        val recordedRequest = AtomicReference<RecordedRequest>()
        val responseBody = """
            {
              "petId": "pet stardust/001",
              "displayName": "Stardust Dragon",
              "ownerUserId": "user-demo-001",
              "package": {
                "exportArtifactPath": "exports/stardust-package.zip",
                "status": "available"
              },
              "assets": {
                "previewPath": "previews/overall-showcase.png",
                "motionSheetCount": 2
              },
              "source": {
                "kind": "fantasy-pet-rule"
              },
              "submissionId": "submission-local-001",
              "importDraftId": "import-draft-local-001",
              "scoreReportId": "score-import-draft-local-001"
            }
        """.trimIndent()

        TestServer(
            responseBody = responseBody,
            handler = { recordedRequest.set(it) }
        ).use { server ->
            val result = HttpCommunityApiClient(server.baseUrl)
                .getApprovedPetPackage("pet stardust/001")

            assertTrue(result is ApiCallResult.Success)
            assertEquals("GET", recordedRequest.get()?.method)
            assertEquals(
                "/v1/pets/approved/pet%20stardust%2F001/package",
                recordedRequest.get()?.path
            )
        }
    }

    @Test
    fun claimDailyCheckInPostsJsonBody() = runTest {
        val recordedRequest = AtomicReference<RecordedRequest>()
        val responseBody = """
            {
              "checkIn": {
                "userId": "user-demo-001",
                "date": "2026-06-05",
                "claimed": true,
                "rewardAmount": 10,
                "ledgerEntryId": "ledger-checkin-2026-06-05"
              },
              "wallet": {
                "userId": "user-demo-001",
                "balance": 133,
                "currencyCode": "petcoin",
                "ledgerEntries": []
              },
              "ledgerEntry": null
            }
        """.trimIndent()

        TestServer(
            responseBody = responseBody,
            handler = { recordedRequest.set(it) }
        ).use { server ->
            val result = HttpCommunityApiClient(server.baseUrl).claimDailyCheckIn()

            assertTrue(result is ApiCallResult.Success)
            assertEquals("POST", recordedRequest.get()?.method)
            assertEquals("/v1/check-in", recordedRequest.get()?.path)
            assertEquals("{}", recordedRequest.get()?.body)
        }
    }
}

private class TestServer(
    private val status: Int = 200,
    private val responseBody: String = "{}",
    private val handler: ((RecordedRequest) -> Unit)? = null
) : AutoCloseable {
    private val server = HttpServer.create(InetSocketAddress(0), 0)

    val baseUrl: String
        get() = "http://127.0.0.1:${server.address.port}"

    init {
        server.createContext("/") { exchange ->
            val body = exchange.requestBody.bufferedReader(Charsets.UTF_8).use { it.readText() }
            handler?.invoke(RecordedRequest(exchange.requestMethod, exchange.requestURI.rawPath, body))
            val bytes = responseBody.toByteArray(Charsets.UTF_8)
            exchange.sendResponseHeaders(status, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
    }

    override fun close() {
        server.stop(0)
    }
}

private data class RecordedRequest(val method: String, val path: String, val body: String)
