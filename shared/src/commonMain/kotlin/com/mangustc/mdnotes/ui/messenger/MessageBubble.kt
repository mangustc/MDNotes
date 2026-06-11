package com.mangustc.mdnotes.ui.messenger


import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.CarouselDefaults
import androidx.compose.material3.carousel.HorizontalUncontainedCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImage
import com.mangustc.mdnotes.domain.models.LinkPreview
import com.mangustc.mdnotes.domain.models.MessageBody
import com.mangustc.mdnotes.ui.components.MenuPopup
import com.mangustc.mdnotes.ui.components.MenuPopupGroup
import com.mangustc.mdnotes.ui.components.MenuPopupItem
import com.mangustc.mdnotes.ui.util.DateFormatter
import io.ktor.http.Url
import mdnotes.shared.generated.resources.Res
import mdnotes.shared.generated.resources.actions
import mdnotes.shared.generated.resources.cannot_be_undone
import mdnotes.shared.generated.resources.copy
import mdnotes.shared.generated.resources.delete
import mdnotes.shared.generated.resources.edit
import mdnotes.shared.generated.resources.edited_date
import mdnotes.shared.generated.resources.open
import mdnotes.shared.generated.resources.pin
import mdnotes.shared.generated.resources.unpin
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun MessageBubble(
    message: MessageBody,
    linkPreviews: Map<String, LinkPreview?>,
    isPinned: Boolean = false,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    dateFormatter: DateFormatter,
    actions: MessengerUiActions,
) {
    val density = LocalDensity.current
    val uriHandler = LocalUriHandler.current

    val urls =
        remember(message.note.body) { message.links.map { it.value }.toList() }
    LaunchedEffect(urls) { urls.forEach(actions::onEnsurePreview) }

    val previews = remember(urls, linkPreviews) { urls.mapNotNull { linkPreviews[it] } }

    var menuExpanded by remember { mutableStateOf(false) }
    var touchX by remember { mutableStateOf(0.dp) }
    var touchY by remember { mutableStateOf(0.dp) }

    val urlColor = MaterialTheme.colorScheme.primary
    val annotatedBody = remember(message.text) {
        buildAnnotatedString {
            var lastIndex = 0
            message.links.forEach { match ->
                append(message.text.substring(lastIndex, match.range.first))
                pushStringAnnotation(tag = "URL", annotation = match.value)
                withStyle(SpanStyle(color = urlColor, textDecoration = TextDecoration.Underline)) {
                    append(match.value)
                }
                pop()
                lastIndex = match.range.last + 1
            }
            if (lastIndex < message.text.length) append(message.text.substring(lastIndex))
        }
    }

    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }

    val overlayColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .drawWithContent {
                drawContent()
                if (isSelected) drawRect(overlayColor)
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val event = awaitFirstDown(requireUnconsumed = false)
                    touchX = with(density) { event.position.x.toDp() }
                    touchY = with(density) { event.position.y.toDp() }
                }
            }
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) actions.onToggleSelect(message) else menuExpanded = true
                },
                onLongClick = { actions.onToggleSelect(message) },
            )
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.width(MESSENGER_SCREEN_MAX_SIZE),
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 400.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 4.dp,
                            bottomStart = 16.dp,
                            bottomEnd = 16.dp,
                        ),
                    ),
                color = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 2.dp,
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    val density = LocalDensity.current
                    var contentWidth by remember { mutableStateOf(0.dp) }

                    val style = MaterialTheme.typography.labelSmall
                    val iconSize = with(density) { style.fontSize.toDp() }
                    val timeColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)

                    val isEdited =
                        message.note.createdAt != null && (message.note.lastModified - message.note.createdAt) > 500
                    val editString = stringResource(
                        Res.string.edited_date,
                        dateFormatter.formatRelativeTime(
                            message.note.lastModified,
                            DateFormatter.HOUR_MILLIS,
                        ),
                    )
                    val timeString = remember(message.note.createdAt, message.note.lastModified) {
                        val time = message.note.createdAt ?: message.note.lastModified
                        dateFormatter.formatRelativeTime(time, DateFormatter.HOUR_MILLIS)
                    }

                    Column(
                        modifier = Modifier.onSizeChanged {
                            contentWidth = with(density) { it.width.toDp() }
                        },
                    ) {
                        if (message.attachments.isNotEmpty()) {
                            AttachmentCarouselStripViewing(
                                attachments = message.attachments,
                                actions = actions,
                            )
                            if (message.text.isNotBlank()) Spacer(modifier = Modifier.height(8.dp))
                        }

                        if (message.text.isNotBlank()) {
                            SelectionContainer {
                                Text(
                                    text = annotatedBody,
                                    style = TextStyle(
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight,
                                    ),
                                    onTextLayout = { layoutResult.value = it },
                                    modifier = Modifier.pointerInput(Unit) {
                                        detectTapGestures(
                                            onTap = { offset ->
                                                if (isSelectionMode) {
                                                    actions.onToggleSelect(message)
                                                    return@detectTapGestures
                                                }
                                                layoutResult.value?.let { result ->
                                                    val position =
                                                        result.getOffsetForPosition(offset)
                                                    annotatedBody.getStringAnnotations(
                                                        "URL",
                                                        position,
                                                        position,
                                                    )
                                                        .firstOrNull()
                                                        ?.let { uriHandler.openUri(it.item) }
                                                        ?: run { menuExpanded = true }
                                                }
                                            },
                                            onLongPress = { offset ->
                                                if (isSelectionMode) return@detectTapGestures
                                                layoutResult.value?.let { result ->
                                                    val position =
                                                        result.getOffsetForPosition(offset)
                                                    annotatedBody.getStringAnnotations(
                                                        "URL",
                                                        position,
                                                        position,
                                                    )
                                                        .firstOrNull()?.let { link ->
                                                            actions.copyLink(link.item)
                                                        }
                                                }
                                            },
                                        )
                                    },
                                )
                            }
                        }

                        if (previews.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LinkPreviewCarousel(previews = previews)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.widthIn(min = contentWidth),
                        horizontalArrangement = if (isEdited) Arrangement.SpaceBetween else Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = editString, style = style, color = timeColor)
                        Spacer(modifier = Modifier.width(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            if (isPinned) {
                                Icon(
                                    imageVector = Icons.Filled.PushPin,
                                    contentDescription = null,
                                    tint = timeColor,
                                    modifier = Modifier.size(iconSize),
                                )
                            }
                            Text(text = timeString, style = style, color = timeColor)
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset { IntOffset(touchX.roundToPx(), touchY.roundToPx()) },
        ) {
            MenuPopup(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) { gs ->
                MenuPopupGroup(
                    index = 0,
                    count = 1,
                    label = stringResource(Res.string.actions),
                    interactionSource = gs,
                ) {
                    MenuPopupItem(
                        text = stringResource(Res.string.open), index = 0, count = 5,
                        icon = Icons.AutoMirrored.Outlined.OpenInNew,
                        onClick = { menuExpanded = false; actions.onNoteSelected(message) },
                    )
                    MenuPopupItem(
                        text = stringResource(Res.string.copy), index = 1, count = 5,
                        icon = Icons.Outlined.ContentCopy,
                        onClick = { menuExpanded = false; actions.copyText(message.text) },
                    )
                    MenuPopupItem(
                        text = if (isPinned) stringResource(Res.string.unpin) else stringResource(
                            Res.string.pin,
                        ),
                        index = 2, count = 5,
                        icon = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        onClick = { menuExpanded = false; actions.onPinNote(message) },
                    )
                    MenuPopupItem(
                        text = stringResource(Res.string.edit), index = 3, count = 5,
                        icon = Icons.Outlined.Edit,
                        onClick = { menuExpanded = false; actions.onEditNote(message) },
                    )
                    MenuPopupItem(
                        text = stringResource(Res.string.delete), index = 4, count = 5,
                        supportingText = stringResource(Res.string.cannot_be_undone),
                        icon = Icons.Outlined.Delete,
                        tint = MaterialTheme.colorScheme.error,
                        onClick = { menuExpanded = false; actions.onDeleteNote(message) },
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalCoilApi::class)
@Composable
private fun LinkPreviewCarousel(previews: List<LinkPreview>) {
    val state = rememberCarouselState { previews.size }

    Box(modifier = Modifier.fillMaxWidth()) {
        HorizontalUncontainedCarousel(
            state = state,
            itemWidth = Dp.Infinity,
            itemSpacing = 8.dp,
            flingBehavior = CarouselDefaults.singleAdvanceFlingBehavior(state),
            userScrollEnabled = previews.size > 1,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
        ) { page ->
            val preview = previews[page]
            val uriHandler = LocalUriHandler.current

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp)),
                onClick = { uriHandler.openUri(preview.url) },
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
            ) {
                Column {
                    if (!preview.imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = preview.imageUrl,
                            contentDescription = preview.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
                            contentScale = ContentScale.Crop,
                        )
                    }

                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                        Text(
                            text = remember(preview.url) {
                                runCatching { Url(preview.url).host.removePrefix("www.") }
                                    .getOrDefault(preview.url)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (!preview.title.isNullOrBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = preview.title,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (!preview.description.isNullOrBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = preview.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
        if (previews.size > 1) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
            ) {
                Text(
                    text = "${state.currentItem + 1}/${previews.size}",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
    }
}
