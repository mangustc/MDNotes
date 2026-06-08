package com.mangustc.mdnotes.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mdnotes.shared.generated.resources.Res
import mdnotes.shared.generated.resources.are_you_sure_you_want_to_delete_this_note
import mdnotes.shared.generated.resources.cancel
import mdnotes.shared.generated.resources.delete
import mdnotes.shared.generated.resources.delete_note
import mdnotes.shared.generated.resources.this_action_cannot_be_undone
import org.jetbrains.compose.resources.stringResource

@Composable
fun DeleteNoteDialog(
    onDismissRequest: () -> Unit,
    onConfirmDelete: () -> Unit,
    noteName: String,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(Res.string.delete_note)) },
        text = {
            Column {
                Text(stringResource(Res.string.are_you_sure_you_want_to_delete_this_note))
                Text(
                    text = noteName,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(Res.string.this_action_cannot_be_undone),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmDelete,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            ) { Text(stringResource(Res.string.delete)) }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismissRequest) { Text(stringResource(Res.string.cancel)) }
        },
    )
}