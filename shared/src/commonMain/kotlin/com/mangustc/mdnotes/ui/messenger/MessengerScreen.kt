package com.mangustc.mdnotes.ui.messenger

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.carousel.CarouselDefaults
import androidx.compose.material3.carousel.HorizontalUncontainedCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImage
import com.mangustc.mdnotes.domain.models.Attachment
import com.mangustc.mdnotes.domain.models.DomainFile
import com.mangustc.mdnotes.domain.models.FrontMatter
import com.mangustc.mdnotes.domain.models.LinkPreview
import com.mangustc.mdnotes.domain.models.MessageBody
import com.mangustc.mdnotes.ui.components.MenuPopup
import com.mangustc.mdnotes.ui.components.MenuPopupGroup
import com.mangustc.mdnotes.ui.components.MenuPopupItem
import com.mangustc.mdnotes.ui.components.TooltipIconButton
import com.mangustc.mdnotes.ui.util.DateFormatter
import com.mangustc.mdnotes.ui.util.FullscreenDialogProperties
import com.mangustc.mdnotes.ui.util.RememberCameraLauncher
import com.mangustc.mdnotes.ui.util.clipEntryOf
import com.mangustc.mdnotes.ui.util.scrollbar
import com.mangustc.mdnotes.ui.viewmodel.AppViewModel
import com.mangustc.mdnotes.ui.viewmodel.events.NotificationEvent
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.dialogs.openFileWithDefaultApplication
import io.github.vinceglb.filekit.name
import io.ktor.http.Url
import kotlinx.coroutines.launch
import mdnotes.shared.generated.resources.Res
import mdnotes.shared.generated.resources.actions
import mdnotes.shared.generated.resources.attach_content
import mdnotes.shared.generated.resources.attach_files
import mdnotes.shared.generated.resources.attach_images
import mdnotes.shared.generated.resources.attachment
import mdnotes.shared.generated.resources.cancel_editing
import mdnotes.shared.generated.resources.cannot_be_undone
import mdnotes.shared.generated.resources.copy
import mdnotes.shared.generated.resources.create_note
import mdnotes.shared.generated.resources.delete
import mdnotes.shared.generated.resources.edit
import mdnotes.shared.generated.resources.edit_note
import mdnotes.shared.generated.resources.edited_date
import mdnotes.shared.generated.resources.editing_note
import mdnotes.shared.generated.resources.go_back
import mdnotes.shared.generated.resources.new_quick_note
import mdnotes.shared.generated.resources.no_quick_notes_yet_type_something_below_to_get_started
import mdnotes.shared.generated.resources.open
import mdnotes.shared.generated.resources.open_a_project_folder_to_see_notes
import mdnotes.shared.generated.resources.pin
import mdnotes.shared.generated.resources.pinned_message
import mdnotes.shared.generated.resources.remove_attachment
import mdnotes.shared.generated.resources.save_changes
import mdnotes.shared.generated.resources.take_photo
import mdnotes.shared.generated.resources.unpin
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MessengerScreen(viewModel: AppViewModel) {
    val dateFormatter = koinInject<DateFormatter>()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val attachments = remember { mutableStateListOf<Attachment>() }
    var imagePagerState by remember { mutableStateOf<Pair<Int, List<DomainFile>>?>(null) }
    var carouselExpanded by rememberSaveable { mutableStateOf(false) }

    val imagePickerLauncher = rememberFilePickerLauncher(
        mode = FileKitMode.Multiple(),
        type = FileKitType.Image,
    ) { files ->
        files?.forEach { file ->
            attachments.add(
                Attachment.PendingAttachment(
                    domainFile = DomainFile(file),
                    displayName = file.name,
                    type = Attachment.AttachmentType.IMAGE,
                ),
            )
        }
    }
    val filePickerLauncher = rememberFilePickerLauncher(
        mode = FileKitMode.Multiple(),
    ) { files ->
        files?.forEach { file ->
            attachments.add(
                Attachment.PendingAttachment(
                    domainFile = DomainFile(file),
                    displayName = file.name,
                    type = Attachment.AttachmentType.FILE,
                ),
            )
        }
    }

    val cameraLauncher = koinInject<RememberCameraLauncher>().rememberCameraLauncher { file ->
        if (file == null) return@rememberCameraLauncher
        attachments.add(
            Attachment.PendingAttachment(
                domainFile = file,
                displayName = file.name,
                type = Attachment.AttachmentType.IMAGE,
            ),
        )
    }

    LaunchedEffect(Unit) {
        val pending = viewModel.consumePendingIntentAttachments()
        if (!pending.isEmpty()) carouselExpanded = true
        attachments.addAll(pending)
    }
    val pagedNotes = viewModel.messenger.notesPaged.collectAsLazyPagingItems()

    val listState = rememberLazyListState()
    val currentPinnedInfo by remember {
        derivedStateOf {
            val pinned = uiState.messengerPinnedMessages
            if (pinned.isEmpty()) return@derivedStateOf null

            val pinnedIndicesInList = pinned.map { pinnedNote ->
                pagedNotes.itemSnapshotList.items.indexOfFirst { it.note.projectFile.relativePath == pinnedNote.note.projectFile.relativePath }
            }

            val firstVisible = listState.firstVisibleItemIndex
            val targetIdx = pinnedIndicesInList.indexOfFirst { it >= firstVisible }

            if (targetIdx == -1) {
                pinned.size - 1
            } else {
                targetIdx
            }
        }
    }

    val scope = rememberCoroutineScope()

    val density = LocalDensity.current
    var inputBarHeightDp by remember { mutableStateOf(0.dp) }

    val clipboard = LocalClipboard.current
    val focusManager = LocalFocusManager.current

    val actions = remember(viewModel) {
        object : MessengerUiActions {
            override fun onNoteSelected(message: MessageBody) {
                viewModel.drawer.onNoteSelected(message.note)
            }

            override fun onDeleteNote(message: MessageBody) {
                viewModel.drawer.onDeleteNote(message.note)
            }

            override fun onEditNote(message: MessageBody) {
                viewModel.messenger.startEditNote(message)
                attachments.clear()
                attachments.addAll(message.attachments)
                if (attachments.isNotEmpty()) carouselExpanded = true
            }

            override fun onCancelEdit() {
                viewModel.messenger.cancelEditNote()
                attachments.clear()
                carouselExpanded = false
            }

            override fun onImageClick(images: List<DomainFile>, file: DomainFile) {
                val index = images.indexOf(file)
                imagePagerState = index to images
            }

            override fun onFileClick(file: DomainFile) {
                try {
                    FileKit.openFileWithDefaultApplication(file.file)
                } catch (_: Exception) {
                    viewModel.onEvent(NotificationEvent.NoAppFoundToOpenThisFile)
                }
            }

            override fun onPinNote(message: MessageBody) {
                viewModel.drawer.onPinNote(message.note)
            }

            override fun onToggleSelect(message: MessageBody) {
                viewModel.messenger.toggleNoteSelection(message)
            }

            override fun onEnsurePreview(url: String) {
                viewModel.messenger.ensureLinkPreview(url)
            }

            override val onTakePhoto =
                if (cameraLauncher == null) null else { -> cameraLauncher.launch() }

            override fun onAddImage() {
                imagePickerLauncher.launch()
            }

            override fun onAddFile() {
                filePickerLauncher.launch()
            }

            override fun onRemoveAttachment(index: Int) {
                attachments.removeAt(index)
            }

            override fun onSend() {
                val snapshot = attachments.toList()
                attachments.clear()
                if (uiState.messengerEditingNote != null) {
                    viewModel.messenger.onSendNote(
                        isEditedNote = true,
                        attachments = snapshot,
                    )
                } else {
                    viewModel.messenger.onSendNote(
                        isEditedNote = false,
                        attachments = snapshot,
                        afterUpdate = {
                            scope.launch {
                                if (pagedNotes.itemCount != 0) listState.animateScrollToItem(
                                    0,
                                )
                            }
                        },
                    )
                }
            }

            override fun onCarouselExpandedClick() {
                carouselExpanded = !carouselExpanded
            }

            override fun onDismissFullscreenCarousel() {
                imagePagerState = null
            }

            override fun onGoToPinned(message: MessageBody) {
                scope.launch {
                    val indexInList =
                        pagedNotes.itemSnapshotList.items.indexOfFirst {
                            it.note.projectFile.relativePath == message.note.projectFile.relativePath
                        }
                    if (indexInList != -1) {
                        listState.animateScrollToItem(indexInList)
                    }
                }
            }

            override fun copyText(text: String) {
                scope.launch {
                    clipboard.setClipEntry(clipEntryOf(text))
                    focusManager.clearFocus()
                }
            }

            override fun copyLink(text: String) {
                scope.launch {
                    clipboard.setClipEntry(clipEntryOf(text))
                    viewModel.onEvent(NotificationEvent.LinkCopied)
                    focusManager.clearFocus()
                }
            }
        }
    }

    if (!uiState.messengerIsLoading && uiState.project == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(Res.string.open_a_project_folder_to_see_notes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
        ) {
            MessengerInputBar(
                value = uiState.messengerNewNoteText,
                onValueChange = { viewModel.messenger.onNewNoteTextChanged(it) },
                attachments = attachments,
                isEditing = uiState.messengerEditingNote != null,
                carouselExpanded = carouselExpanded,
                actions = actions,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(1f)
                    .onSizeChanged { inputBarHeightDp = with(density) { it.height.toDp() } }
                    .fillMaxWidth(),
            )
            when {
                uiState.messengerIsLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        LoadingIndicator()
                    }
                }

                pagedNotes.itemCount == 0 -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(Res.string.no_quick_notes_yet_type_something_below_to_get_started),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                }

                else -> {
                    var pinnedBannerHeightDp by remember { mutableStateOf(0.dp) }
                    if (uiState.messengerPinnedMessages.isNotEmpty()) {
                        PinnedMessageBanner(
                            notes = uiState.messengerPinnedMessages,
                            currentIndex = currentPinnedInfo ?: 0,
                            actions = actions,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .zIndex(2f)
                                .fillMaxWidth()
                                .onSizeChanged {
                                    pinnedBannerHeightDp = with(density) { it.height.toDp() }
                                },
                        )
                    }
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(
                            top = pinnedBannerHeightDp + 8.dp,
                            bottom = 8.dp,
                        ),
                        reverseLayout = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .scrollbar(listState),
                    ) {
                        item(key = "input_spacer") {
                            Spacer(modifier = Modifier.height(inputBarHeightDp))
                        }
                        items(
                            count = pagedNotes.itemCount,
                            key = pagedNotes.itemKey { it.note.projectFile.relativePath.toString() },
                        ) { index ->
                            val note = pagedNotes[index]
                            if (note != null) {
                                val currentTimestamp = note.note.createdAt ?: note.note.lastModified
                                val prevNote =
                                    if (index + 1 < pagedNotes.itemCount) pagedNotes[index + 1] else null
                                val prevTimestamp =
                                    prevNote?.let { it.note.createdAt ?: it.note.lastModified }

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(top = 2.dp),
                                ) {
                                    if (prevTimestamp == null || !dateFormatter.isSameDay(
                                            currentTimestamp,
                                            prevTimestamp,
                                        )
                                    ) {
                                        val dateStr = remember(currentTimestamp) {
                                            dateFormatter.formatDateOnly(currentTimestamp, "MMMMd")
                                        }
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .fillMaxWidth(),
                                        ) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.secondaryContainer,
                                                shape = CircleShape,
                                            ) {
                                                Text(
                                                    text = dateStr,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    modifier = Modifier.padding(
                                                        horizontal = 12.dp,
                                                        vertical = 4.dp,
                                                    ),
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                )
                                            }
                                        }
                                    }
                                    MessageBubble(
                                        message = note,
                                        linkPreviews = uiState.messengerLinkPreviews,
                                        isPinned = note.note.tags?.contains(FrontMatter.PINNED_TAG)
                                            ?: false,
                                        isSelected = uiState.messengerSelectedNotes.contains(note),
                                        isSelectionMode = uiState.messengerSelectedNotes.isNotEmpty(),
                                        actions = actions,
                                        dateFormatter = dateFormatter,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    imagePagerState?.let { (index, uris) ->
        FullScreenImageCarouselDialog(
            initialIndex = index,
            uris = uris,
            actions = actions,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MessengerInputBar(
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

@Composable
private fun AttachmentCarouselStripViewing(
    modifier: Modifier = Modifier,
    attachments: List<Attachment>,
    actions: MessengerUiActions,
) {
    val state = rememberCarouselState { attachments.size }
    HorizontalUncontainedCarousel(
        state = state,
        itemWidth = 80.dp,
        itemSpacing = 4.dp,
        modifier = modifier
            .wrapContentHeight(),
    ) { page ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
        ) {
            val attachment = attachments[page]
            AttachmentIconButton(
                attachment = attachment,
                onClick = when (attachment.type) {
                    Attachment.AttachmentType.IMAGE -> {
                        {
                            val images = attachments.mapNotNull {
                                if (attachment.type == Attachment.AttachmentType.FILE) return@mapNotNull null
                                it.domainFile
                            }
                            actions.onImageClick(images, attachment.domainFile)
                        }
                    }

                    Attachment.AttachmentType.FILE -> {
                        { actions.onFileClick(attachment.domainFile) }
                    }
                },
            )
        }
    }
}

@Composable
private fun AttachmentCarouselStrip(
    modifier: Modifier = Modifier,
    attachments: List<Attachment>,
    actions: MessengerUiActions,
) {
    val photoDiff = if (actions.onTakePhoto == null) 1 else 0
    val extraCount = 2 + if (actions.onTakePhoto != null) 1 else 0
    val state = rememberCarouselState { attachments.size + extraCount }
    HorizontalUncontainedCarousel(
        state = state,
        itemWidth = 80.dp,
        itemSpacing = 4.dp,
        modifier = modifier
            .wrapContentHeight(),
    ) { page ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
        ) {
            when (page) {
                0 - photoDiff -> {
                    AttachmentIconButton(
                        attachment = Attachment.PendingAttachment(
                            domainFile = DomainFile(PlatformFile("")),
                            type = Attachment.AttachmentType.FILE,
                            displayName = stringResource(Res.string.take_photo),
                        ),
                        icon = Icons.Default.PhotoCamera,
                        onClick = actions.onTakePhoto ?: {},
                    )
                }

                1 - photoDiff -> {
                    AttachmentIconButton(
                        attachment = Attachment.PendingAttachment(
                            domainFile = DomainFile(PlatformFile("")),
                            type = Attachment.AttachmentType.FILE,
                            displayName = stringResource(Res.string.attach_images),
                        ),
                        icon = Icons.Default.Image,
                        onClick = actions::onAddImage,
                    )
                }

                2 - photoDiff -> {
                    AttachmentIconButton(
                        attachment = Attachment.PendingAttachment(
                            domainFile = DomainFile(PlatformFile("")),
                            type = Attachment.AttachmentType.FILE,
                            displayName = stringResource(Res.string.attach_files),
                        ),
                        icon = Icons.Default.AttachFile,
                        onClick = actions::onAddFile,
                    )
                }

                else -> {
                    val attachment = attachments[page - extraCount]
                    AttachmentIconButton(
                        attachment = attachment,
                        onClick = when (attachment.type) {
                            Attachment.AttachmentType.IMAGE -> {
                                {
                                    val images = attachments.mapNotNull {
                                        if (attachment.type == Attachment.AttachmentType.FILE) return@mapNotNull null
                                        it.domainFile
                                    }
                                    actions.onImageClick(images, attachment.domainFile)
                                }
                            }

                            Attachment.AttachmentType.FILE -> {
                                { actions.onFileClick(attachment.domainFile) }
                            }
                        },
                    )

                    Box(
                        contentAlignment = Alignment.TopEnd,
                        modifier = Modifier
                            .padding(4.dp)
                            .fillMaxSize(),
                    ) {
                        TooltipIconButton(
                            onClick = { actions.onRemoveAttachment(page - 3) },
                            icon = Icons.Default.Close,
                            tooltip = stringResource(Res.string.remove_attachment),
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            ),
                            shapes = IconButtonDefaults.shapes(
                                shape = IconButtonDefaults.extraSmallRoundShape,
                            ),
                            modifier = Modifier
                                .size(IconButtonDefaults.extraSmallIconSize),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AttachmentIconButton(
    attachment: Attachment,
    icon: ImageVector = Icons.Default.AttachFile,
    onClick: () -> Unit,
) {
    val isInvalidAttachment = attachment is Attachment.InvalidProjectAttachment
    TooltipBox(
        positionProvider =
            TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(attachment.displayName) } },
        state = rememberTooltipState(),
    ) {
        IconButton(
            onClick = onClick,
            enabled = !isInvalidAttachment,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                disabledContentColor = MaterialTheme.colorScheme.onErrorContainer,
                disabledContainerColor = MaterialTheme.colorScheme.errorContainer,
            ),
            shapes = IconButtonDefaults.shapes(
                shape = IconButtonDefaults.smallSquareShape,
            ),
            modifier = Modifier
                .fillMaxSize(),
        ) {
            when (attachment.type) {
                Attachment.AttachmentType.IMAGE ->
                    AsyncImage(
                        model = attachment.domainFile.file,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )

                Attachment.AttachmentType.FILE -> Column(
                    modifier = Modifier.padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    if (isInvalidAttachment) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    } else {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Text(
                        text = attachment.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isInvalidAttachment) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                }
            }

        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
private fun MessageBubble(
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
    BoxWithConstraints(
        contentAlignment = Alignment.TopEnd,
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
        val bubbleMaxWidth = maxWidth * 0.9f
        Row(horizontalArrangement = Arrangement.End) {
            Surface(
                modifier = Modifier
                    .widthIn(max = bubbleMaxWidth)
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

@Composable
private fun PinnedMessageBanner(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FullScreenImageCarouselDialog(
    initialIndex: Int,
    uris: List<DomainFile>,
    actions: MessengerUiActions,
) {
    val state = rememberCarouselState(initialItem = initialIndex) { uris.size }
    var showTopPanel by remember { mutableStateOf(true) }
    val fullscreenDialogProperties = koinInject<FullscreenDialogProperties>()

    Dialog(
        onDismissRequest = actions::onDismissFullscreenCarousel,
        properties = fullscreenDialogProperties.dialogProperties,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            HorizontalUncontainedCarousel(
                state = state,
                itemWidth = Dp.Infinity,
                itemSpacing = 0.dp,
                flingBehavior = CarouselDefaults.singleAdvanceFlingBehavior(state),
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                ZoomableImage(
                    file = uris[page],
                    onTap = { showTopPanel = !showTopPanel },
                )
            }

            AnimatedVisibility(
                visible = showTopPanel,
                enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
                exit = fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec()),
                modifier = Modifier.align(Alignment.TopCenter),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .statusBarsPadding()
                        .padding(8.dp),
                ) {
                    TooltipIconButton(
                        onClick = actions::onDismissFullscreenCarousel,
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        tooltip = stringResource(Res.string.go_back),
                        tooltipAnchorPosition = TooltipAnchorPosition.Below,
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = Color.White,
                        ),
                        modifier = Modifier
                            .align(Alignment.CenterStart),
                    )

                    Text(
                        text = "${state.currentItem + 1} of ${uris.size}",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.Center),
                    )
                }
            }
        }
    }
}

@Composable
private fun ZoomableImage(file: DomainFile, onTap: () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .onSizeChanged { containerSize = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = {
                        scale = 1f
                        offset = Offset.Zero
                    },
                )
            }
            .pointerInput(Unit) {
                val slop = viewConfiguration.touchSlop
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var moved = false
                    var totalPan = Offset.Zero

                    do {
                        val event = awaitPointerEvent()
                        val zoom = event.calculateZoom()
                        val pan = event.calculatePan()
                        val pointers = event.changes.size

                        totalPan += pan

                        if (!moved && (zoom != 1f || totalPan.getDistance() > slop || pointers > 1)) {
                            moved = true
                        }

                        if (moved) {
                            scale = (scale * zoom).coerceIn(1f, 4f)

                            if (scale > 1f) {
                                val maxX = (containerSize.width * (scale - 1)) / 2f
                                val maxY = (containerSize.height * (scale - 1)) / 2f

                                offset = Offset(
                                    x = (offset.x + pan.x).coerceIn(-maxX, maxX),
                                    y = (offset.y + pan.y).coerceIn(-maxY, maxY),
                                )
                                event.changes.forEach { it.consume() }
                            } else {
                                offset = Offset.Zero
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            },
    ) {
        AsyncImage(
            model = file.file,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y,
                ),
        )
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
