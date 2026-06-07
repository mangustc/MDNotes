package com.mangustc.mdnotes.domain.usecases.settings

import com.mangustc.mdnotes.domain.models.Project
import com.mangustc.mdnotes.domain.models.Settings
import com.mangustc.mdnotes.domain.repositories.SettingsRepository
import com.mangustc.mdnotes.domain.usecases.UseCase

data class GetSettingsInput(
    val project: Project,
)

class GetSettingsUseCase(
    private val settingsRepository: SettingsRepository,
) : UseCase<GetSettingsInput, Settings> {
    override suspend fun invoke(input: GetSettingsInput): Settings {
        return settingsRepository.getSettings(input.project)
    }
}
