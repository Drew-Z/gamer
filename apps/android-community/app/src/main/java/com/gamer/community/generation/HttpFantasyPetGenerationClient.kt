package com.gamer.community.generation

import com.gamer.community.api.ApiCallResult
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class HttpFantasyPetGenerationClient(
    private val baseUrl: String
) : FantasyPetGenerationClient {
    override suspend fun createJob(
        request: PetGenerationJobCreateRequestDto
    ): ApiCallResult<PetGenerationJobResponseDto> =
        postJson(
            path = "/pet-generation-jobs",
            body = json.encodeToString(request),
            decode = Companion::decodeJob
        )

    override suspend fun getJob(appJobId: String): ApiCallResult<PetGenerationJobResponseDto> =
        getJson(
            path = "/pet-generation-jobs/${appJobId.pathSegment()}",
            decode = Companion::decodeJob
        )

    override suspend fun getArtifacts(
        appJobId: String
    ): ApiCallResult<PetGenerationArtifactIndexResponseDto> =
        getJson(
            path = "/pet-generation-jobs/${appJobId.pathSegment()}/artifacts",
            decode = Companion::decodeArtifacts
        )

    override suspend fun submitReviewDecision(
        appJobId: String,
        request: ReviewDecisionRequestDto
    ): ApiCallResult<PetGenerationJobResponseDto> =
        postJson(
            path = "/pet-generation-jobs/${appJobId.pathSegment()}/review-decisions",
            body = json.encodeToString(request),
            decode = Companion::decodeJob
        )

    override suspend fun downloadPackage(appJobId: String): ApiCallResult<ByteArray> =
        requestBytes(
            method = "GET",
            path = "/pet-generation-jobs/${appJobId.pathSegment()}/package"
        )

    override suspend fun getWorkerReadiness(): ApiCallResult<WorkerReadinessResponseDto> =
        getJson(
            path = "/worker-readiness",
            decode = Companion::decodeWorkerReadiness
        )

    override suspend fun getAppApiContract(): ApiCallResult<PetGenerationAppApiContractDto> =
        getJson(
            path = "/app-api-contract",
            decode = Companion::decodeAppApiContract
        )

    private suspend fun <T> getJson(
        path: String,
        decode: (String) -> T
    ): ApiCallResult<T> =
        requestText(method = "GET", path = path, body = null, decode = decode)

    private suspend fun <T> postJson(
        path: String,
        body: String,
        decode: (String) -> T
    ): ApiCallResult<T> =
        requestText(method = "POST", path = path, body = body, decode = decode)

    private suspend fun <T> requestText(
        method: String,
        path: String,
        body: String?,
        decode: (String) -> T
    ): ApiCallResult<T> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = openConnection(path)
            connection.requestMethod = method
            connection.setRequestProperty("Accept", "application/json")
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { output ->
                    output.write(body.toByteArray(Charsets.UTF_8))
                }
            }

            val status = connection.responseCode
            if (status !in 200..299) {
                return@withContext ApiCallResult.Failure("http_$status")
            }

            decodeCatching(connection.inputStream.readUtf8Text(), decode)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            ApiCallResult.Failure(error.message ?: error::class.java.simpleName)
        } finally {
            connection?.disconnect()
        }
    }

    private suspend fun requestBytes(method: String, path: String): ApiCallResult<ByteArray> =
        withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                connection = openConnection(path)
                connection.requestMethod = method
                connection.setRequestProperty("Accept", "application/zip")

                val status = connection.responseCode
                if (status !in 200..299) {
                    return@withContext ApiCallResult.Failure("http_$status")
                }

                ApiCallResult.Success(connection.inputStream.use { it.readBytes() })
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                ApiCallResult.Failure(error.message ?: error::class.java.simpleName)
            } finally {
                connection?.disconnect()
            }
        }

    private fun openConnection(path: String): HttpURLConnection =
        (URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection).apply {
            connectTimeout = 2_000
            readTimeout = 2_000
        }

    private fun String.pathSegment(): String =
        URLEncoder.encode(this, Charsets.UTF_8.name()).replace("+", "%20")

    private fun InputStream.readUtf8Text(): String =
        BufferedReader(InputStreamReader(this, Charsets.UTF_8)).use { it.readText() }

    companion object {
        @OptIn(ExperimentalSerializationApi::class)
        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            explicitNulls = false
        }

        fun decodeJob(text: String): PetGenerationJobResponseDto =
            json.decodeFromString<PetGenerationJobResponseDto>(text)

        fun decodeArtifacts(text: String): PetGenerationArtifactIndexResponseDto =
            json.decodeFromString<PetGenerationArtifactIndexResponseDto>(text)

        fun decodeWorkerReadiness(text: String): WorkerReadinessResponseDto =
            json.decodeFromString<WorkerReadinessResponseDto>(text)

        fun decodeAppApiContract(text: String): PetGenerationAppApiContractDto =
            json.decodeFromString<PetGenerationAppApiContractDto>(text)

        fun <T> decodeCatching(text: String, decode: (String) -> T): ApiCallResult<T> =
            try {
                ApiCallResult.Success(decode(text))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                ApiCallResult.Failure(error.message ?: error::class.java.simpleName)
            }
    }
}
