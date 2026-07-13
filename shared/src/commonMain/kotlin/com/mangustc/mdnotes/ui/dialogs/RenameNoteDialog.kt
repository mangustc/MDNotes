package com.mangustc.mdnotes.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import mdnotes.shared.generated.resources.Res
import mdnotes.shared.generated.resources.cancel
import mdnotes.shared.generated.resources.enter_a_new_name_for_your_note
import mdnotes.shared.generated.resources.note_name
import mdnotes.shared.generated.resources.rename
import mdnotes.shared.generated.resources.rename_note
import org.jetbrains.compose.resources.stringResource

@Composable
fun RenameNoteDialog(
    onDismissRequest: () -> Unit,
    onConfirmRename: () -> Unit,
    name: String,
    onNameChange: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(Res.string.rename_note)) },
        text = {
            Column {
                Text(stringResource(Res.string.enter_a_new_name_for_your_note))
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { onNameChange(it) },
                    label = { Text(stringResource(Res.string.note_name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirmRename) { Text(stringResource(Res.string.rename)) }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismissRequest) { Text(stringResource(Res.string.cancel)) }
        },
    )
}