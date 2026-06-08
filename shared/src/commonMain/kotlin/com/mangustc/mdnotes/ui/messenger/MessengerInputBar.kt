package com.mangustc.mdnotes.ui.messenger


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.mangustc.mdnotes.domain.models.Attachment
import com.mangustc.mdnotes.ui.components.TooltipIconButton
import mdnotes.shared.generated.resources.Res
import mdnotes.shared.generated.resources.attach_content
import mdnotes.shared.generated.resources.cancel_editing
import mdnotes.shared.generated.resources.create_note
import mdnotes.shared.generated.resources.edit_note
import mdnotes.shared.generated.resources.editing_note
import mdnotes.shared.generated.resources.new_quick_note
import mdnotes.shared.generated.resources.save_changes
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MessengerInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    attachments: List<Attachment>,
    isEditing: Boolean,
    carouselExpanded: Boolean,
    actions: MessengerUiActions,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .background(
                color = if (carouselExpanded || isEditing) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent,
            )
            .padding(8.dp),
    ) {
        AnimatedVisibility(
            visible = isEditing,
            enter = expandVertically(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                    fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
            exit = shrinkVertically(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                    fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec()),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = stringResource(Res.string.editing_note),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                IconButton(onClick = actions::onCancelEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(Res.string.cancel_editing),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        AnimatedVisibility(
            visible = carouselExpanded,
            enter = expandVertically(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                    fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
            exit = shrinkVertically(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                    fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec()),
        ) {
            AttachmentCarouselStrip(
                attachments = attachments,
                actions = actions,
            )
        }
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    if (isEditing) stringResource(Res.string.edit_note) else stringResource(
                        Res.string.new_quick_note,
                    ),
                )
            },
            shape = MaterialTheme.shapes.extraLargeIncreased,
            maxLines = 6,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
            ),
            colors = TextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            leadingIcon = {
                TooltipIconButton(
                    onClick = actions::onCarouselExpandedClick,
                    icon = Icons.Default.AttachFile,
                    tooltip = stringResource(Res.string.attach_content),
                    shapes = IconButtonDefaults.shapes(
                        shape = IconButtonDefaults.standardShape,
                        pressedShape = IconButtonDefaults.standardShape,
                    ),
                )
            },
            trailingIcon = {
                TooltipIconButton(
                    onClick = actions::onSend,
                    icon = Icons.AutoMirrored.Filled.Send,
                    tooltip = if (isEditing) stringResource(Res.string.save_changes) else stringResource(
                        Res.string.create_note,
                    ),
                    colors = IconButtonDefaults.iconButtonColors(
                        disabledContentColor = Color.Unspecified,
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
                    shapes = IconButtonDefaults.shapes(
                        shape = IconButtonDefaults.standardShape,
                        pressedShape = IconButtonDefaults.standardShape,
                    ),
                    enabled = value.isNotBlank() || attachments.isNotEmpty(),
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 8.dp,
                    shape = MaterialTheme.shapes.extraLargeIncreased,
                ),
        )
    }
}

