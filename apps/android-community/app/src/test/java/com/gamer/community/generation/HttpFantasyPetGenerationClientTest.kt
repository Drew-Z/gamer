package com.gamer.community.generation

import com.gamer.community.api.ApiCallResult
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HttpFantasyPetGenerationClientTest {
    @Test
    fun createJobPostsPublicSchema() = runTest {
        val recordedRequest = AtomicReference<RecordedRequest>()
        val responseBody = """
            {
              "schema": "fantasy-pet.app-job-create-response.v1",
              "appJobId": "job-123",
              "runId": "job-123",
              "status": "queued",
              "nextAction": "wait",
              "requiresHumanReview": true
            }
        """.trimIndent()

        TestServer(
            responseBody = responseBody,
            handler = { recordedRequest.set(it) }
        ).use { server ->
            val result = HttpFantasyPetGenerationClient(server.baseUrl).createJob(
                PetGenerationJobCreateRequestDto(description = "tiny dragon")
            )

            assertTrue(result is ApiCallResult.Success)
            assertEquals("POST", recordedRequest.get()?.method)
            assertEquals("/pet-generation-jobs", recordedRequest.get()?.path)
            assertTrue(
                recordedRequest.get()?.body?.contains(
                    "\"schema\":\"fantasy-pet.app-job-create-request.v1\""
                ) == true
            )
        }
    }

    @Test
    fun getJobRequestsEncodedPublicPath() = runTest {
        val recordedRequest = AtomicReference<RecordedRequest>()

        TestServer(
            responseBody = """{"appJobId":"Job 123/A"}""",
            handler = { recordedRequest.set(it) }
        ).use { server ->
            HttpFantasyPetGenerationClient(server.baseUrl).getJob("Job 123/A")

            assertEquals("GET", recordedRequest.get()?.method)
            assertEquals("/pet-generation-jobs/Job%20123%2FA", recordedRequest.get()?.path)
        }
    }

    @Test
    fun reviewDecisionPostsToEncodedPublicPath() = runTest {
        val recordedRequest = AtomicReference<RecordedRequest>()

        TestServer(
            responseBody = """{"appJobId":"Job 123/A"}""",
            handler = { recordedRequest.set(it) }
        ).use { server ->
            HttpFantasyPetGenerationClient(server.baseUrl).submitReviewDecision(
                appJobId = "Job 123/A",
                request = ReviewDecisionRequestDto(
                    decisionId = "decision-1",
                    decision = "accept",
                    targetDownloadId = "artifact-1",
                    notes = listOf("User visually accepted this candidate in the app.")
                )
            )

            assertEquals("POST", recordedRequest.get()?.method)
            assertEquals(
                "/pet-generation-jobs/Job%20123%2FA/review-decisions",
                recordedRequest.get()?.path
            )
            assertTrue(recordedRequest.get()?.body?.contains("\"targetDownloadId\":\"artifact-1\"") == true)
            assertFalseText(recordedRequest.get()?.body.orEmpty(), "targetOutput")
        }
    }

    @Test
    fun downloadPackageRequestsEncodedPublicPath() = runTest {
        val recordedRequest = AtomicReference<RecordedRequest>()

        TestServer(
            responseBody = "zip-bytes",
            handler = { recordedRequest.set(it) }
        ).use { server ->
            val result = HttpFantasyPetGenerationClient(server.baseUrl).downloadPackage("Job 123/A")

            assertTrue(result is ApiCallResult.Success)
            assertEquals("GET", recordedRequest.get()?.method)
            assertEquals("/pet-generation-jobs/Job%20123%2FA/package", recordedRequest.get()?.path)
        }
    }

    @Test
    fun workerReadinessRequestsPublicPath() = runTest {
        val recordedRequest = AtomicReference<RecordedRequest>()
        val responseBody = """
            {
              "schema": "fantasy-pet.worker-readiness-public.v1",
              "status": "blocked",
              "adapters": [
                {
                  "adapter": "codex-cli",
                  "configured": false,
                  "status": "disabled",
                  "checks": {
                    "command": "disabled"
                  }
                }
              ],
              "security": {
                "secretsInReport": false,
                "executesAgentProcesses": false,
                "appMayInvokeAgentsDirectly": false,
                "executesReadinessProbe": false
              }
            }
        """.trimIndent()

        TestServer(
            responseBody = responseBody,
            handler = { recordedRequest.set(it) }
        ).use { server ->
            val result = HttpFantasyPetGenerationClient(server.baseUrl).getWorkerReadiness()

            assertTrue(result is ApiCallResult.Success)
            val readiness = (result as ApiCallResult.Success<WorkerReadinessResponseDto>).value
            assertEquals("GET", recordedRequest.get()?.method)
            assertEquals("/worker-readiness", recordedRequest.get()?.path)
            assertEquals("fantasy-pet.worker-readiness-public.v1", readiness.schema)
            assertEquals("blocked", readiness.status)
            assertEquals("codex-cli", readiness.adapters.first().adapter)
            assertEquals("disabled", readiness.adapters.first().checks["command"])
            assertEquals(false, readiness.security.appMayInvokeAgentsDirectly)
        }
    }

    @Test
    fun appApiContractRequestsPublicPath() = runTest {
        val recordedRequest = AtomicReference<RecordedRequest>()
        val responseBody = """
            {
              "schema": "fantasy-pet.app-api-contract.v1",
              "publicEndpoints": [
                {
                  "method": "POST",
                  "path": "/pet-generation-jobs",
                  "public": true,
                  "requestSchema": "fantasy-pet.app-job-create-request.v1",
                  "responseSchema": "fantasy-pet.app-job-create-response.v1"
                }
              ],
              "security": {
                "exposesInternalPaths": false,
                "exposesWorkerCommands": false,
                "exposesSecrets": false,
                "appMayInvokeAgentsDirectly": false,
                "requiresHumanReview": true,
                "adminEndpointsDisabledByDefault": true
              }
            }
        """.trimIndent()

        TestServer(
            responseBody = responseBody,
            handler = { recordedRequest.set(it) }
        ).use { server ->
            val result = HttpFantasyPetGenerationClient(server.baseUrl).getAppApiContract()

            assertTrue(result is ApiCallResult.Success)
            val contract = (result as ApiCallResult.Success<PetGenerationAppApiContractDto>).value
            assertEquals("GET", recordedRequest.get()?.method)
            assertEquals("/app-api-contract", recordedRequest.get()?.path)
            assertEquals("fantasy-pet.app-api-contract.v1", contract.schema)
            assertEquals("/pet-generation-jobs", contract.publicEndpoints.single().path)
            assertEquals(false, contract.security.appMayInvokeAgentsDirectly)
            assertEquals(true, contract.security.requiresHumanReview)
        }
    }
}

private fun assertFalseText(text: String, forbidden: String) {
    assertTrue("Did not expect <$forbidden> in <$text>", !text.contains(forbidden))
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
