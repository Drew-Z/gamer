package com.gamer.community.api

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class HttpCommunityApiClient(
    private val baseUrl: String,
    private val demoToken: String = ""
) : CommunityApiClient {
    override suspend fun getCommunityHome(): ApiCallResult<CommunityHomeResponseDto> =
        get("/v1/community-home", Companion::decodeCommunityHome)

    override suspend fun getCommunitySla(): ApiCallResult<CommunitySlaDto> =
        get("/v1/sla", Companion::decodeCommunitySla)

    override suspend fun getFeed(): ApiCallResult<FeedResponseDto> =
        get("/v1/feed", Companion::decodeFeed)

    override suspend fun getWallet(): ApiCallResult<WalletDto> =
        get("/v1/wallet/me", Companion::decodeWallet)

    override suspend fun getApprovedPets(): ApiCallResult<ApprovedPetsResponseDto> =
        get("/v1/pets/approved", Companion::decodeApprovedPets)

    override suspend fun getApprovedPetPackage(petId: String): ApiCallResult<ApprovedPetPackageDto> =
        get(
            "/v1/pets/approved/${petId.pathSegment()}/package",
            Companion::decodeApprovedPetPackage
        )

    override suspend fun getSubmissions(): ApiCallResult<SubmissionsResponseDto> =
        get("/v1/submissions", Companion::decodeSubmissions)

    override suspend fun getSubmission(submissionId: String): ApiCallResult<SubmissionDto> =
        get(
            "/v1/submissions/${submissionId.pathSegment()}",
            Companion::decodeSubmission
        )

    override suspend fun createImportDraftFromFantasyPetPackage(
        request: FantasyPetPackageImportDraftRequestDto
    ): ApiCallResult<ImportDraftDto> =
        post(
            "/v1/import-drafts/from-fantasy-pet-package",
            json.encodeToString(request),
            Companion::decodeImportDraft
        )

    override suspend fun submitImportDraft(draftId: String): ApiCallResult<ImportDraftSubmissionResponseDto> =
        post(
            "/v1/import-drafts/submit",
            json.encodeToString(ImportDraftSubmitRequestDto(draftId = draftId)),
            Companion::decodeImportDraftSubmission
        )

    override suspend fun claimDailyCheckIn(): ApiCallResult<CheckInResponseDto> =
        post("/v1/check-in", "{}", Companion::decodeCheckIn)

    private suspend fun <T> get(path: String, decode: (String) -> T): ApiCallResult<T> =
        request(method = "GET", path = path, body = null, decode = decode)

    private suspend fun <T> post(path: String, body: String, decode: (String) -> T): ApiCallResult<T> =
        request(method = "POST", path = path, body = body, decode = decode)

    private suspend fun <T> request(
        method: String,
        path: String,
        body: String?,
        decode: (String) -> T
    ): ApiCallResult<T> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection
            connection.requestMethod = method
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("Accept", "application/json")
            demoToken.trim().takeIf { it.isNotBlank() }?.let { token ->
                connection.setRequestProperty("X-Demo-Token", token)
            }

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

            val text = connection.inputStream.readUtf8Text()
            decodeCatching(text, decode)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            ApiCallResult.Failure(error.message ?: error::class.java.simpleName)
        } finally {
            connection?.disconnect()
        }
    }

    private fun String.pathSegment(): String =
        URLEncoder.encode(this, Charsets.UTF_8.name()).replace("+", "%20")

    private fun InputStream.readUtf8Text(): String =
        BufferedReader(InputStreamReader(this, Charsets.UTF_8)).use { it.readText() }

    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        fun decodeFeed(text: String): FeedResponseDto = json.decodeFromString<FeedResponseDto>(text)

        fun decodeCommunityHome(text: String): CommunityHomeResponseDto =
            json.decodeFromString<CommunityHomeResponseDto>(text)

        fun decodeCommunitySla(text: String): CommunitySlaDto =
            json.decodeFromString<CommunitySlaDto>(text)

        fun decodeWallet(text: String): WalletDto = json.decodeFromString<WalletDto>(text)

        fun decodeApprovedPets(text: String): ApprovedPetsResponseDto =
            json.decodeFromString<ApprovedPetsResponseDto>(text)

        fun decodeApprovedPetPackage(text: String): ApprovedPetPackageDto =
            json.decodeFromString<ApprovedPetPackageDto>(text)

        fun decodeImportDraft(text: String): ImportDraftDto =
            json.decodeFromString<ImportDraftDto>(text)

        fun decodeImportDraftSubmission(text: String): ImportDraftSubmissionResponseDto =
            json.decodeFromString<ImportDraftSubmissionResponseDto>(text)

        fun decodeSubmissions(text: String): SubmissionsResponseDto =
            json.decodeFromString<SubmissionsResponseDto>(text)

        fun decodeSubmission(text: String): SubmissionDto =
            json.decodeFromString<SubmissionDto>(text)

        fun decodeCheckIn(text: String): CheckInResponseDto = json.decodeFromString<CheckInResponseDto>(text)

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
