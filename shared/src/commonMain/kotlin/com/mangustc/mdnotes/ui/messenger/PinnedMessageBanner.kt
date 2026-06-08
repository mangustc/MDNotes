package com.mangustc.mdnotes.ui.messenger


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mangustc.mdnotes.domain.models.MessageBody
import mdnotes.shared.generated.resources.Res
import mdnotes.shared.generated.resources.attachment
import mdnotes.shared.generated.resources.pinned_message
import org.jetbrains.compose.resources.stringResource

@Composable
fun PinnedMessageBanner(
    notes: List<MessageBody>,
    currentIndex: Int,
    actions: MessengerUiActions,
    modifier: Modifier = Modifier,
) {
    val currentMessage = notes.getOrNull(currentIndex) ?: return
    val displayPreview = currentMessage.text.ifBlank { stringResource(Res.string.attachment) }

    Surface(
        onClick = { actions.onGoToPinned(currentMessage) },
        color = MaterialTheme.colorScheme.background,
        shadowElevation = 4.dp,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .padding(8.dp),
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.PushPin,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.pinned_message, currentIndex + 1),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = displayPreview,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
