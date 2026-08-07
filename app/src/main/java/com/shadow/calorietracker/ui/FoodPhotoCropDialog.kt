package com.shadow.calorietracker.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.shadow.calorietracker.R
import java.io.File
import java.util.UUID
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PhotoCropRect(val left: Int, val top: Int, val size: Int)

fun calculatePhotoCropRect(
    bitmapWidth: Int,
    bitmapHeight: Int,
    viewportSize: Int,
    zoom: Float,
    offset: Offset,
): PhotoCropRect {
    require(bitmapWidth > 0 && bitmapHeight > 0 && viewportSize > 0 && zoom > 0f)
    val fitScale = min(viewportSize / bitmapWidth.toFloat(), viewportSize / bitmapHeight.toFloat())
    val totalScale = fitScale * zoom
    val cropSize = (viewportSize / totalScale).roundToInt().coerceIn(1, min(bitmapWidth, bitmapHeight))
    val left = ((bitmapWidth - cropSize) / 2f - offset.x / totalScale)
        .roundToInt().coerceIn(0, bitmapWidth - cropSize)
    val top = ((bitmapHeight - cropSize) / 2f - offset.y / totalScale)
        .roundToInt().coerceIn(0, bitmapHeight - cropSize)
    return PhotoCropRect(left, top, cropSize)
}

