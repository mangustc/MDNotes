package com.mangustc.mdnotes.ui.messenger

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.mangustc.mdnotes.domain.models.Attachment
import com.mangustc.mdnotes.domain.models.DomainFile
import com.mangustc.mdnotes.domain.models.FrontMatter
import com.mangustc.mdnotes.domain.models.MessageBody
import com.mangustc.mdnotes.ui.util.DateFormatter
import com.mangustc.mdnotes.ui.util.RememberCameraLauncher
import com.mangustc.mdnotes.ui.util.clipEntryOf
import com.mangustc.mdnotes.ui.util.scrollbar
import com.mangustc.mdnotes.ui.viewmodel.MessengerViewModel
import com.mangustc.mdnotes.ui.viewmodel.events.NotificationEvent
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.dialogs.openFileWithDefaultApplication
import io.github.vinceglb.filekit.name
import kotlinx.coroutines.launch
import mdnotes.shared.generated.resources.Res
import mdnotes.shared.generated.resources.no_quick_notes_yet_type_something_below_to_get_started
import mdnotes.shared.generated.resources.open_a_project_folder_to_see_notes
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MessengerScreen(viewModel: MessengerViewModel) {
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

    val pendingShareIntent by viewModel.pendingShareIntent.collectAsStateWithLifecycle()
    LaunchedEffect(pendingShareIntent) {
        val payload = viewModel.consumeShareIntent() ?: return@LaunchedEffect
        if (!payload.text.isNullOrEmpty()) {
            viewModel.onNewNoteTextChanged(payload.text)
        }
        if (payload.attachments.isNotEmpty()) {
            carouselExpanded = true
            attachments.addAll(payload.attachments)
        }
    }
    val pagedNotes = viewModel.notesPaged.collectAsLazyPagingItems()

    val listState = rememberLazyListState()
    val currentPinnedInfo by remember {
        derivedStateOf {
            val pinned = uiState.pinnedMessages
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
                viewModel.openNote(message.note)
            }

            override fun onDeleteNote(message: MessageBody) {
                viewModel.deleteNote(message.note)
            }

            override fun onEditNote(message: MessageBody) {
                viewModel.startEditNote(message)
                attachments.clear()
                attachments.addAll(message.attachments)
                if (attachments.isNotEmpty()) carouselExpanded = true
            }

            override fun onCancelEdit() {
                viewModel.cancelEditNote()
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
                    viewModel.emitEvent(NotificationEvent.NoAppFoundToOpenThisFile)
                }
            }

            override fun onPinNote(message: MessageBody) {
                viewModel.pinNote(message.note)
            }

            override fun onToggleSelect(message: MessageBody) {
                viewModel.toggleNoteSelection(message)
            }

            override fun onEnsurePreview(url: String) {
                viewModel.ensureLinkPreview(url)
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
                if (uiState.editingNote != null) {
                    viewModel.onSendNote(
                        isEditedNote = true,
                        attachments = snapshot,
                    )
                } else {
                    viewModel.onSendNote(
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
                    viewModel.emitEvent(NotificationEvent.LinkCopied)
                    focusManager.clearFocus()
                }
            }
        }
    }

    if (!uiState.isLoading && uiState.project == null) {
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
                value = uiState.newNoteText,
                onValueChange = { viewModel.onNewNoteTextChanged(it) },
                attachments = attachments,
                isEditing = uiState.editingNote != null,
                carouselExpanded = carouselExpanded,
                actions = actions,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(1f)
                    .onSizeChanged { inputBarHeightDp = with(density) { it.height.toDp() } },
            )
            when {
                uiState.isLoading -> {
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
                    if (uiState.pinnedMessages.isNotEmpty()) {
                        PinnedMessageBanner(
                            notes = uiState.pinnedMessages,
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
                                        linkPreviews = uiState.linkPreviews,
                                        isPinned = note.note.tags?.contains(FrontMatter.PINNED_TAG)
                                            ?: false,
                                        isSelected = uiState.selectedNotes.contains(note),
                                        isSelectionMode = uiState.selectedNotes.isNotEmpty(),
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

val MESSENGER_SCREEN_MAX_SIZE = 600.dp
