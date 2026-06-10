package com.gamer.community.generation

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class PetPreviewDownloadResult {
    data class Success(val bytes: ByteArray) : PetPreviewDownloadResult() {
        override fun equals(other: Any?): Boolean =
            other is Success && bytes.contentEquals(other.bytes)

        override fun hashCode(): Int = bytes.contentHashCode()
    }

    data class Failure(val reason: String) : PetPreviewDownloadResult()
}

class FantasyPetPreviewDownloader(
    private val connectTimeoutMillis: Int = 10_000,
    private val readTimeoutMillis: Int = 45_000,
    private val connectionFactory: (URL) -> HttpURLConnection = { url ->
        url.openConnection() as HttpURLConnection
    }
) {
    suspend fun download(previewUrl: String): PetPreviewDownloadResult =
        withContext(Dispatchers.IO) {
            downloadBlocking(previewUrl)
        }

    fun downloadBlocking(previewUrl: String): PetPreviewDownloadResult {
        val url = try {
            URL(previewUrl.trim())
        } catch (_: Exception) {
            return PetPreviewDownloadResult.Failure("invalid_preview_url")
        }

        if (url.protocol.lowercase() !in setOf("http", "https")) {
            return PetPreviewDownloadResult.Failure("preview_url_must_be_http_or_https")
        }

        var connection: HttpURLConnection? = null
        return try {
            connection = connectionFactory(url).apply {
                requestMethod = "GET"
                connectTimeout = connectTimeoutMillis
                readTimeout = readTimeoutMillis
                setRequestProperty("Accept", "image/*")
            }

            val status = connection.responseCode
            if (status !in 200..299) {
                PetPreviewDownloadResult.Failure("http_$status")
            } else {
                PetPreviewDownloadResult.Success(
                    connection.inputStream.use { input -> input.readBytes() }
                )
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            PetPreviewDownloadResult.Failure(error.message ?: error::class.java.simpleName)
        } finally {
            connection?.disconnect()
        }
    }
}
