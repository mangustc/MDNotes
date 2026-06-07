package com.mangustc.mdnotes.domain.usecases.settings

import com.mangustc.mdnotes.domain.models.Project
import com.mangustc.mdnotes.domain.models.Settings
import com.mangustc.mdnotes.domain.repositories.SettingsRepository
import com.mangustc.mdnotes.domain.usecases.UseCase

data class SetSettingsInput(
    val project: Project,
    val newSettings: Settings,
)

class SetSettingsUseCase(
    private val settingsRepository: SettingsRepository,
) : UseCase<SetSettingsInput, Settings> {
    override suspend fun invoke(input: SetSettingsInput): Settings {
        return settingsRepository.setSettings(input.project, input.newSettings)
    }
}
