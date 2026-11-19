package com.mangustc.mdnotes.ui.viewmodel.session

import com.mangustc.mdnotes.domain.models.Attachment
import com.mangustc.mdnotes.domain.models.Project
import com.mangustc.mdnotes.domain.models.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate

data class SharedIntentPayload(
    val text: String?,
    val attachments: List<Attachment>,
)

class ProjectSession {
    private val _project = MutableStateFlow<Project?>(null)
    val project: StateFlow<Project?> = _project.asStateFlow()

    private val _settings = MutableStateFlow<Settings?>(null)
    val settings: StateFlow<Settings?> = _settings.asStateFlow()

    private val _isProjectLoading = MutableStateFlow(true)
    val isProjectLoading: StateFlow<Boolean> = _isProjectLoading.asStateFlow()

    private val _isSyncInProgress = MutableStateFlow(false)
    val isSyncInProgress: StateFlow<Boolean> = _isSyncInProgress.asStateFlow()

    private val _pendingShareIntent = MutableStateFlow<SharedIntentPayload?>(null)
    val pendingShareIntent: StateFlow<SharedIntentPayload?> = _pendingShareIntent.asStateFlow()

    fun setProject(project: Project?, settings: Settings?) {
        _project.value = project
        _settings.value = settings
    }

    fun setSettings(settings: Settings) {
        _settings.value = settings
    }

    fun setProjectLoading(isLoading: Boolean) {
        _isProjectLoading.value = isLoading
    }

    fun setSyncInProgress(inProgress: Boolean) {
        _isSyncInProgress.value = inProgress
    }

    fun submitShareIntent(text: String?, attachments: List<Attachment>) {
        if (text.isNullOrEmpty() && attachments.isEmpty()) return
        _pendingShareIntent.value = SharedIntentPayload(
            text = text,
            attachments = attachments,
        )
    }

    fun consumeShareIntent(): SharedIntentPayload? =
        _pendingShareIntent.getAndUpdate { null }
}
