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
    fun configuredDemoTokenIsSentAsDemoHeader() = runTest {
        val recordedRequest = AtomicReference<RecordedRequest>()

        TestServer(
            responseBody = """{"items":[]}""",
            handler = { recordedRequest.set(it) }
        ).use { server ->
            HttpCommunityApiClient(
                baseUrl = server.baseUrl,
                demoToken = "community-secret"
            ).getFeed()

            assertEquals("community-secret", recordedRequest.get()?.demoToken)
        }
    }

    @Test
    fun blankDemoTokenIsNotSentAsDemoHeader() = runTest {
        val recordedRequest = AtomicReference<RecordedRequest>()

        TestServer(
            responseBody = """{"items":[]}""",
            handler = { recordedRequest.set(it) }
        ).use { server ->
            HttpCommunityApiClient(
                baseUrl = server.baseUrl,
                demoToken = " "
            ).getFeed()

            assertEquals(null, recordedRequest.get()?.demoToken)
        }
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
                  "source": {
                    "kind": "fantasy-pet-rule",
                    "appJobId": "stardust-job-001"
                  },
                  "assets": {
                    "previewPath": "previews/overall-showcase.png",
                    "targetDownloadId": "artifact-34",
                    "previewUrl": "/pet-generation-jobs/stardust-job-001/artifacts/artifact-34",
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
        assertEquals("stardust-job-001", response.items[0].source.appJobId)
        assertEquals("artifact-34", response.items[0].assets.targetDownloadId)
        assertEquals(
            "/pet-generation-jobs/stardust-job-001/artifacts/artifact-34",
            response.items[0].assets.previewUrl
        )
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
    fun decodesCommunityHomeJson() {
        val json = """
            {
              "schema": "gamer.community-home.v1",
              "userId": "user-demo-001",
              "feed": {
                "items": [
                  {
                    "id": "post-live-001",
                    "authorId": "user-demo-001",
                    "petId": "pet-live-001",
                    "title": "Live feed",
                    "body": "Remote body",
                    "reactionCount": 18,
                    "createdAt": "2026-06-09T00:00:00.000Z"
                  }
                ]
              },
              "wallet": {
                "userId": "user-demo-001",
                "balance": 140,
                "currencyCode": "petcoin",
                "ledgerEntries": []
              },
              "approvedPets": {"items": []},
              "dailyCheckIn": {
                "date": "2026-06-09",
                "claimed": true,
                "rewardAmount": 10,
                "ledgerEntryId": "ledger-checkin-2026-06-09"
              },
              "submissionsSummary": {
                "pendingCount": 2,
                "approvedCount": 1,
                "heldCount": 0,
                "rejectedCount": 0,
                "revokedCount": 0,
                "latest": {
                  "id": "submission-local-010",
                  "petId": "pet-live-001",
                  "userId": "user-demo-001",
                  "status": "pending"
                }
              }
            }
        """.trimIndent()

        val home = HttpCommunityApiClient.decodeCommunityHome(json)

        assertEquals("gamer.community-home.v1", home.schema)
        assertEquals("Live feed", home.feed.items[0].title)
        assertEquals(140, home.wallet.balance)
        assertEquals(true, home.dailyCheckIn.claimed)
        assertEquals(2, home.submissionsSummary.pendingCount)
        assertEquals("submission-local-010", home.submissionsSummary.latest?.id)
    }

    @Test
    fun decodesCommunitySlaJson() {
        val json = """
            {
              "schema": "gamer.sla.v1",
              "hatch": {
                "reserveEggMaxMs": 60000,
                "mysteryEggMaxMs": 300000,
                "customHatchMaxMs": 777000
              },
              "polling": {
                "suggestedIntervalMs": 5000,
                "maxAttempts": 5,
                "baseBackoffMs": 2000
              },
              "failureThresholds": {
                "consecutivePollFailuresBeforeSlowNotice": 2
              }
            }
        """.trimIndent()

        val sla = HttpCommunityApiClient.decodeCommunitySla(json)

        assertEquals("gamer.sla.v1", sla.schema)
        assertEquals(60_000L, sla.hatch.reserveEggMaxMs)
        assertEquals(777_000L, sla.hatch.customHatchMaxMs)
        assertEquals(5_000L, sla.polling.suggestedIntervalMs)
        assertEquals(2, sla.failureThresholds.consecutivePollFailuresBeforeSlowNotice)
    }

    @Test
    fun decodesImportDraftJson() {
        val json = """
            {
              "id": "import-draft-local-001",
              "userId": "user-demo-001",
              "status": "ready",
              "petId": "public-lifecycle-smoke",
              "ownershipClaimId": "claim-public-lifecycle-smoke",
              "scoreReportId": "score-import-draft-local-001"
            }
        """.trimIndent()

        val draft = HttpCommunityApiClient.decodeImportDraft(json)

        assertEquals("import-draft-local-001", draft.id)
        assertEquals("ready", draft.status)
        assertEquals("public-lifecycle-smoke", draft.petId)
        assertEquals("score-import-draft-local-001", draft.scoreReportId)
    }

    @Test
    fun decodesSubmissionsJson() {
        val json = """
            {
              "submissions": [
                {
                  "id": "submission-local-001",
                  "petId": "public-lifecycle-smoke",
                  "userId": "user-demo-001",
                  "status": "pending",
                  "scoreReportId": "score-import-draft-local-001",
                  "ownershipClaimId": "claim-public-lifecycle-smoke",
                  "importDraftId": "import-draft-local-001",
                  "submittedAt": "2026-06-08T00:00:00.000Z"
                }
              ],
              "reviewQueue": []
            }
        """.trimIndent()

        val response = HttpCommunityApiClient.decodeSubmissions(json)

        assertEquals(1, response.submissions.size)
        assertEquals("submission-local-001", response.submissions[0].id)
        assertEquals("pending", response.submissions[0].status)
    }

    @Test
    fun decodesSubmissionJson() {
        val json = """
            {
              "id": "submission-local-001",
              "petId": "public-lifecycle-smoke",
              "userId": "user-demo-001",
              "status": "pending",
              "scoreReportId": "score-import-draft-local-001",
              "ownershipClaimId": "claim-public-lifecycle-smoke",
              "importDraftId": "import-draft-local-001",
              "submittedAt": "2026-06-08T00:00:00.000Z"
            }
        """.trimIndent()

        val response = HttpCommunityApiClient.decodeSubmission(json)

        assertEquals("submission-local-001", response.id)
        assertEquals("public-lifecycle-smoke", response.petId)
        assertEquals("pending", response.status)
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
    fun getCommunityHomeRequestsPublicHomePath() = runTest {
        val recordedRequest = AtomicReference<RecordedRequest>()
        val responseBody = """
            {
              "schema": "gamer.community-home.v1",
              "userId": "user-demo-001",
              "feed": {"items": []},
              "wallet": {
                "userId": "user-demo-001",
                "balance": 90,
                "currencyCode": "petcoin",
                "ledgerEntries": []
              },
              "approvedPets": {"items": []},
              "dailyCheckIn": {
                "date": "2026-06-09",
                "claimed": false,
                "rewardAmount": 10,
                "ledgerEntryId": ""
              },
              "submissionsSummary": {
                "pendingCount": 0,
                "approvedCount": 0,
                "heldCount": 0,
                "rejectedCount": 0,
                "revokedCount": 0,
                "latest": null
              }
            }
        """.trimIndent()

        TestServer(
            responseBody = responseBody,
            handler = { recordedRequest.set(it) }
        ).use { server ->
            val result = HttpCommunityApiClient(server.baseUrl).getCommunityHome()

            assertTrue(result is ApiCallResult.Success)
            assertEquals("GET", recordedRequest.get()?.method)
            assertEquals("/v1/community-home", recordedRequest.get()?.path)
        }
    }

    @Test
    fun getCommunitySlaRequestsPublicSlaPath() = runTest {
        val recordedRequest = AtomicReference<RecordedRequest>()
        val responseBody = """
            {
              "schema": "gamer.sla.v1",
              "hatch": {
                "reserveEggMaxMs": 120000,
                "mysteryEggMaxMs": 600000,
                "customHatchMaxMs": 900000
              },
              "polling": {
                "suggestedIntervalMs": 3000,
                "maxAttempts": 3,
                "baseBackoffMs": 1000
              },
              "failureThresholds": {
                "consecutivePollFailuresBeforeSlowNotice": 3
              }
            }
        """.trimIndent()

        TestServer(
            responseBody = responseBody,
            handler = { recordedRequest.set(it) }
        ).use { server ->
            val result = HttpCommunityApiClient(server.baseUrl).getCommunitySla()

            assertTrue(result is ApiCallResult.Success)
            assertEquals("GET", recordedRequest.get()?.method)
            assertEquals("/v1/sla", recordedRequest.get()?.path)
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
    fun getSubmissionsRequestsPublicSubmissionsPath() = runTest {
        val recordedRequest = AtomicReference<RecordedRequest>()
        val responseBody = """
            {
              "submissions": [
                {
                  "id": "submission-local-001",
                  "petId": "public-lifecycle-smoke",
                  "userId": "user-demo-001",
                  "status": "pending"
                }
              ],
              "reviewQueue": []
            }
        """.trimIndent()

        TestServer(
            responseBody = responseBody,
            handler = { recordedRequest.set(it) }
        ).use { server ->
            val result = HttpCommunityApiClient(server.baseUrl).getSubmissions()

            assertTrue(result is ApiCallResult.Success)
            assertEquals("GET", recordedRequest.get()?.method)
            assertEquals("/v1/submissions", recordedRequest.get()?.path)
        }
    }

    @Test
    fun getSubmissionRequestsEncodedPublicSubmissionPath() = runTest {
        val recordedRequest = AtomicReference<RecordedRequest>()
        val responseBody = """
            {
              "id": "submission local/001",
              "petId": "public-lifecycle-smoke",
              "userId": "user-demo-001",
              "status": "pending"
            }
        """.trimIndent()

        TestServer(
            responseBody = responseBody,
            handler = { recordedRequest.set(it) }
        ).use { server ->
            val result = HttpCommunityApiClient(server.baseUrl)
                .getSubmission("submission local/001")

            assertTrue(result is ApiCallResult.Success)
            assertEquals("GET", recordedRequest.get()?.method)
            assertEquals(
                "/v1/submissions/submission%20local%2F001",
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

    @Test
    fun createImportDraftFromFantasyPetPackagePostsPublicPackageManifest() = runTest {
        val recordedRequest = AtomicReference<RecordedRequest>()
        val responseBody = """
            {
              "id": "import-draft-local-001",
              "userId": "user-demo-001",
              "status": "ready",
              "petId": "public-lifecycle-smoke",
              "ownershipClaimId": "claim-public-lifecycle-smoke",
              "scoreReportId": "score-import-draft-local-001"
            }
        """.trimIndent()

        TestServer(
            responseBody = responseBody,
            handler = { recordedRequest.set(it) }
        ).use { server ->
            val result = HttpCommunityApiClient(server.baseUrl)
                .createImportDraftFromFantasyPetPackage(
                    FantasyPetPackageImportDraftRequestDto(
                        packageManifest = FantasyPetPackageManifestDto(
                            runId = "run-public-lifecycle-smoke",
                            appJobId = "public-lifecycle-smoke",
                            acceptedBy = "human-review",
                            sourceDownloadId = "artifact-1",
                            sourceTaskId = "codex-worker-task",
                            files = listOf(
                                FantasyPetPackageFileDto(
                                    kind = "candidate",
                                    path = "artifacts/candidates/final-preview.png"
                                )
                            )
                        ),
                        packageFileName = "pet-public-lifecycle-smoke.zip",
                        packageByteCount = 664L,
                        targetDownloadId = "artifact-1",
                        ownershipClaimId = "claim-public-lifecycle-smoke"
                    )
                )

            assertTrue(result is ApiCallResult.Success)
            assertEquals("POST", recordedRequest.get()?.method)
            assertEquals(
                "/v1/import-drafts/from-fantasy-pet-package",
                recordedRequest.get()?.path
            )
            assertTrue(
                recordedRequest.get()?.body.orEmpty()
                    .contains("\"schema\":\"fantasy-pet.package-manifest.v1\"")
            )
            assertTrue(
                recordedRequest.get()?.body.orEmpty()
                    .contains("\"targetDownloadId\":\"artifact-1\"")
            )
        }
    }

    @Test
    fun submitImportDraftPostsDraftIdToPublicSubmitEndpoint() = runTest {
        val recordedRequest = AtomicReference<RecordedRequest>()
        val responseBody = """
            {
              "draft": {
                "id": "import-draft-local-001",
                "userId": "user-demo-001",
                "status": "submitted",
                "petId": "public-lifecycle-smoke",
                "ownershipClaimId": "claim-public-lifecycle-smoke",
                "scoreReportId": "score-import-draft-local-001",
                "submissionId": "submission-local-001"
              },
              "submission": {
                "id": "submission-local-001",
                "petId": "public-lifecycle-smoke",
                "userId": "user-demo-001",
                "status": "pending",
                "scoreReportId": "score-import-draft-local-001",
                "ownershipClaimId": "claim-public-lifecycle-smoke",
                "importDraftId": "import-draft-local-001",
                "submittedAt": "2026-06-08T00:00:00.000Z"
              }
            }
        """.trimIndent()

        TestServer(
            responseBody = responseBody,
            handler = { recordedRequest.set(it) }
        ).use { server ->
            val result = HttpCommunityApiClient(server.baseUrl)
                .submitImportDraft("import-draft-local-001")

            assertTrue(result is ApiCallResult.Success)
            assertEquals("POST", recordedRequest.get()?.method)
            assertEquals("/v1/import-drafts/submit", recordedRequest.get()?.path)
            assertTrue(
                recordedRequest.get()?.body.orEmpty()
                    .contains("\"draftId\":\"import-draft-local-001\"")
            )
            val response = (result as ApiCallResult.Success<ImportDraftSubmissionResponseDto>).value
            assertEquals("submitted", response.draft.status)
            assertEquals("submission-local-001", response.submission.id)
            assertEquals("pending", response.submission.status)
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
            handler?.invoke(
                RecordedRequest(
                    method = exchange.requestMethod,
                    path = exchange.requestURI.rawPath,
                    body = body,
                    demoToken = exchange.requestHeaders.getFirst("X-Demo-Token")
                )
            )
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

private data class RecordedRequest(
    val method: String,
    val path: String,
    val body: String,
    val demoToken: String?
)
