package com.mangustc.mdnotes.data.project

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.mangustc.mdnotes.data.database.NoteDb
import com.mangustc.mdnotes.data.database.NoteEntity
import com.mangustc.mdnotes.data.database.ProjectEntity
import com.mangustc.mdnotes.domain.models.DomainFile
import com.mangustc.mdnotes.domain.models.Project
import com.mangustc.mdnotes.domain.models.SearchQuery
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class NoteSearchFtsTest {

    private lateinit var db: NoteDb
    private lateinit var project: Project
    private var projectId: Long = 0

    @BeforeTest
    fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder<NoteDb>()
            .setDriver(BundledSQLiteDriver())
            .build()

        project = Project(
            name = "vault",
            rootDomainFile = DomainFile(PlatformFile("/test/vault")),
            notesRelativePath = Project.DefaultNotesRelativePath,
            assetsRelativePath = Project.DefaultAssetsRelativePath,
        )
        projectId = db.projectDao().insertProject(
            ProjectEntity(name = project.name, rootPath = project.rootDomainFile.path),
        )
    }

    @AfterTest
    fun tearDown() {
        db.close()
    }

    private suspend fun addNote(
        name: String,
        body: String,
        tags: List<String> = emptyList(),
        lastModified: Long = 100L,
    ) {
        db.noteDao().upsertNoteWithTags(
            note = NoteEntity(
                projectId = projectId,
                uri = "/test/vault/notes/$name.md",
                name = name,
                lastModified = lastModified,
                body = body,
            ),
            tags = tags,
        )
    }

    private suspend fun searchNames(rawQuery: String): Set<String> {
        val query = SearchQuery.parse(rawQuery)
        val rawSql = query.buildRoomRawQuery(project)
        return db.noteDao().observeNotes(rawSql).first().map { it.note.name }.toSet()
    }

    @Test
    fun multiWordAndCrossColumnSearch_findsProperNounsWithText() = runTest {
        addNote(name = "Proper nouns", body = "text")
        addNote(name = "Other note", body = "something else")

        assertEquals(setOf("Proper nouns"), searchNames("proper"))
        assertEquals(setOf("Proper nouns"), searchNames("proper nouns"))
        assertEquals(setOf("Proper nouns"), searchNames("proper text"))
        assertEquals(emptySet(), searchNames("roper")) // FTS4 unicode61 is word-prefix only
    }

    @Test
    fun tagsAndBodyTerms_matchSpec() = runTest {
        addNote(name = "Candy", body = "jelly and beans", tags = listOf("work", "daily"))
        addNote(name = "Work only", body = "jellyfish", tags = listOf("work"))
        addNote(name = "Daily only", body = "beans", tags = listOf("daily"))

        assertEquals(setOf("Candy"), searchNames("tag:work tag:daily"))
        assertEquals(setOf("Candy"), searchNames("tag:work jelly beans"))
        assertEquals(setOf("Work only"), searchNames("tag:work -tag:daily"))
    }

    @Test
    fun exactQuotedPhrase_matchesOnlyAdjacentPhrase() = runTest {
        addNote(name = "History of snacks", body = "We love jelly beans very much")
        addNote(name = "History of food", body = "We love jelly and beans separately")
        addNote(name = "Other", body = "jelly beans")

        assertEquals(setOf("History of snacks", "Other"), searchNames("\"jelly beans\""))
        assertEquals(setOf("History of snacks"), searchNames("name:history \"jelly beans\""))
        assertEquals(setOf("Other"), searchNames("-name:history \"jelly beans\""))
    }

    @Test
    fun negatedBodyTerms_excludeMatchesInBothNameAndBody() = runTest {
        addNote(name = "Jelly recipe", body = "sugar and fruit")
        addNote(name = "Dessert", body = "sweet jelly beans")
        addNote(name = "Plain", body = "water")

        assertEquals(setOf("Plain"), searchNames("-jelly"))
        assertEquals(setOf("Jelly recipe", "Plain"), searchNames("-\"jelly beans\""))
    }

    @Test
    fun specialCharactersFtsKeywordsAndDanglingHyphen_doNotCrash() = runTest {
        addNote(name = "Schedule", body = "Meeting at 12:30 OR NOT AND hello-world")

        assertEquals(setOf("Schedule"), searchNames("12:30"))
        assertEquals(setOf("Schedule"), searchNames("hello-world"))
        assertEquals(setOf("Schedule"), searchNames("OR NOT AND"))
        assertEquals(setOf("Schedule"), searchNames("schedule -"))
    }

    @Test
    fun noteIdIsNotIndexedInFts() = runTest {
        addNote(name = "Alpha", body = "no digits here")

        assertEquals(emptySet(), searchNames("1"))
    }
}
