package com.mangustc.mdnotes.domain.models

import com.mangustc.mdnotes.domain.models.FrontMatter.FrontMatterValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.time.Instant

class NoteDocumentAndFrontMatterTest {

    // NoteDocument.parse & serialize

    @Test
    fun parseAndSerialize_plainNoteWithoutFrontMatter_roundTripsVerbatim() {
        val raw = "# Hello\n\nJust a regular markdown note.\n"

        val doc = NoteDocument.parse(raw)

        assertEquals(FrontMatter.Empty, doc.frontMatter)
        assertEquals(raw, doc.body)
        assertEquals(raw, doc.serialize())
    }

    @Test
    fun parseAndSerialize_emptyString_roundTrips() {
        val doc = NoteDocument.parse("")

        assertEquals(FrontMatter.Empty, doc.frontMatter)
        assertEquals("", doc.body)
        assertEquals("", doc.serialize())
    }

    @Test
    fun parseAndSerialize_fullFrontMatterWithScalarsListsAndTagsWithSpaces_roundTrips() {
        val original = NoteDocument(
            frontMatter = FrontMatter(
                fields = linkedMapOf(
                    FrontMatter.CREATED_AT_FIELD to FrontMatterValue.Scalar("2026-03-15T10:20:30Z"),
                    FrontMatter.TAGS_FIELD to FrontMatterValue.StringList(
                        listOf("work", "work in progress", FrontMatter.PINNED_TAG),
                    ),
                    "url" to FrontMatterValue.Scalar("https://example.com:8080/path"),
                ),
            ),
            body = "# Title\n\nParagraph 1\n\n---\n\nParagraph after horizontal rule",
        )

        val serialized = original.serialize()
        val parsed = NoteDocument.parse(serialized)

        assertEquals(original, parsed)
        assertEquals(
            listOf("work", "work in progress", FrontMatter.PINNED_TAG),
            parsed.frontMatter.tags,
        )
        assertEquals("2026-03-15T10:20:30Z", parsed.frontMatter.createdAt)
        assertEquals(
            FrontMatterValue.Scalar("https://example.com:8080/path"),
            parsed.frontMatter["url"],
        )
    }

    @Test
    fun parse_leadingWhitespaceBeforeOpeningFence_parsesFrontMatter() {
        val raw = "  \n---\ntitle: Hello\n---\nBody text"

        val doc = NoteDocument.parse(raw)

        assertEquals(FrontMatterValue.Scalar("Hello"), doc.frontMatter["title"])
        assertEquals("Body text", doc.body)
    }

    @Test
    fun parse_emptyFrontMatterBlock_returnsEmptyFrontMatterAndBody() {
        val raw = "---\n---\nBody line"

        val doc = NoteDocument.parse(raw)

        assertEquals(FrontMatter.Empty, doc.frontMatter)
        assertEquals("Body line", doc.body)
    }

    @Test
    fun parse_unclosedFrontMatter_preservesOriginalContentVerbatim() {
        val raw = "---\ntitle: Draft\ntags:\n- note\nNo closing delimiter"

        val doc = NoteDocument.parse(raw)

        assertEquals(FrontMatter.Empty, doc.frontMatter)
        assertEquals(raw, doc.body)
    }

    @Test
    fun parse_openingLineWithExtraCharacters_treatedAsPlainBody() {
        val raw = "---not-frontmatter\ntitle: Something\n---\nBody"

        val doc = NoteDocument.parse(raw)

        assertEquals(FrontMatter.Empty, doc.frontMatter)
        assertEquals(raw, doc.body)
    }

    @Test
    fun parse_malformedYamlInsideFences_preservesOriginalContentVerbatim() {
        val raw = "---\ntitle: Valid\nthis line has no colon\n---\nImportant user body"

        val doc = NoteDocument.parse(raw)

        assertEquals(FrontMatter.Empty, doc.frontMatter)
        assertEquals(raw, doc.body)
        assertEquals(raw, doc.serialize())
    }

    @Test
    fun serialize_frontMatterWithEmptyBody_keepsTrailingNewlineAfterFence() {
        val doc = NoteDocument(
            frontMatter = FrontMatter(mapOf("title" to FrontMatterValue.Scalar("Empty"))),
            body = "",
        )

        val serialized = doc.serialize()
        assertEquals("---\ntitle: Empty\n---\n", serialized)
        assertEquals(doc, NoteDocument.parse(serialized))
    }

