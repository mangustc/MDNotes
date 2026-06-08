package com.mangustc.mdnotes.ui.messenger


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.carousel.CarouselDefaults
import androidx.compose.material3.carousel.HorizontalUncontainedCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import com.mangustc.mdnotes.domain.models.DomainFile
import com.mangustc.mdnotes.ui.components.TooltipIconButton
import com.mangustc.mdnotes.ui.util.FullscreenDialogProperties
import mdnotes.shared.generated.resources.Res
import mdnotes.shared.generated.resources.go_back
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenImageCarouselDialog(
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
