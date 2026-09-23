package com.jumincho.beatingyesterday.core.food

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.io.InterruptedIOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * [FoodSearchService] backed by the Food Safety Korea OpenAPI nutrition service `I2790`.
 *
 * Requests have the form
 * `{baseUrl}/{apiKey}/I2790/json/{start}/{end}/DESC_KOR={query}` where the query is a
 * percent-encoded path segment. The key is part of the URL, so request URLs must never be logged.
 *
 * @param apiKey the personal service key issued by the portal; must not be blank.
 * @param pageSize how many foods to request (the API pages with 1-based, inclusive indexes).
 */
class FoodSafetyKoreaClient(
    private val apiKey: String,
    private val httpClient: OkHttpClient = defaultHttpClient(),
    private val baseUrl: HttpUrl = DEFAULT_BASE_URL,
    private val pageSize: Int = DEFAULT_PAGE_SIZE,
) : FoodSearchService {

    init {
        require(apiKey.isNotBlank()) { "apiKey must not be blank" }
        require(pageSize in 1..MAX_PAGE_SIZE) { "pageSize must be within 1..$MAX_PAGE_SIZE but was $pageSize" }
    }

    override suspend fun search(query: String): FoodSearchResult {
        val trimmed = query.trim()
        require(trimmed.isNotEmpty()) { "query must not be blank" }
        val request = Request.Builder().url(searchUrl(trimmed)).get().build()
        val response = try {
            httpClient.newCall(request).fetch()
        } catch (e: InterruptedIOException) {
            return FoodSearchResult.Failure(FoodSearchError.Timeout)
        } catch (e: IOException) {
            return FoodSearchResult.Failure(FoodSearchError.Network)
        }
        if (response.code !in 200..299) return FoodSearchResult.Failure(FoodSearchError.Http(response.code))
        return I2790ResponseParser.parse(response.body)
    }

    private fun searchUrl(query: String): HttpUrl = baseUrl.newBuilder()
        .addPathSegment(apiKey)
        .addPathSegment(SERVICE_ID)
        .addPathSegment("json")
        .addPathSegment("1")
        .addPathSegment(pageSize.toString())
        .addPathSegment("DESC_KOR=$query")
        .build()

    private class FetchedResponse(val code: Int, val body: String)

    /** Executes the call asynchronously, cancelling it if the coroutine is cancelled. */
    private suspend fun Call.fetch(): FetchedResponse = suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { cancel() }
        enqueue(
            object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    val fetched = try {
                        response.use { FetchedResponse(it.code, it.body.string()) }
                    } catch (e: IOException) {
                        continuation.resumeWithException(e)
                        return
                    }
                    continuation.resume(fetched)
                }
            },
        )
    }

    companion object {
        /** Production endpoint. */
        val DEFAULT_BASE_URL: HttpUrl = "https://openapi.foodsafetykorea.go.kr/api/".toHttpUrl()

        /** Service id of the food nutrition database. */
        const val SERVICE_ID = "I2790"

        /** Default number of results requested per search. */
        const val DEFAULT_PAGE_SIZE = 20

        /** Largest page size this client allows. */
        const val MAX_PAGE_SIZE = 100

        /** An [OkHttpClient] with timeouts suited to an interactive search. */
        fun defaultHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .build()
    }
}
