package com.weathersnap.data.cache

import android.util.LruCache
import com.weathersnap.domain.model.City
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Caches city-suggestion lookups by normalized query, so retyping the same prefix
 * (or selecting → coming back) does not hit the geocoding API again.
 */
@Singleton
class CitySuggestionCache @Inject constructor() {

    private val cache = LruCache<String, List<City>>(MAX_ENTRIES)

    fun get(query: String): List<City>? = cache.get(normalize(query))

    fun put(query: String, results: List<City>) {
        cache.put(normalize(query), results)
    }

    private fun normalize(q: String) = q.trim().lowercase()

    companion object { private const val MAX_ENTRIES = 64 }
}
