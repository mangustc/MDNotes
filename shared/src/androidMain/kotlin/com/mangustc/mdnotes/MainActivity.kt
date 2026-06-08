package com.mangustc.mdnotes

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.documentfile.provider.DocumentFile
import com.mangustc.mdnotes.domain.models.Attachment
import com.mangustc.mdnotes.domain.models.DomainFile
import com.mangustc.mdnotes.ui.viewmodel.AppViewModel
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.init
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    private val appViewModel: AppViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FileKit.init(this)

        enableEdgeToEdge()
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)

        handleShareIntent(intent, appViewModel)

        setContent {
            App(viewModel = appViewModel)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShareIntent(intent, appViewModel)
    }

    private fun handleShareIntent(intent: Intent, viewModel: AppViewModel) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_SEND && action != Intent.ACTION_SEND_MULTIPLE) return

        val text = intent.getStringExtra(Intent.EXTRA_TEXT)
        val mimeType = intent.type ?: ""

        val uris: List<Uri> = when (action) {
            Intent.ACTION_SEND -> {
                val uri =
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                listOfNotNull(uri)
            }

            Intent.ACTION_SEND_MULTIPLE -> {
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                    ?: emptyList()
            }

            else -> emptyList()
        }

        val attachments = uris.map { uri ->
            val resolvedMime = contentResolver.getType(uri) ?: mimeType
            val displayName = DocumentFile.fromSingleUri(this, uri)?.name ?: "File"
            Attachment.PendingAttachment(
                domainFile = DomainFile(PlatformFile(uri)),
                displayName = displayName,
                type = if (resolvedMime.startsWith("image/")) Attachment.AttachmentType.IMAGE else Attachment.AttachmentType.FILE,
            )
        }

        if (text != null || attachments.isNotEmpty()) {
            viewModel.onShareIntent(text, attachments)
        }
    }
}
