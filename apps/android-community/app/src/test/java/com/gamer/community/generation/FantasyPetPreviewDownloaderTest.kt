package com.gamer.community.generation

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FantasyPetPreviewDownloaderTest {
    @Test
    fun downloadBlockingAppliesTimeoutsAndReadsPreviewBytes() {
        val connection = FakeHttpURLConnection(
            responseStatus = 200,
            responseBytes = "preview".toByteArray()
        )
        val downloader = FantasyPetPreviewDownloader(
            connectTimeoutMillis = 1_234,
            readTimeoutMillis = 5_678,
            connectionFactory = { connection }
        )

        val result = downloader.downloadBlocking("https://example.com/candidate.png")

        assertTrue(result is PetPreviewDownloadResult.Success)
        assertArrayEquals(
            "preview".toByteArray(),
            (result as PetPreviewDownloadResult.Success).bytes
        )
        assertEquals(1_234, connection.connectTimeout)
        assertEquals(5_678, connection.readTimeout)
        assertEquals("GET", connection.requestMethod)
        assertEquals("image/*", connection.getRequestProperty("Accept"))
        assertTrue(connection.disconnected)
    }

    @Test
    fun downloadBlockingRejectsNonHttpPreviewUrls() {
        var openedConnection = false
        val downloader = FantasyPetPreviewDownloader(
            connectionFactory = {
                openedConnection = true
                FakeHttpURLConnection()
            }
        )

        val result = downloader.downloadBlocking("file:///C:/secret/runs/job/output.png")

        assertEquals(
            PetPreviewDownloadResult.Failure("preview_url_must_be_http_or_https"),
            result
        )
        assertFalse(openedConnection)
    }

    @Test
    fun downloadBlockingReturnsFailureForHttpErrors() {
        val downloader = FantasyPetPreviewDownloader(
            connectionFactory = {
                FakeHttpURLConnection(responseStatus = 404)
            }
        )

        val result = downloader.downloadBlocking("http://127.0.0.1:8765/missing.png")

        assertEquals(PetPreviewDownloadResult.Failure("http_404"), result)
    }
}

private class FakeHttpURLConnection(
    private val responseStatus: Int = 200,
    private val responseBytes: ByteArray = ByteArray(0)
) : HttpURLConnection(URL("https://example.com")) {
    var disconnected = false

    override fun connect() = Unit

    override fun disconnect() {
        disconnected = true
    }

    override fun usingProxy(): Boolean = false

    override fun getResponseCode(): Int = responseStatus

    override fun getInputStream(): InputStream = ByteArrayInputStream(responseBytes)
}
