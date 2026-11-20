package com.mangustc.mdnotes.domain.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SearchQueryTest {

    @Test
    fun parse_emptyAndBlank_returnsEmptyQuery() {
        val empty = SearchQuery.parse("")
        val blank = SearchQuery.parse("   \t\n  ")

        assertTrue(empty.isEmpty)
        assertTrue(blank.isEmpty)
        assertNull(empty.buildFtsMatchQuery())
        assertNull(blank.buildFtsMatchQuery())
    }

    @Test
    fun parse_positiveAndNegatedTags_supportsUnquotedAndQuotedWithSpaces() {
        val q = SearchQuery.parse(
            """tag:work tag:"work in progress" -tag:archive -tag:"old project"""",
        )

        assertFalse(q.isEmpty)
        assertEquals(listOf("work", "work in progress"), q.tagFilters)
        assertEquals(listOf("archive", "old project"), q.negatedTagFilters)
        assertEquals(listOf("work", "work in progress"), q.positiveTagLikes())
        assertEquals(listOf("archive", "old project"), q.negatedTagLikes())
        assertNull(q.buildFtsMatchQuery())
    }

    @Test
    fun parse_propertyAndNameFilters_extractsAllFields() {
        val q = SearchQuery.parse(
            """[status:draft] [author:Ada] name:"Proper nouns" -name:old""",
        )

        assertEquals(mapOf("status" to "draft", "author" to "Ada"), q.propFilters)
        assertEquals("Proper nouns", q.nameFilter)
        assertEquals("old", q.negatedNameFilter)
        assertTrue(q.bodyTerms.isEmpty())
        assertTrue(q.exactBodyTerms.isEmpty())
    }

    @Test
    fun parse_combinedQuery_separatesExactBodyUnquotedBodyAndNegatedTerms() {
        val q = SearchQuery.parse(
            """tag:work name:history "jelly beans" sweet snack -sour -"very bitter"""",
        )

        assertEquals(listOf("work"), q.tagFilters)
        assertEquals("history", q.nameFilter)
        assertEquals(listOf("jelly beans"), q.exactBodyTerms)
        assertEquals(listOf("sweet", "snack"), q.bodyTerms)
        assertEquals(listOf("very bitter", "sour"), q.negatedBodyTerms)
        assertEquals(""""jelly beans" sweet* snack*""", q.buildFtsMatchQuery())
    }

    @Test
    fun parse_danglingHyphen_doesNotAddEmptyNegatedTerm() {
        val q = SearchQuery.parse("tag:work -")

        assertEquals(listOf("work"), q.tagFilters)
        assertTrue(q.negatedBodyTerms.isEmpty())
        assertTrue(q.bodyTerms.isEmpty())
    }

    @Test
    fun buildFtsMatchQuery_formatsPrefixAndExactTermsWithSpaceAndLowercase() {
        assertEquals("proper*", SearchQuery.parse("Proper").buildFtsMatchQuery())
        assertEquals("proper* nouns*", SearchQuery.parse("Proper nouns").buildFtsMatchQuery())
        assertEquals(
            """"jelly beans"""",
            SearchQuery.parse(""""jelly beans"""").buildFtsMatchQuery()
        )
        assertEquals(""""jelly"""", SearchQuery.parse(""""jelly"""").buildFtsMatchQuery())
        assertEquals(
            "or* not* and* near*",
            SearchQuery.parse("OR NOT AND NEAR").buildFtsMatchQuery()
        )
    }

    @Test
    fun buildFtsMatchQuery_sanitizesSpecialCharsAndDropsPunctuationOnlyTerms() {
        assertEquals(
            """"12 30" "hello world"""",
            SearchQuery.parse("12:30 hello-world").buildFtsMatchQuery()
        )
        assertEquals("c*", SearchQuery.parse("C++").buildFtsMatchQuery())
        assertNull(SearchQuery.parse("--- *** ::: ()").buildFtsMatchQuery())
    }

    @Test
    fun sanitizeFtsTerm_replacesSpecialCharsCollapsesSpacesAndLowercases() {
        assertEquals("foo bar", SearchQuery.sanitizeFtsTerm("Foo:Bar*"))
        assertEquals("don t", SearchQuery.sanitizeFtsTerm("Don't"))
        assertEquals("", SearchQuery.sanitizeFtsTerm("---\"'*^:()+"))
    }
}
