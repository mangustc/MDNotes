package com.mangustc.mdnotes.ui

import android.content.ClipData
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Abc
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.paging.compose.collectAsLazyPagingItems
import com.mangustc.mdnotes.domain.models.DomainFile
import com.mangustc.mdnotes.domain.models.FrontMatter
import com.mangustc.mdnotes.domain.models.Note
import com.mangustc.mdnotes.ui.components.NoteDrawerItem
import com.mangustc.mdnotes.ui.components.NoteSearchBar
import com.mangustc.mdnotes.ui.components.TooltipIconButton
import com.mangustc.mdnotes.ui.editor.EditorScreen
import com.mangustc.mdnotes.ui.messenger.MessengerScreen
import com.mangustc.mdnotes.ui.navigation.EditorDestination
import com.mangustc.mdnotes.ui.navigation.MessengerDestination
import com.mangustc.mdnotes.ui.settings.SettingsDialog
import com.mangustc.mdnotes.ui.util.DateFormatter
import com.mangustc.mdnotes.ui.util.onNotificationToast
import com.mangustc.mdnotes.ui.viewmodel.AppViewModel
import com.mangustc.mdnotes.ui.viewmodel.events.ClipboardEvent
import com.mangustc.mdnotes.ui.viewmodel.events.FocusEvent
import com.mangustc.mdnotes.ui.viewmodel.events.NavigationEvent
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.compose.rememberDirectoryPickerLauncher
import io.github.vinceglb.filekit.dialogs.openFileWithDefaultApplication
import mdnotes.shared.generated.resources.Res
import mdnotes.shared.generated.resources.app_name
import mdnotes.shared.generated.resources.are_you_sure_you_want_to_delete_this_note
import mdnotes.shared.generated.resources.cancel
import mdnotes.shared.generated.resources.clear_selection
import mdnotes.shared.generated.resources.close
import mdnotes.shared.generated.resources.copy_selected
import mdnotes.shared.generated.resources.create
import mdnotes.shared.generated.resources.create_new_note
import mdnotes.shared.generated.resources.created_at
import mdnotes.shared.generated.resources.delete
import mdnotes.shared.generated.resources.delete_note
import mdnotes.shared.generated.resources.delete_selected
import mdnotes.shared.generated.resources.enter_a_name_for_your_new_note
import mdnotes.shared.generated.resources.enter_a_new_name_for_your_note
import mdnotes.shared.generated.resources.go_back
import mdnotes.shared.generated.resources.last_modified
import mdnotes.shared.generated.resources.n_a
import mdnotes.shared.generated.resources.name
import mdnotes.shared.generated.resources.new_note
import mdnotes.shared.generated.resources.none
import mdnotes.shared.generated.resources.note_details
import mdnotes.shared.generated.resources.note_name
import mdnotes.shared.generated.resources.open_a_project_folder_to_see_notes
import mdnotes.shared.generated.resources.open_menu
import mdnotes.shared.generated.resources.quick_notes
import mdnotes.shared.generated.resources.read_editor
import mdnotes.shared.generated.resources.rename
import mdnotes.shared.generated.resources.rename_note
import mdnotes.shared.generated.resources.select_project_folder
import mdnotes.shared.generated.resources.settings
import mdnotes.shared.generated.resources.synchronization
import mdnotes.shared.generated.resources.tags
import mdnotes.shared.generated.resources.this_action_cannot_be_undone
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
actual fun AppScaffold(
    appViewModel: AppViewModel,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    val uiState by appViewModel.uiState.collectAsStateWithLifecycle()

    val folderPicker = rememberDirectoryPickerLauncher() { directory ->
        if (directory == null) return@rememberDirectoryPickerLauncher
        appViewModel.project.onProjectSelected(DomainFile(directory))
    }

    val clipboard = LocalClipboard.current
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        appViewModel.navigationEvents.collect {
            when (it) {
                is NavigationEvent.GoToEditor ->
                    navController.navigate(EditorDestination(it.note.projectFile.relativePath.value))

                is NavigationEvent.GoBack -> navController.popBackStack()
                is NavigationEvent.OpenDrawer -> drawerState.open()
                is NavigationEvent.CloseDrawer -> drawerState.close()
                is NavigationEvent.OpenUrl -> uriHandler.openUri(it.url)
                is NavigationEvent.OpenFile -> {
                    FileKit.openFileWithDefaultApplication(it.uri.file)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        appViewModel.focusEvents.collect {
            when (it) {
                is FocusEvent.ClearFocus -> focusManager.clearFocus()
            }
        }
    }

    LaunchedEffect(Unit) {
        appViewModel.clipboardEvents.collect {
            when (it) {
                is ClipboardEvent.Copy -> clipboard.setClipEntry(
                    ClipEntry(
                        ClipData.newPlainText(
                            "Markdown Editor",
                            it.text,
                        ),
                    ),
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        fun toast(message: String) {
            Toast.makeText(
                context,
                message,
                Toast.LENGTH_SHORT,
            ).show()
        }
        appViewModel.notificationEvents.collect {
            onNotificationToast(it, ::toast)
        }
    }

    val searchResults = appViewModel.drawer.searchResultsPaged.collectAsLazyPagingItems()

    val isSelectionMode = uiState.messengerSelectedNotes.isNotEmpty()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.imePadding(),
            ) {
                val reverseLayout = uiState.settings?.reverseLayout ?: false
                val projectComponent = @Composable {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    ) {
                        SplitButtonLayout(
                            leadingButton = {
                                val projectName = uiState.project?.name
                                    ?: stringResource(Res.string.select_project_folder)
                                TooltipBox(
                                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                                        TooltipAnchorPosition.Below,
                                    ),
                                    tooltip = { PlainTooltip { Text(projectName) } },
                                    state = rememberTooltipState(),
                                ) {
                                    val content = @Composable {
                                        Icon(
                                            Icons.Default.FolderOpen,
                                            modifier = Modifier.size(SplitButtonDefaults.LeadingIconSize),
                                            contentDescription = projectName,
                                        )
                                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                        Text(projectName)
                                    }
                                    if (uiState.project != null) {
                                        SplitButtonDefaults.TonalLeadingButton(
                                            onClick = { folderPicker.launch() },
                                        ) { content() }
                                    } else {
                                        Button(
                                            onClick = { folderPicker.launch() },
                                            shapes = ButtonDefaults.shapes(),
                                        ) { content() }
                                    }
                                }
                            },
                            trailingButton = {
                                if (uiState.project != null) {
                                    TooltipBox(
                                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                                            TooltipAnchorPosition.Below,
                                        ),
                                        tooltip = { PlainTooltip { Text(stringResource(Res.string.settings)) } },
                                        state = rememberTooltipState(),
                                    ) {
                                        SplitButtonDefaults.TonalTrailingButton(
                                            checked = false,
                                            onCheckedChange = {
                                                appViewModel.settings.showSettings()
                                            },
                                        ) {
                                            Icon(
                                                Icons.Default.Settings,
                                                modifier = Modifier.size(SplitButtonDefaults.TrailingIconSize),
                                                contentDescription = stringResource(Res.string.settings),
                                            )
                                        }
                                    }
                                }
                            },
                        )
                        if (uiState.project != null) {
                            TooltipBox(
                                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                                    TooltipAnchorPosition.Below,
                                ),
                                tooltip = { PlainTooltip { Text(stringResource(Res.string.synchronization)) } },
                                state = rememberTooltipState(),
                            ) {
                                OutlinedIconButton(
                                    onClick = {
                                        appViewModel.project.syncNow()
                                    },
                                    border = BorderStroke(
                                        width = IconButtonDefaults.outlinedIconButtonBorder(true).width,
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                    ),
                                    shapes = IconButtonDefaults.shapes(
                                        shape = IconButtonDefaults.mediumSquareShape,
                                    ),
                                    modifier = Modifier.size(
                                        IconButtonDefaults.mediumContainerSize(),
                                    ),
                                ) {
                                    if (uiState.isSyncInProgress) {
                                        LoadingIndicator()
                                    } else {
                                        Icon(
                                            Icons.Default.Sync,
                                            contentDescription = stringResource(Res.string.synchronization),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                val searchComponent = @Composable {
                    if (uiState.project != null) {
                        NoteSearchBar(
                            searchState = appViewModel.drawer.searchState,
                            searchResults = searchResults,
                            onSearchEvent = appViewModel.drawer::onSearchEvent,
                            reverseLayout = reverseLayout,
                            paddingValues = PaddingValues(horizontal = 16.dp),
                        ) { note ->
                            NoteDrawerItem(
                                name = note.name,
                                supportingText = note.tags?.filter { it != FrontMatter.PINNED_TAG }
                                    ?.let {
                                        if (it.isEmpty()) return@let null
                                        it.joinToString(", ")
                                    },
                                isPinned = note.tags?.contains(FrontMatter.PINNED_TAG) == true,
                                selected = note.projectFile.relativePath == uiState.activeNote?.projectFile?.relativePath,
                                onClick = { appViewModel.drawer.onNoteSelected(note); focusManager.clearFocus() },
                                onOpen = { appViewModel.drawer.onNoteSelected(note) },
                                onDelete = { appViewModel.drawer.showNoteDeleteDialog(note) },
                                onRename = { appViewModel.drawer.showNoteRenameDialog(note) },
                                onShowInfo = { appViewModel.drawer.showNoteShowInfoDialog(note) },
                                onPin = { appViewModel.drawer.onPinNote(note) },
                            )
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Text(
                                stringResource(Res.string.open_a_project_folder_to_see_notes),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                            )
                        }
                    }
                }
                val createNoteComponent = @Composable {
                    if (uiState.project != null) {
                        FilledTonalButton(
                            onClick = { appViewModel.drawer.showCreateNoteDialog() },
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .fillMaxWidth(),
                        ) {
                            Icon(
                                Icons.Default.Create,
                                contentDescription = stringResource(Res.string.create_new_note),
                            )
                            Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                            Text(text = stringResource(Res.string.create_new_note))
                        }
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(vertical = 8.dp),
                ) {
                    if (reverseLayout) {
                        projectComponent()
                        createNoteComponent()
                        HorizontalDivider()
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.BottomStart,
                        ) {
                            searchComponent()
                        }
                    } else {
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.TopStart,
                        ) {
                            searchComponent()
                        }
                        HorizontalDivider()
                        createNoteComponent()
                        projectComponent()
                    }
                }
            }
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        if (isSelectionMode) {
                            Text("${uiState.messengerSelectedNotes.size}")
                        } else {
                            Text(
                                if (navBackStackEntry?.destination?.route == MessengerDestination::class.qualifiedName) stringResource(
                                    Res.string.quick_notes,
                                ) else uiState.activeNote?.name
                                    ?: stringResource(Res.string.app_name),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                    navigationIcon = {
                        if (isSelectionMode) {
                            TooltipIconButton(
                                onClick = { appViewModel.messenger.clearSelection() },
                                icon = Icons.Default.Close,
                                tooltip = stringResource(Res.string.clear_selection),
                                tooltipAnchorPosition = TooltipAnchorPosition.Below,
                            )
                        } else if (navBackStackEntry?.destination?.route != MessengerDestination::class.qualifiedName) {
                            TooltipIconButton(
                                onClick = { appViewModel.editor.onCloseEditor() },
                                icon = Icons.AutoMirrored.Filled.ArrowBack,
                                tooltip = stringResource(Res.string.go_back),
                                tooltipAnchorPosition = TooltipAnchorPosition.Below,
                            )
                        } else {
                            TooltipIconButton(
                                onClick = {
                                    appViewModel.onEvent(NavigationEvent.OpenDrawer)
                                },
                                icon = Icons.Default.Menu,
                                tooltip = stringResource(Res.string.open_menu),
                                tooltipAnchorPosition = TooltipAnchorPosition.Below,
                            )
                        }
                    },
                    actions = {
                        if (isSelectionMode) {
                            TooltipIconButton(
                                onClick = {
                                    appViewModel.messenger.copySelectedNotesText()
                                },
                                icon = Icons.Outlined.ContentCopy,
                                tooltip = stringResource(Res.string.copy_selected),
                                tooltipAnchorPosition = TooltipAnchorPosition.Below,
                            )
                            TooltipIconButton(
                                onClick = { appViewModel.messenger.deleteSelectedNotes() },
                                icon = Icons.Outlined.Delete,
                                tooltip = stringResource(Res.string.delete_selected),
                                tooltipAnchorPosition = TooltipAnchorPosition.Below,
                            )
                        }

                        if (!isSelectionMode &&
                            navBackStackEntry?.destination?.route != MessengerDestination::class.qualifiedName
                        ) {
                            if (!uiState.isViewingMode) {
                                TooltipIconButton(
                                    onClick = { appViewModel.editor.toggleViewingMode() },
                                    icon = Icons.Default.Visibility,
                                    tooltip = stringResource(Res.string.read_editor),
                                    tooltipAnchorPosition = TooltipAnchorPosition.Below,
                                )
                            }
                        }
                    },
                )
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = MessengerDestination,
                modifier = Modifier.padding(innerPadding),
            ) {
                composable<EditorDestination> { backStackEntry ->
                    val data: EditorDestination = backStackEntry.toRoute()
                    EditorScreen(viewModel = appViewModel, noteRelativePath = data.noteRelativePath)
                }
                composable<MessengerDestination> {
                    MessengerScreen(viewModel = appViewModel)
                }
            }
        }
    }

    if (uiState.isCreateNoteDialogVisible) {
        CreateNoteDialog(
            onDismissRequest = { appViewModel.drawer.dismissCreateNoteDialog() },
            onConfirmCreate = {
                appViewModel.drawer.onCreateNote()
                appViewModel.drawer.dismissCreateNoteDialog()
            },
            initialName = uiState.newNoteNameInput,
            onNameChange = { newName -> appViewModel.drawer.updateNewNoteName(newName) },
        )
    }
    if (uiState.isNoteDeleteDialogVisible && uiState.dialogNote != null) {
        DeleteNoteDialog(
            onDismissRequest = { appViewModel.drawer.dismissNoteDeleteDialog() },
            onConfirmDelete = {
                appViewModel.drawer.onDeleteNote(uiState.dialogNote!!)
                appViewModel.drawer.dismissNoteDeleteDialog()
            },
            noteName = uiState.dialogNote!!.name,
        )
    }
    if (uiState.isNoteRenameDialogVisible && uiState.dialogNote != null) {
        RenameNoteDialog(
            onDismissRequest = { appViewModel.drawer.dismissNoteRenameDialog() },
            onConfirmRename = {
                appViewModel.drawer.onRenameNote(uiState.dialogNote!!, uiState.noteRenameInput)
                appViewModel.drawer.dismissNoteRenameDialog()
            },
            name = uiState.noteRenameInput,
            onNameChange = { newName -> appViewModel.drawer.onRenameNameInputChanged(newName) },
        )
    }
    if (uiState.isNoteShowInfoDialogVisible && uiState.dialogNote != null) {
        ShowInfoDialog(
            onDismissRequest = { appViewModel.drawer.dismissNoteShowInfoDialog() },
            note = uiState.dialogNote!!,
        )
    }
    if (uiState.isSettingsDialogVisible && uiState.settings != null) {
        SettingsDialog(
            onDismissRequest = { appViewModel.settings.dismissSettings() },
            settings = uiState.settings!!,
            openYandexLink = appViewModel.settings::openYandexLink,
            onReverseLayoutChange = appViewModel.settings::setReverseLayout,
            onSyncProviderChange = appViewModel.settings::setSyncProvider,
            onOauthTokenChange = appViewModel.settings::setYandexOauthToken,
        )
    }
}

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
                    label = { Text(stringResource(Res.string.note_name)) },
                    modifier = Modifier.fillMaxWidth(),
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
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(stringResource(Res.string.name)) },
                    supportingContent = { Text(note.name) },
                    leadingContent = { Icon(Icons.Default.Abc, contentDescription = null) },
                )
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(stringResource(Res.string.last_modified)) },
                    supportingContent = {
                        val timeString = dateFormatter.formatRelativeTime(
                            note.lastModified,
                            DateFormatter.HOUR_MILLIS,
                        )
                        Text(timeString.ifBlank { stringResource(Res.string.n_a) })
                    },
                    leadingContent = { Icon(Icons.Default.History, contentDescription = null) },
                )
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(stringResource(Res.string.created_at)) },
                    supportingContent = {
                        val timeString =
                            if (note.createdAt != null) dateFormatter.formatRelativeTime(
                                note.createdAt,
                                DateFormatter.HOUR_MILLIS,
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
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
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