    // FrontMatter.parse

    @Test
    fun frontMatterParse_blankInput_returnsEmpty() {
        assertEquals(FrontMatter.Empty, FrontMatter.parse(""))
        assertEquals(FrontMatter.Empty, FrontMatter.parse("   \n\t\n  "))
    }

    @Test
    fun frontMatterParse_skipsCommentsAndBlankLines() {
        val text = """
            # top comment

            author: Ada Lovelace
            # middle comment
            tags:
            - math
            - computing history
        """.trimIndent()

        val fm = FrontMatter.parse(text)

        assertEquals(FrontMatterValue.Scalar("Ada Lovelace"), fm["author"])
        assertEquals(listOf("math", "computing history"), fm.tags)
    }

    @Test
    fun frontMatterParse_emptyListFollowedByScalar_parsesBothCorrectly() {
        val text = """
            tags:
            status: draft
        """.trimIndent()

        val fm = FrontMatter.parse(text)

        assertEquals(FrontMatterValue.StringList(emptyList()), fm[FrontMatter.TAGS_FIELD])
        assertEquals(FrontMatterValue.Scalar("draft"), fm["status"])
    }

    @Test
    fun frontMatterParse_lineWithoutColon_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            FrontMatter.parse("valid: ok\nbroken line")
        }
    }

    // FrontMatter helpers & mutations

    @Test
    fun toCreatedAtMillis_validIsoInstant_returnsEpochMillis() {
        val iso = "2026-05-01T08:30:00Z"
        val fm = FrontMatter(mapOf(FrontMatter.CREATED_AT_FIELD to FrontMatterValue.Scalar(iso)))

        assertEquals(Instant.parse(iso).toEpochMilliseconds(), fm.toCreatedAtMillis())
    }

    @Test
    fun toCreatedAtMillis_missingOrInvalid_returnsNull() {
        assertNull(FrontMatter.Empty.toCreatedAtMillis())

        val invalid = FrontMatter(
            mapOf(FrontMatter.CREATED_AT_FIELD to FrontMatterValue.Scalar("not-a-timestamp")),
        )
        assertNull(invalid.toCreatedAtMillis())
    }

    @Test
    fun withTagAndWithoutTag_deduplicatesIgnoresBlankAndPreservesSpaces() {
        val fm = FrontMatter.Empty
            .withTag("my tag")
            .withTag("my tag")
            .withTag("   ")
            .withTag("second")

        assertEquals(listOf("my tag", "second"), fm.tags)
        assertEquals("my tag second", fm.toTagString())

        val removed = fm.withoutTag("my tag").withoutTag("nonexistent")
        assertEquals(listOf("second"), removed.tags)
    }

    @Test
    fun withRenamedKey_preservesKeyOrderAndIgnoresInvalidRenames() {
        val fm = FrontMatter(
            linkedMapOf(
                "first" to FrontMatterValue.Scalar("1"),
                "second" to FrontMatterValue.Scalar("2"),
                "third" to FrontMatterValue.Scalar("3"),
            ),
        )

        val renamed = fm.withRenamedKey("second", "middle")
        assertEquals(listOf("first", "middle", "third"), renamed.fields.keys.toList())
        assertEquals(FrontMatterValue.Scalar("2"), renamed["middle"])

        assertEquals(fm, fm.withRenamedKey("second", "second"))
        assertEquals(fm, fm.withRenamedKey("second", "   "))
    }

    @Test
    fun withoutField_removesCustomFieldButProtectsTagsAndCreatedAt() {
        val fm = FrontMatter(
            linkedMapOf(
                FrontMatter.CREATED_AT_FIELD to FrontMatterValue.Scalar("2026-01-01T00:00:00Z"),
                FrontMatter.TAGS_FIELD to FrontMatterValue.StringList(listOf("a")),
                "custom" to FrontMatterValue.Scalar("val"),
            ),
        )

        val afterProtectedAttempts = fm
            .withoutField(FrontMatter.TAGS_FIELD)
            .withoutField(FrontMatter.CREATED_AT_FIELD)
        assertEquals(fm, afterProtectedAttempts)

        val afterCustomRemoval = fm.withoutField("custom")
        assertNull(afterCustomRemoval["custom"])
        assertEquals(listOf("a"), afterCustomRemoval.tags)
        assertEquals("2026-01-01T00:00:00Z", afterCustomRemoval.createdAt)
    }
}
