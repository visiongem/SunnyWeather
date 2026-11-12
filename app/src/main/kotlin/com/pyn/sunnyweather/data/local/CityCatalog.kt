package com.pyn.sunnyweather.data.local

import com.pyn.sunnyweather.model.City
import java.text.Normalizer
import java.util.Locale
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class CityCatalog(
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val computation: CoroutineDispatcher = Dispatchers.Default,
    private val readText: () -> String,
) {
    private val mutex = Mutex()
    private var entries: List<SearchEntry>? = null

    suspend fun search(query: String): List<City> = withContext(computation) {
        val term = normalize(query)
        if (term.isEmpty()) return@withContext emptyList()
        val all = mutex.withLock {
            entries ?: parseEntries(withContext(io) { readText() }).also { entries = it }
        }
        all.mapNotNull { entry ->
            ensureActive()
            entry.rank(term)?.let { rank -> entry.city to rank }
        }.sortedWith(compareBy<Pair<City, Int>> { it.second }.thenBy { it.first.id })
            .map { it.first }
    }

    private data class SearchEntry(
        val city: City,
        val nameKeys: Set<String>,
        val addressKeys: Set<String>,
    ) {
        fun rank(term: String): Int? = when {
            term in nameKeys -> 0
            nameKeys.any { it.startsWith(term) } -> 1
            nameKeys.any { term in it } -> 2
            addressKeys.any { term in it } -> 3
            else -> null
        }
    }

    companion object {
        private val administrativeSuffixes = listOf("特别行政区", "自治区", "自治州", "自治县", "地区", "省", "市", "区", "县", "盟")
        private val syllablePattern = Regex("[a-z]+")

        fun parse(text: String): List<City> = parseEntries(text).map { it.city }

        private fun parseEntries(text: String): List<SearchEntry> = text.lineSequence().mapNotNull {
            val fields = it.split('\t')
            // Five-column data remains readable; only seven-column data has pinyin search keys.
            if (fields.size != 5 && fields.size != 7) return@mapNotNull null
            val lng = fields[3].toDoubleOrNull() ?: return@mapNotNull null
            val lat = fields[4].toDoubleOrNull() ?: return@mapNotNull null
            if (lng !in -180.0..180.0 || lat !in -90.0..90.0 || fields[2].isBlank()) return@mapNotNull null
            val city = City(fields[0], fields[1].ifBlank { fields[2] }, fields[2], lng, lat)
            SearchEntry(
                city,
                keys(city.name, fields.getOrNull(5), includeShortName = true),
                keys(city.address, fields.getOrNull(6), includeShortName = false),
            )
        }.distinctBy { it.city.id }.toList().also { require(it.isNotEmpty()) { "Empty city catalog" } }

        private fun keys(text: String, pinyin: String?, includeShortName: Boolean): Set<String> = buildSet {
            add(normalize(text))
            val suffix = if (includeShortName) administrativeSuffixes.firstOrNull {
                text.endsWith(it) && text.length > it.length
            } else null
            if (suffix != null) add(normalize(text.removeSuffix(suffix)))
            val syllables = pinyin?.split(' ')?.takeIf { words -> words.all { syllablePattern.matches(it) } }
                ?: return@buildSet
            add(syllables.joinToString(""))
            add(syllables.joinToString("") { it.first().toString() })
            // Strip the administrative suffix only when syllables map one-to-one to the name.
            if (suffix != null && syllables.size == text.length) {
                val short = syllables.dropLast(suffix.length)
                add(short.joinToString(""))
                add(short.joinToString("") { it.first().toString() })
            }
        }

        private fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFKC)
            .lowercase(Locale.ROOT)
            .replace("u:", "v")
            .replace('ü', 'v')
            .filterNot { it.isWhitespace() || it == '\'' || it == '’' || it == '-' }
    }
}
