package com.mangustc.mdnotes.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mangustc.mdnotes.domain.models.Settings
import com.mangustc.mdnotes.domain.usecases.sync.ValidSyncProvider
import mdnotes.shared.generated.resources.Res
import mdnotes.shared.generated.resources.close
import mdnotes.shared.generated.resources.general
import mdnotes.shared.generated.resources.get_oauth_token
import mdnotes.shared.generated.resources.none_sync
import mdnotes.shared.generated.resources.oauth_token
import mdnotes.shared.generated.resources.open_link
import mdnotes.shared.generated.resources.provider
import mdnotes.shared.generated.resources.reverse_drawer_layout
import mdnotes.shared.generated.resources.settings
import mdnotes.shared.generated.resources.synchronization
import mdnotes.shared.generated.resources.yandex_disk
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsDialog(
    onDismissRequest: () -> Unit,
    settings: Settings,
    openYandexLink: () -> Unit,
    onReverseLayoutChange: (Boolean) -> Unit,
    onSyncProviderChange: (ValidSyncProvider) -> Unit,
    onOauthTokenChange: (String) -> Unit,
) {
    var dropdownExpanded by remember { mutableStateOf(false) }
    val currentProviderString = getStringFromValidSyncProvider(settings.syncProvider)

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
            ) {
                TopAppBar(
                    title = { Text(stringResource(Res.string.settings)) },
                    navigationIcon = {
                        IconButton(onClick = onDismissRequest) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(Res.string.close),
                            )
                        }
                    },
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.general),
                        style = MaterialTheme.typography.titleMedium,
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = stringResource(Res.string.reverse_drawer_layout),
                            modifier = Modifier.fillMaxWidth(0.8f),
                        )
                        Switch(
                            checked = settings.reverseLayout,
                            onCheckedChange = onReverseLayoutChange,
                        )
                    }

                    HorizontalDivider()

                    Text(
                        text = stringResource(Res.string.synchronization),
                        style = MaterialTheme.typography.titleMedium,
                    )

                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded,
                        onExpandedChange = { dropdownExpanded = it },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        OutlinedTextField(
                            value = currentProviderString,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(Res.string.provider)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth(),
                        )

                        DropdownMenuPopup(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier.exposedDropdownSize(matchAnchorWidth = true),
                        ) {
                            DropdownMenuGroup(
                                shapes = MenuDefaults.groupShape(index = 0, count = 1),
                            ) {
                                DropdownMenuItem(
                                    selected = settings.syncProvider == ValidSyncProvider.NONE,
                                    text = {
                                        Text(
                                            getStringFromValidSyncProvider(
                                                ValidSyncProvider.NONE,
                                            ),
                                        )
                                    },
                                    shapes = MenuDefaults.itemShape(index = 0, count = 2),
                                    onClick = {
                                        onSyncProviderChange(ValidSyncProvider.NONE)
                                        dropdownExpanded = false
                                    },
                                )
                                DropdownMenuItem(
                                    selected = settings.syncProvider == ValidSyncProvider.YANDEX,
                                    text = {
                                        Text(
                                            getStringFromValidSyncProvider(
                                                ValidSyncProvider.YANDEX,
                                            ),
                                        )
                                    },
                                    shapes = MenuDefaults.itemShape(index = 1, count = 2),
                                    onClick = {
                                        onSyncProviderChange(ValidSyncProvider.YANDEX)
                                        dropdownExpanded = false
                                    },
                                )
                            }
                        }
                    }

                    when (settings.syncProvider) {
                        ValidSyncProvider.YANDEX -> {
                            OutlinedButton(
                                onClick = openYandexLink,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(stringResource(Res.string.get_oauth_token))
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = stringResource(Res.string.open_link),
                                )
                            }
                            OutlinedTextField(
                                value = settings.yandexOauthToken,
                                onValueChange = onOauthTokenChange,
                                label = { Text(stringResource(Res.string.oauth_token)) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        ValidSyncProvider.NONE -> {}
                    }
                }
            }
        }
    }
}

@Composable
fun getStringFromValidSyncProvider(provider: ValidSyncProvider): String {
    val result = when (provider) {
        ValidSyncProvider.NONE -> stringResource(Res.string.none_sync)
        ValidSyncProvider.YANDEX -> stringResource(Res.string.yandex_disk)
    }
    return result
}
