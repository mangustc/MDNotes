package com.mangustc.mdnotes.ui.util

import com.mangustc.mdnotes.domain.exceptions.FileNotFoundException
import com.mangustc.mdnotes.domain.exceptions.FileNotReadableException
import com.mangustc.mdnotes.domain.exceptions.FileNotWritableException
import com.mangustc.mdnotes.domain.exceptions.LinkFetchException
import com.mangustc.mdnotes.domain.exceptions.LinkPreviewException
import com.mangustc.mdnotes.domain.exceptions.ProjectAccessException
import com.mangustc.mdnotes.domain.exceptions.ProjectException
import com.mangustc.mdnotes.domain.exceptions.SyncAuthException
import com.mangustc.mdnotes.domain.exceptions.SyncException
import com.mangustc.mdnotes.domain.exceptions.SyncNetworkException
import com.mangustc.mdnotes.domain.exceptions.SyncQuotaException
import com.mangustc.mdnotes.domain.exceptions.SyncServerException
import com.mangustc.mdnotes.domain.exceptions.SyncStateException
import com.mangustc.mdnotes.ui.viewmodel.events.NotificationEvent
import mdnotes.shared.generated.resources.Res
import mdnotes.shared.generated.resources.*
import org.jetbrains.compose.resources.getString

suspend fun onNotificationToast(event: NotificationEvent, toast: (String) -> Unit) {
    when (event) {
        is NotificationEvent.LinkCopied -> toast(getString(Res.string.copy))
        is NotificationEvent.FailedToAddPhoto -> toast(getString(Res.string.failed_to_create_photo_container))
        is NotificationEvent.FailedToStartCamera -> toast(getString(Res.string.failed_to_start_camera))
        is NotificationEvent.NoAppFoundToOpenThisFile -> toast(getString(Res.string.no_app_found_to_open_this_file))
        is NotificationEvent.SyncServiceIsNone -> toast(getString(Res.string.no_sync_service_configured_configure_one_in_settings))
        is NotificationEvent.CustomMessage -> toast(event.message)
        is NotificationEvent.FromException -> when (val e = event.exception) {
            is SyncException -> when (e) {
                is SyncAuthException -> toast(getString(Res.string.authentication_failed_please_log_in_again))
                is SyncNetworkException -> toast(getString(Res.string.network_error_check_internet_connection))
                is SyncQuotaException -> toast(getString(Res.string.cloud_storage_full_free_up_space))
                is SyncServerException -> toast(getString(Res.string.sync_server_unavailable_try_again_later))
                is SyncStateException -> toast(getString(Res.string.sync_data_corrupted_please_reset_sync))
            }

            is ProjectException -> when (e) {
                is FileNotFoundException -> toast(
                    getString(
                        Res.string.file_not_found,
                        e.path,
                    ),
                )

                is FileNotReadableException -> toast(
                    getString(
                        Res.string.file_not_readable,
                        e.path,
                    ),
                )

                is FileNotWritableException -> toast(
                    getString(
                        Res.string.file_not_writable,
                        e.path,
                    ),
                )

                is ProjectAccessException -> toast(
                    getString(
                        Res.string.failed_to_access_project_directory,
                        e.path,
                    ),
                )
            }

            is LinkPreviewException -> when (e) {
                is LinkFetchException -> toast(
                    getString(
                        Res.string.failed_to_fetch_link_information,
                        e.path,
                    ),
                )
            }

            else -> toast(
                e.message ?: getString(
                    Res.string.unknown_error,
                    e.localizedMessage ?: "Unknown",
                ),
            )
        }
    }
}
