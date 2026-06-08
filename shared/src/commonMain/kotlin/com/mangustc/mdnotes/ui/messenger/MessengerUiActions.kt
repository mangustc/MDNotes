package com.mangustc.mdnotes.ui.messenger

import com.mangustc.mdnotes.domain.models.DomainFile
import com.mangustc.mdnotes.domain.models.MessageBody

interface MessengerUiActions {
    fun onNoteSelected(message: MessageBody)
    fun onDeleteNote(message: MessageBody)
    fun onEditNote(message: MessageBody)
    fun onCancelEdit()
    fun onImageClick(images: List<DomainFile>, file: DomainFile)
    fun onFileClick(file: DomainFile)
    fun onPinNote(message: MessageBody)
    fun onToggleSelect(message: MessageBody)
    fun onEnsurePreview(url: String)
    val onTakePhoto: (() -> Unit)?
    fun onAddImage()
    fun onAddFile()
    fun onRemoveAttachment(index: Int)
    fun onSend()
    fun onCarouselExpandedClick()
    fun onDismissFullscreenCarousel()
    fun onGoToPinned(message: MessageBody)
    fun copyText(text: String)
    fun copyLink(text: String)
}