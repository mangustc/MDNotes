package com.mangustc.mdnotes.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data class EditorDestination(val noteRelativePath: String)

@Serializable
object MessengerDestination