@Composable
fun FoodPhotoCropDialog(
    source: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    var loadFinished by remember(source) { mutableStateOf(false) }
    val bitmap by produceState<Bitmap?>(initialValue = null, source) {
        value = withContext(Dispatchers.IO) {
            if (source.startsWith("https://")) PreviewBitmapCache.loadRemote(context, source)
            else PreviewBitmapCache.loadSource(context, source)
        }
        loadFinished = true
    }
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    var zoom by remember(source) { mutableFloatStateOf(1f) }
    var offset by remember(source) { mutableStateOf(Offset.Zero) }
    var saving by remember { mutableStateOf(false) }
    var saveFailed by remember { mutableStateOf(false) }

    fun minimumZoom(image: Bitmap, size: IntSize): Float {
        if (size.width <= 0 || image.width <= 0 || image.height <= 0) return 1f
        val fitScale = min(size.width / image.width.toFloat(), size.height / image.height.toFloat())
        return max(size.width / (image.width * fitScale), size.height / (image.height * fitScale))
    }

    fun clampedOffset(image: Bitmap, size: IntSize, requested: Offset, requestedZoom: Float): Offset {
        if (size.width <= 0) return Offset.Zero
        val fitScale = min(size.width / image.width.toFloat(), size.height / image.height.toFloat())
        val maximumX = ((image.width * fitScale * requestedZoom - size.width) / 2f).coerceAtLeast(0f)
        val maximumY = ((image.height * fitScale * requestedZoom - size.height) / 2f).coerceAtLeast(0f)
        return Offset(
            requested.x.coerceIn(-maximumX, maximumX),
            requested.y.coerceIn(-maximumY, maximumY),
        )
    }

    LaunchedEffect(bitmap, viewport) {
        bitmap?.let { image ->
            zoom = minimumZoom(image, viewport)
            offset = Offset.Zero
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Column(
                Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss, enabled = !saving) { Text(stringResource(R.string.cancel)) }
                    Text(
                        stringResource(R.string.crop_photo_title),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Button(
                        enabled = bitmap != null && viewport.width > 0 && !saving,
                        onClick = {
                            val image = bitmap ?: return@Button
                            saving = true
                            scope.launch {
                                val uri = withContext(Dispatchers.IO) {
                                    runCatching {
                                        val rect = calculatePhotoCropRect(
                                            image.width,
                                            image.height,
                                            viewport.width,
                                            zoom,
                                            offset,
                                        )
                                        val cropped = Bitmap.createBitmap(image, rect.left, rect.top, rect.size, rect.size)
                                        try {
                                            val directory = File(context.cacheDir, "food-photo-edits").apply { mkdirs() }
                                            val file = File(directory, "crop-${UUID.randomUUID()}.jpg")
                                            file.outputStream().buffered().use { output ->
                                                check(cropped.compress(Bitmap.CompressFormat.JPEG, 92, output))
                                            }
                                            directory.listFiles().orEmpty()
                                                .sortedByDescending(File::lastModified)
                                                .drop(24)
                                                .forEach(File::delete)
                                            FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider",
                                                file,
                                            ).toString()
                                        } finally {
                                            if (cropped !== image) cropped.recycle()
                                        }
                                    }.getOrNull()
                                }
                                saving = false
                                if (uri != null) onSave(uri) else saveFailed = true
                            }
                        },
                    ) {
                        if (saving) CircularProgressIndicator(Modifier.width(18.dp).height(18.dp), strokeWidth = 2.dp)
                        else Text(stringResource(R.string.save_crop))
                    }
                }

                Text(
                    stringResource(R.string.crop_photo_hint),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                val image = bitmap
                if (image == null) {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        if (loadFinished) {
                            Text(
                                stringResource(R.string.crop_photo_load_failed),
                                color = MaterialTheme.colorScheme.error,
                            )
                        } else {
                            CircularProgressIndicator()
                        }
                    }
                } else {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.Black)
                            .clipToBounds()
                            .onSizeChanged {
                                val edge = min(it.width, it.height)
                                viewport = IntSize(edge, edge)
                            }
                            .pointerInput(image, viewport) {
                                detectTransformGestures { centroid, pan, zoomChange, _ ->
                                    if (viewport.width <= 0) return@detectTransformGestures
                                    val minimum = minimumZoom(image, viewport)
                                    val nextZoom = (zoom * zoomChange).coerceIn(minimum, minimum * 10f)
                                    val ratio = nextZoom / zoom
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    val focalPoint = centroid - center
                                    val requestedOffset = offset * ratio + focalPoint * (1f - ratio) + pan
                                    zoom = nextZoom
                                    offset = clampedOffset(image, viewport, requestedOffset, nextZoom)
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(Modifier.width(with(androidx.compose.ui.platform.LocalDensity.current) { viewport.width.toDp() })
                            .height(with(androidx.compose.ui.platform.LocalDensity.current) { viewport.height.toDp() })) {
                            Image(
                                bitmap = image.asImageBitmap(),
                                contentDescription = stringResource(R.string.food_photo),
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = zoom
                                        scaleY = zoom
                                        translationX = offset.x
                                        translationY = offset.y
                                    },
                            )
                            Canvas(Modifier.fillMaxSize()) {
                                val lineColor = Color.White.copy(alpha = 0.65f)
                                drawRect(Color.White, style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
                                listOf(size.width / 3f, size.width * 2f / 3f).forEach { x ->
                                    drawLine(lineColor, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx(), StrokeCap.Round)
                                }
                                listOf(size.height / 3f, size.height * 2f / 3f).forEach { y ->
                                    drawLine(lineColor, Offset(0f, y), Offset(size.width, y), 1.dp.toPx(), StrokeCap.Round)
                                }
                            }
                        }
                    }

                    val minimum = minimumZoom(image, viewport)
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TextButton(
                            onClick = {
                                val nextZoom = (zoom / 1.2f).coerceAtLeast(minimum)
                                zoom = nextZoom
                                offset = clampedOffset(image, viewport, offset, nextZoom)
                            },
                        ) { Text("−") }
                        Slider(
                            value = zoom.coerceIn(minimum, minimum * 10f),
                            onValueChange = { requested ->
                                zoom = requested
                                offset = clampedOffset(image, viewport, offset, requested)
                            },
                            valueRange = minimum..(minimum * 10f),
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            onClick = {
                                val nextZoom = (zoom * 1.2f).coerceAtMost(minimum * 10f)
                                zoom = nextZoom
                                offset = clampedOffset(image, viewport, offset, nextZoom)
                            },
                        ) { Text("+") }
                    }
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.crop_photo_zoom), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(
                        enabled = bitmap != null && !saving,
                        onClick = {
                            bitmap?.let { image -> zoom = minimumZoom(image, viewport) }
                            offset = Offset.Zero
                        },
                    ) { Text(stringResource(R.string.reset_crop)) }
                }
                if (saveFailed) {
                    Text(stringResource(R.string.crop_photo_save_failed), color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(2.dp))
            }
        }
    }
}
