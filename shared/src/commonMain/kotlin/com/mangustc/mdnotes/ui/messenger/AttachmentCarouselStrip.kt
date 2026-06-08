package com.mangustc.mdnotes.ui.messenger


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.carousel.HorizontalUncontainedCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.mangustc.mdnotes.domain.models.Attachment
import com.mangustc.mdnotes.domain.models.DomainFile
import com.mangustc.mdnotes.ui.components.TooltipIconButton
import io.github.vinceglb.filekit.PlatformFile
import mdnotes.shared.generated.resources.Res
import mdnotes.shared.generated.resources.attach_files
import mdnotes.shared.generated.resources.attach_images
import mdnotes.shared.generated.resources.remove_attachment
import mdnotes.shared.generated.resources.take_photo
import org.jetbrains.compose.resources.stringResource

@Composable
fun AttachmentCarouselStripViewing(
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
fun AttachmentCarouselStrip(
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
