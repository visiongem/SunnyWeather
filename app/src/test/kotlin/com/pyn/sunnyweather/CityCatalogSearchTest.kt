package com.pyn.sunnyweather

import com.pyn.sunnyweather.data.local.CityCatalog
import java.io.IOException
import java.util.Locale
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class CityCatalogSearchTest {
    @Test fun `full pinyin partial spelling and separators match the same city`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val catalog = CityCatalog(dispatcher, dispatcher) { records }
        for (query in listOf("深圳", "深圳市", "shenzhen", "shenzhenshi", "shen", "zhen", " SHEN ZHEN ", "ＳＨＥＮＺＨＥＮ", "shen-zhen", "shen'zhen")) {
            assertTrue(query, catalog.search(query).any { it.id == "440300" })
        }
        assertEquals("440300", catalog.search("shenzhen").first().id)
        assertEquals("440300", catalog.search("深圳").first().id)
    }

    @Test fun `ambiguous initials retain every city and rank names ahead of parent addresses`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val catalog = CityCatalog(dispatcher, dispatcher) { records }
        // All three names shorten to sz; 福田 only matches 深圳 in its parent address.
        assertEquals(listOf("131182", "320500", "440300", "440304"), catalog.search("SZ").map { it.id })
        assertEquals(listOf("440300", "440304"), catalog.search("shenzhen").map { it.id })
        assertEquals(setOf("440300", "440304"), catalog.search("guangdong").map { it.id }.toSet())
    }

    @Test fun `empty unsupported and excessively long input never matches the entire catalog`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val catalog = CityCatalog(dispatcher, dispatcher) { records }
        for (query in listOf("", " \t\n", " - ' ’ ", "😀", "👨‍👩‍👧‍👦", "不存在", "shenzhne", "شنتشن", "0", "\u200B", "s".repeat(10_000))) {
            assertTrue(query.take(40), catalog.search(query).isEmpty())
        }
    }

    @Test fun `pinyin umlaut variants and casing do not depend on device locale`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val catalog = CityCatalog(dispatcher, dispatcher) { records }
        val originalLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            for (query in listOf("LVLIANG", "LÜLIANG", "lu:liang", "LL")) {
                assertEquals(query, "141100", catalog.search(query).first().id)
            }
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    @Test fun `missing or invalid pinyin preserves Chinese search and duplicates remain deduplicated`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val rows = """440300	深圳市	广东省深圳市	114	22
440304	福田区	广东省深圳市福田区	114	22	BAD!	BAD!
440300	深圳市	广东省深圳市	114	22	shen zhen shi	guang dong sheng shen zhen shi"""
        val catalog = CityCatalog(dispatcher, dispatcher) { rows }
        assertEquals(listOf("440300", "440304"), catalog.search("深圳").map { it.id })
        assertTrue(catalog.search("shenzhen").isEmpty())
        assertEquals("440304", catalog.search("福田").single().id)
    }

    @Test fun `empty query avoids loading and failed load can retry then reuse the index`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        var reads = 0
        val catalog = CityCatalog(dispatcher, dispatcher) {
            reads++
            if (reads == 1) throw IOException("unavailable")
            records
        }
        assertTrue(catalog.search(" -- ").isEmpty())
        assertEquals(0, reads)
        try {
            catalog.search("sz")
            fail("A failed load must reach the caller")
        } catch (_: IOException) { }
        assertTrue(catalog.search("sz").isNotEmpty())
        assertEquals("440300", catalog.search("shenzhen").first().id)
        assertEquals(2, reads)
    }

    private val records = """440304	福田区	广东省深圳市福田区	114	22	fu tian qu	guang dong sheng shen zhen shi fu tian qu
440300	深圳市	广东省深圳市	114	22	shen zhen shi	guang dong sheng shen zhen shi
320500	苏州市	江苏省苏州市	120	31	su zhou shi	jiang su sheng su zhou shi
131182	深州市	河北省衡水市深州市	115	38	shen zhou shi	he bei sheng heng shui shi shen zhou shi
141100	吕梁市	山西省吕梁市	111	37	lv liang shi	shan xi sheng lv liang shi"""
}
