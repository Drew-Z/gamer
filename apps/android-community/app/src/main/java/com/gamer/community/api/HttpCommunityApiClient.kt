package com.gamer.community.api

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class HttpCommunityApiClient(
    private val baseUrl: String
) : CommunityApiClient {
    override suspend fun getFeed(): ApiCallResult<FeedResponseDto> =
        get("/v1/feed", Companion::decodeFeed)

    override suspend fun getWallet(): ApiCallResult<WalletDto> =
        get("/v1/wallet/me", Companion::decodeWallet)

    override suspend fun getApprovedPets(): ApiCallResult<ApprovedPetsResponseDto> =
        get("/v1/pets/approved", Companion::decodeApprovedPets)

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
            connection.connectTimeout = 2_000
            connection.readTimeout = 2_000
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

    private fun InputStream.readUtf8Text(): String =
        BufferedReader(InputStreamReader(this, Charsets.UTF_8)).use { it.readText() }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun decodeFeed(text: String): FeedResponseDto = json.decodeFromString<FeedResponseDto>(text)

        fun decodeWallet(text: String): WalletDto = json.decodeFromString<WalletDto>(text)

        fun decodeApprovedPets(text: String): ApprovedPetsResponseDto =
            json.decodeFromString<ApprovedPetsResponseDto>(text)

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
