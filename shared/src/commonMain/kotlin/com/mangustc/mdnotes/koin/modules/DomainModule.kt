package com.mangustc.mdnotes.koin.modules

import com.mangustc.mdnotes.domain.usecases.editor.ApplyEditorEventUseCase
import com.mangustc.mdnotes.domain.usecases.editor.GetRealSpanInfoLinkTypeUseCase
import com.mangustc.mdnotes.domain.usecases.linkPreview.GetLinkPreviewUseCase
import com.mangustc.mdnotes.domain.usecases.messenger.GetMessagesUseCase
import com.mangustc.mdnotes.domain.usecases.messenger.GetPinnedMessagesUseCase
import com.mangustc.mdnotes.domain.usecases.messenger.SendNoteUseCase
import com.mangustc.mdnotes.domain.usecases.notes.CreateNoteUseCase
import com.mangustc.mdnotes.domain.usecases.notes.DeleteNoteUseCase
import com.mangustc.mdnotes.domain.usecases.notes.GetNoteUseCase
import com.mangustc.mdnotes.domain.usecases.notes.RenameNoteUseCase
import com.mangustc.mdnotes.domain.usecases.notes.SaveNoteTextUseCase
import com.mangustc.mdnotes.domain.usecases.notes.ToggleNoteTagUseCase
import com.mangustc.mdnotes.domain.usecases.project.CopyToAssetsUseCase
import com.mangustc.mdnotes.domain.usecases.project.GetAllTagsUseCase
import com.mangustc.mdnotes.domain.usecases.project.GetNotesUseCase
import com.mangustc.mdnotes.domain.usecases.project.GetProjectFileUseCase
import com.mangustc.mdnotes.domain.usecases.project.LoadSavedProjectUseCase
import com.mangustc.mdnotes.domain.usecases.project.SelectProjectUseCase
import com.mangustc.mdnotes.domain.usecases.project.SyncDatabaseUseCase
import com.mangustc.mdnotes.domain.usecases.search.ApplySearchEventUseCase
import com.mangustc.mdnotes.domain.usecases.settings.GetSettingsUseCase
import com.mangustc.mdnotes.domain.usecases.settings.SetSettingsUseCase
import com.mangustc.mdnotes.domain.usecases.sync.SyncProjectUseCase
import org.koin.dsl.module
import org.koin.plugin.module.dsl.factory

val domainModule = module {
    factory<SyncProjectUseCase>()
    factory<GetMessagesUseCase>()
    factory<GetPinnedMessagesUseCase>()
    factory<LoadSavedProjectUseCase>()
    factory<SelectProjectUseCase>()
    factory<SetSettingsUseCase>()
    factory<GetSettingsUseCase>()
    factory<SyncDatabaseUseCase>()
    factory<GetNotesUseCase>()
    factory<CreateNoteUseCase>()
    factory<GetNoteUseCase>()
    factory<DeleteNoteUseCase>()
    factory<RenameNoteUseCase>()
    factory<ToggleNoteTagUseCase>()
    factory<GetAllTagsUseCase>()
    factory<GetLinkPreviewUseCase>()
    factory<CopyToAssetsUseCase>()
    factory<GetProjectFileUseCase>()
    factory<GetRealSpanInfoLinkTypeUseCase>()
    factory<SaveNoteTextUseCase>()
    factory<SendNoteUseCase>()
    factory<ApplyEditorEventUseCase>()
    factory<ApplySearchEventUseCase>()
}
