package com.mangustc.mdnotes.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Abc
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.mangustc.mdnotes.domain.models.Note
import com.mangustc.mdnotes.ui.util.DateFormatter
import mdnotes.shared.generated.resources.Res
import mdnotes.shared.generated.resources.close
import mdnotes.shared.generated.resources.created_at
import mdnotes.shared.generated.resources.last_modified
import mdnotes.shared.generated.resources.n_a
import mdnotes.shared.generated.resources.name
import mdnotes.shared.generated.resources.none
import mdnotes.shared.generated.resources.note_details
import mdnotes.shared.generated.resources.tags
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun ShowInfoDialog(
    onDismissRequest: () -> Unit,
    note: Note,
) {
    val dateFormatter = koinInject<DateFormatter>()

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(Res.string.note_details)) },
        text = {
            Column {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Companion.Transparent),
                    headlineContent = { Text(stringResource(Res.string.name)) },
                    supportingContent = { Text(note.name) },
                    leadingContent = { Icon(Icons.Default.Abc, contentDescription = null) },
                )
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Companion.Transparent),
                    headlineContent = { Text(stringResource(Res.string.last_modified)) },
                    supportingContent = {
                        val timeString = dateFormatter.formatRelativeTime(
                            note.lastModified,
                            DateFormatter.Companion.HOUR_MILLIS,
                        )
                        Text(timeString.ifBlank { stringResource(Res.string.n_a) })
                    },
                    leadingContent = { Icon(Icons.Default.History, contentDescription = null) },
                )
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Companion.Transparent),
                    headlineContent = { Text(stringResource(Res.string.created_at)) },
                    supportingContent = {
                        val timeString =
                            if (note.createdAt != null) dateFormatter.formatRelativeTime(
                                note.createdAt,
                                DateFormatter.Companion.HOUR_MILLIS,
                            ) else null
                        Text(if (!timeString.isNullOrBlank()) timeString else stringResource(Res.string.n_a))
                    },
                    leadingContent = {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                        )
                    },
                )
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Companion.Transparent),
                    headlineContent = { Text(stringResource(Res.string.tags)) },
                    supportingContent = {
                        Text(
                            if (!note.tags.isNullOrEmpty()) note.tags.joinToString(", ") else stringResource(
                                Res.string.none,
                            ),
                        )
                    },
                    leadingContent = { Icon(Icons.Default.Tag, contentDescription = null) },
                )
            }
        },
        confirmButton = {
            OutlinedButton(onClick = onDismissRequest) { Text(stringResource(Res.string.close)) }
        },
    )
}