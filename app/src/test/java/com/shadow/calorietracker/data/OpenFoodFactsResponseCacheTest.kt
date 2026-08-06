package com.shadow.calorietracker.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class OpenFoodFactsResponseCacheTest {
    private lateinit var cache: OpenFoodFactsResponseCache

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        cache = OpenFoodFactsResponseCache(context)
        cache.entries().forEach { it.delete() }
    }

    @Test
    fun `repeat search uses cached raw response without another request`() = runBlocking {
        var requests = 0
        val response =
            """{"products":[{"code":"12345678","product_name":"Cached drink","nutriments":{"energy-kcal_100g":42,"proteins_100g":0,"carbohydrates_100g":10,"fat_100g":0}}]}"""
        val client = OpenFoodFactsClient(cache) { url ->
            requests += 1
            StubConnection(url, response)
        }

        assertEquals(1, client.search("drink", "en").size)
        assertEquals(1, client.search("drink", "en").size)
        assertEquals(1, requests)
        assertEquals(1, cache.entries().size)
    }

    private class StubConnection(url: String, private val body: String) : HttpURLConnection(URL(url)) {
        override fun connect() = Unit
        override fun disconnect() = Unit
        override fun usingProxy(): Boolean = false
        override fun getResponseCode(): Int = HTTP_OK
        override fun getInputStream() = ByteArrayInputStream(body.toByteArray())
    }
}
