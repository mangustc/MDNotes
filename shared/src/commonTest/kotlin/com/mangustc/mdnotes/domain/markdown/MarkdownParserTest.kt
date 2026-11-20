package com.mangustc.mdnotes.domain.markdown

import com.mangustc.mdnotes.domain.models.SpanInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarkdownParserTest {

    @Test
    fun parse_emptyText_returnsEmptyList() {
        assertTrue(MarkdownParser.parse("").isEmpty())
    }

    @Test
    fun parse_headingsLevelsOneThroughSix_emitsCorrectLevelsAndRanges() {
        val md = """
            # H1
            ## H2
            ### H3
            #### H4
            ##### H5
            ###### H6
        """.trimIndent()

        val headings = MarkdownParser.parse(md).filterIsInstance<SpanInfo.Heading>()

        assertEquals(listOf(1, 2, 3, 4, 5, 6), headings.map { it.level })
        assertEquals("# H1", md.substring(headings[0].range.start, headings[0].range.end))
        assertEquals("###### H6", md.substring(headings[5].range.start, headings[5].range.end))
    }

    @Test
    fun parse_inlineAndBlockFormatting_emitsExpectedSpans() {
        val md = """
            Text with **bold**, *italic*, and `inline`.

            > Quoted line

            - list entry

            ```kotlin
            val x = 1
            ```
        """.trimIndent()

        val spans = MarkdownParser.parse(md)

        val bold = spans.filterIsInstance<SpanInfo.Bold>().single()
        assertEquals("**bold**", md.substring(bold.range.start, bold.range.end))

        val italic = spans.filterIsInstance<SpanInfo.Italic>().single()
        assertEquals("*italic*", md.substring(italic.range.start, italic.range.end))

        val inlineCode = spans.filterIsInstance<SpanInfo.CodeInline>().single()
        assertEquals("`inline`", md.substring(inlineCode.range.start, inlineCode.range.end))

        assertEquals(1, spans.filterIsInstance<SpanInfo.Blockquote>().size)
        assertEquals(1, spans.filterIsInstance<SpanInfo.ListItem>().size)
        assertEquals(1, spans.filterIsInstance<SpanInfo.CodeBlock>().size)
    }

    @Test
    fun parse_links_extractsLabelsPayloadsRangesAndLinkTypes() {
        val md =
            "See [Docs](https://example.com/a), [Note](<notes/my note.md>), and [File](assets/doc.pdf)."

        val links = MarkdownParser.parse(md).filterIsInstance<SpanInfo.Link>()

        assertEquals(3, links.size)

        val httpLink = links[0]
        assertEquals("Docs", httpLink.label)
        assertEquals("https://example.com/a", httpLink.payload)
        assertEquals("Docs", md.substring(httpLink.labelRange.start, httpLink.labelRange.end))
        assertEquals(
            "https://example.com/a",
            md.substring(httpLink.payloadRange.start, httpLink.payloadRange.end)
        )
        assertEquals(SpanInfo.Link.LinkType.HTTP, httpLink.linkType)

        val noteLink = links[1]
        assertEquals("Note", noteLink.label)
        assertEquals("notes/my note.md", noteLink.payload)
        assertEquals(
            "notes/my note.md",
            md.substring(noteLink.payloadRange.start, noteLink.payloadRange.end)
        )
        assertEquals(SpanInfo.Link.LinkType.NOTE, noteLink.linkType)

        val fileLink = links[2]
        assertEquals("File", fileLink.label)
        assertEquals("assets/doc.pdf", fileLink.payload)
        assertEquals(SpanInfo.Link.LinkType.FILE, fileLink.linkType)
    }

    @Test
    fun parse_images_extractsPayloadWithoutEmittingDuplicateLinkSpan() {
        val md = "Before ![Photo](assets/pic.png) and ![Spaced](<assets/my pic.png>) after"

        val spans = MarkdownParser.parse(md)
        val images = spans.filterIsInstance<SpanInfo.Image>()
        val links = spans.filterIsInstance<SpanInfo.Link>()

        assertEquals(listOf("assets/pic.png", "assets/my pic.png"), images.map { it.payload })
        assertTrue(links.isEmpty())
    }

    @Test
    fun parse_yamlFrontMatter_excludesSpansInsideFrontMatter() {
        val md = """
            ---
            title: Demo
            tags:
            - work
            - daily
            ---
            # Real Heading
            - Real item
        """.trimIndent()

        val spans = MarkdownParser.parse(md)
        val fmEnd = md.indexOf("---", startIndex = 4) + 3

        assertTrue(spans.isNotEmpty())
        assertTrue(spans.all { it.range.start >= fmEnd })
        assertEquals(1, spans.filterIsInstance<SpanInfo.Heading>().size)
        assertEquals(1, spans.filterIsInstance<SpanInfo.ListItem>().size)
    }

    @Test
    fun stripAttachments_removesAllImagesAndLinksAndTrimsResult() {
        val md =
            "  ![pic](assets/a.png) Hello [link](https://example.com) world ![pic2](assets/b.png)  "
        val spans = MarkdownParser.parse(md)

        val stripped = MarkdownParser.stripAttachments(md, spans)

        assertEquals("Hello  world", stripped)
    }
}
