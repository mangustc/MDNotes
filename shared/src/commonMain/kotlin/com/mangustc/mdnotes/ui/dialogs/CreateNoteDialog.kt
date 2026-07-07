package com.mangustc.mdnotes.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import mdnotes.shared.generated.resources.Res
import mdnotes.shared.generated.resources.cancel
import mdnotes.shared.generated.resources.create
import mdnotes.shared.generated.resources.enter_a_name_for_your_new_note
import mdnotes.shared.generated.resources.new_note
import mdnotes.shared.generated.resources.note_name
import org.jetbrains.compose.resources.stringResource

@Composable
fun CreateNoteDialog(
    onDismissRequest: () -> Unit,
    onConfirmCreate: () -> Unit,
    initialName: String,
    onNameChange: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(Res.string.new_note)) },
        text = {
            Column {
                Text(stringResource(Res.string.enter_a_name_for_your_new_note))
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = initialName,
                    onValueChange = { onNameChange(it) },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                    ),
                    keyboardActions = KeyboardActions(

                    ),
                    label = { Text(stringResource(Res.string.note_name)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onPreviewKeyEvent {
                            if (it.type == KeyEventType.KeyDown && it.key == Key.Enter) {
                                onConfirmCreate()
                                true
                            } else {
                                false
                            }
                        },
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (initialName.isNotBlank()) onConfirmCreate() },
            ) { Text(stringResource(Res.string.create)) }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismissRequest) { Text(stringResource(Res.string.cancel)) }
        },
    )
}