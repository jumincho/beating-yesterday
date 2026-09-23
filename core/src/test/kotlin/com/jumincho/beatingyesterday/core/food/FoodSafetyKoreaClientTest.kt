package com.jumincho.beatingyesterday.core.food

import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.SocketEffect
import okhttp3.OkHttpClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.TimeUnit

/** Exercises the HTTP side of the client against a local MockWebServer with synthetic bodies. */
class FoodSafetyKoreaClientTest {

    private val server = MockWebServer()

    @BeforeEach
    fun startServer() {
        server.start()
    }

    @AfterEach
    fun stopServer() {
        server.close()
    }

    private fun client(httpClient: OkHttpClient = OkHttpClient()) = FoodSafetyKoreaClient(
        apiKey = "test-key",
        httpClient = httpClient,
        baseUrl = server.url("/api/"),
        pageSize = 5,
    )

    private val successBody = """
        {"I2790": {"total_count": "1",
                   "row": [{"DESC_KOR": "Test Stew", "SERVING_SIZE": "300", "NUTR_CONT1": "412.5", "MAKER_NAME": ""}],
                   "RESULT": {"CODE": "INFO-000", "MSG": "OK"}}}
    """.trimIndent()

    @Test
    fun `puts the key, paging and the encoded query in the path`() = runTest {
        server.enqueue(MockResponse(body = successBody))

        client().search("  김치 찌개 ")

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals(
            "/api/test-key/I2790/json/1/5/DESC_KOR=%EA%B9%80%EC%B9%98%20%EC%B0%8C%EA%B0%9C",
            request.url.encodedPath,
        )
    }

    @Test
    fun `returns parsed foods`() = runTest {
        server.enqueue(MockResponse(body = successBody))

        assertEquals(
            FoodSearchResult.Success(listOf(FoodItem("Test Stew", 412.5, "300", null)), totalCount = 1),
            client().search("stew"),
        )
    }

    @Test
    fun `maps HTTP errors`() = runTest {
        server.enqueue(MockResponse(code = 503, body = "unavailable"))

        assertEquals(FoodSearchResult.Failure(FoodSearchError.Http(503)), client().search("stew"))
    }

    @Test
    fun `maps a stalled response to a timeout`() = runTest {
        server.enqueue(MockResponse.Builder().onResponseStart(SocketEffect.Stall).build())
        val impatient = OkHttpClient.Builder().readTimeout(200, TimeUnit.MILLISECONDS).build()

        assertEquals(FoodSearchResult.Failure(FoodSearchError.Timeout), client(impatient).search("stew"))
    }

    @Test
    fun `maps an unreachable server to a network error`() = runTest {
        val client = client()
        server.close()

        assertEquals(FoodSearchResult.Failure(FoodSearchError.Network), client.search("stew"))
    }

    @Test
    fun `rejects a blank key or query`() = runTest {
        assertThrows<IllegalArgumentException> { FoodSafetyKoreaClient(apiKey = " ") }
        assertThrows<IllegalArgumentException> { client().search("   ") }
    }
}
