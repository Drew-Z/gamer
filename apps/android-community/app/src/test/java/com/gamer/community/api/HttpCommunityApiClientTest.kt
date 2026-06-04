package com.gamer.community.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
                  "createdAt": "2026-06-05T00:00:00.000Z"
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
    fun invalidJsonBecomesFailure() {
        val result = HttpCommunityApiClient.decodeCatching("not-json") {
            HttpCommunityApiClient.decodeFeed(it)
        }

        assertTrue(result is ApiCallResult.Failure)
    }
}
