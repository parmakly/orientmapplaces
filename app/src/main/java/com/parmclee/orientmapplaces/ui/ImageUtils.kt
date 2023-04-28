package com.parmclee.orientmapplaces.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import coil.compose.rememberAsyncImagePainter
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import java.io.File


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ZoomableImage(
    painter: Painter,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Transparent,
    imageAlign: Alignment = Alignment.Center,
    shape: Shape = RectangleShape,
    maxScale: Float = 1f,
    minScale: Float = 3f,
    contentScale: ContentScale = ContentScale.Fit,
    isRotation: Boolean = true,
    isZoomable: Boolean = true,
    onClick: () -> Unit = {}
) {
    val scale = remember { mutableStateOf(1f) }
    val rotationState = remember { mutableStateOf(1f) }
    val offsetX = remember { mutableStateOf(1f) }
    val offsetY = remember { mutableStateOf(1f) }
    val currentPointerCount = remember { mutableStateOf(0) }
    var size by remember { mutableStateOf(IntSize.Zero) }

    fun reset() {
        scale.value = 1f
        offsetX.value = 1f
        offsetY.value = 1f
    }

    Box(
        modifier = Modifier
            .clip(shape)
            .background(backgroundColor)
            .onSizeChanged {
                if (size != it) size = it
            }
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    if (currentPointerCount.value < 2) {
                        onClick()
                    }
                }
            )
            .pointerInput(Unit) {
                if (isZoomable) {
                    forEachGesture {
                        awaitPointerEventScope {
                            awaitFirstDown()
                            currentPointerCount.value = 0
                            do {
                                val event = awaitPointerEvent()
                                val nextScale = scale.value * event.calculateZoom()
                                if (nextScale in (maxScale..minScale)) {
                                    scale.value = nextScale
                                }
                                if (scale.value > 1) {
                                    val maxTranslationX = (scale.value - 1) * size.width / 2
                                    val maxTranslationY = (scale.value - 1) * size.height / 2
                                    val offset = event.calculatePan()
                                    if ((offsetX.value + offset.x) in (-maxTranslationX..maxTranslationX)) {
                                        offsetX.value += offset.x
                                        if (offsetY.value + offset.y in (-maxTranslationY..maxTranslationY)) {
                                            offsetY.value += offset.y
                                        }
                                        rotationState.value += event.calculateRotation()
                                    }
                                } else {
                                    reset()
                                }
                                val pointers = event.changes.count { it.pressed }
                                if (currentPointerCount.value < pointers) {
                                    currentPointerCount.value = pointers
                                }
                            } while (event.changes.any { it.pressed })
                        }
                    }
                }
            }
    ) {
        Image(
            painter = painter,
            contentDescription = null,
            contentScale = contentScale,
            modifier = modifier
                .align(imageAlign)
                .graphicsLayer {
                    if (isZoomable) {
                        scaleX = scale.value
                        scaleY = scale.value
                        if (isRotation) {
                            rotationZ = rotationState.value
                        }
                        val maxTranslationX = (scaleX - 1) * size.width / 2
                        val minTranslationX = (1 - scaleX) * size.width / 2
                        translationX = maxOf(minTranslationX, minOf(offsetX.value, maxTranslationX))
                        val maxTranslationY = (scaleX - 1) * size.height / 2
                        val minTranslationY = (1 - scaleX) * size.height / 2
                        translationY = maxOf(minTranslationY, minOf(offsetY.value, maxTranslationY))
                    }
                }
        )
    }
}


@Composable
fun BaseImage(
    url: String?,
    modifier: Modifier = Modifier,
    file: File? = null,
    @DrawableRes placeholder: Int? = null,
    isRound: Boolean = true,
    scaleType: ContentScale = ContentScale.Crop,
    needHardware: Boolean = true,
    zoomable: Boolean = false,
    onZoomableClick: () -> Unit = {}
) {
    val painter = rememberAsyncImagePainter(
        ImageRequest.Builder(LocalContext.current)
            .data(data = url ?: file).apply(block = fun ImageRequest.Builder.() {
                if (!needHardware) {
                    allowHardware(false)
                }
                if (isRound) {
                    transformations(CircleCropTransformation())
                }
                if (placeholder != null) {
                    placeholder(placeholder)
                    error(placeholder)
                    fallback(placeholder)
                }
            }).listener(object : ImageRequest.Listener{
                override fun onError(request: ImageRequest, result: ErrorResult) {
                    result.throwable.printStackTrace()
                }
            })
            .build()
    )
    if (zoomable) {
        ZoomableImage(painter = painter, modifier, onClick = onZoomableClick)
    } else {
        Image(painter, contentDescription = "", modifier = modifier, contentScale = scaleType)
    }
}