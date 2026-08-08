package com.shadow.calorietracker.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.shadow.calorietracker.R
import java.io.File
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PhotoCropBounds(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f,
)

data class PhotoCropRect(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int,
)

enum class PhotoCropHandle {
    NONE,
    LEFT,
    TOP,
    RIGHT,
    BOTTOM,
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    MOVE,
}

fun calculatePhotoCropRect(
    bitmapWidth: Int,
    bitmapHeight: Int,
    bounds: PhotoCropBounds,
): PhotoCropRect {
    require(bitmapWidth > 0 && bitmapHeight > 0)
    val left = (bounds.left.coerceIn(0f, 1f) * bitmapWidth).roundToInt()
        .coerceIn(0, bitmapWidth - 1)
    val top = (bounds.top.coerceIn(0f, 1f) * bitmapHeight).roundToInt()
        .coerceIn(0, bitmapHeight - 1)
    val right = (bounds.right.coerceIn(0f, 1f) * bitmapWidth).roundToInt()
        .coerceIn(left + 1, bitmapWidth)
    val bottom = (bounds.bottom.coerceIn(0f, 1f) * bitmapHeight).roundToInt()
        .coerceIn(top + 1, bitmapHeight)
    return PhotoCropRect(left, top, right - left, bottom - top)
}

fun dragPhotoCropBounds(
    bounds: PhotoCropBounds,
    handle: PhotoCropHandle,
    deltaX: Float,
    deltaY: Float,
    minimumWidth: Float,
    minimumHeight: Float,
): PhotoCropBounds {
    val minWidth = minimumWidth.coerceIn(0.001f, 1f)
    val minHeight = minimumHeight.coerceIn(0.001f, 1f)
    if (handle == PhotoCropHandle.MOVE) {
        val width = bounds.right - bounds.left
        val height = bounds.bottom - bounds.top
        val left = (bounds.left + deltaX).coerceIn(0f, 1f - width)
        val top = (bounds.top + deltaY).coerceIn(0f, 1f - height)
        return PhotoCropBounds(left, top, left + width, top + height)
    }

    var left = bounds.left
    var top = bounds.top
    var right = bounds.right
    var bottom = bounds.bottom
    if (handle in setOf(PhotoCropHandle.LEFT, PhotoCropHandle.TOP_LEFT, PhotoCropHandle.BOTTOM_LEFT)) {
        left = (left + deltaX).coerceIn(0f, right - minWidth)
    }
    if (handle in setOf(PhotoCropHandle.RIGHT, PhotoCropHandle.TOP_RIGHT, PhotoCropHandle.BOTTOM_RIGHT)) {
        right = (right + deltaX).coerceIn(left + minWidth, 1f)
    }
    if (handle in setOf(PhotoCropHandle.TOP, PhotoCropHandle.TOP_LEFT, PhotoCropHandle.TOP_RIGHT)) {
        top = (top + deltaY).coerceIn(0f, bottom - minHeight)
    }
    if (handle in setOf(PhotoCropHandle.BOTTOM, PhotoCropHandle.BOTTOM_LEFT, PhotoCropHandle.BOTTOM_RIGHT)) {
        bottom = (bottom + deltaY).coerceIn(top + minHeight, 1f)
    }
    return PhotoCropBounds(left, top, right, bottom)
}

private fun calculateFitImageRect(
    bitmapWidth: Int,
    bitmapHeight: Int,
    viewport: IntSize,
    contentInset: Float,
): Rect {
    if (bitmapWidth <= 0 || bitmapHeight <= 0 || viewport.width <= 0 || viewport.height <= 0) return Rect.Zero
    val inset = contentInset.coerceIn(0f, min(viewport.width, viewport.height) / 3f)
    val availableWidth = (viewport.width - inset * 2f).coerceAtLeast(1f)
    val availableHeight = (viewport.height - inset * 2f).coerceAtLeast(1f)
    val scale = min(availableWidth / bitmapWidth.toFloat(), availableHeight / bitmapHeight.toFloat())
    val width = bitmapWidth * scale
    val height = bitmapHeight * scale
    val left = (viewport.width - width) / 2f
    val top = (viewport.height - height) / 2f
    return Rect(left, top, left + width, top + height)
}

private fun cropDisplayRect(imageRect: Rect, bounds: PhotoCropBounds): Rect = Rect(
    imageRect.left + bounds.left * imageRect.width,
    imageRect.top + bounds.top * imageRect.height,
    imageRect.left + bounds.right * imageRect.width,
    imageRect.top + bounds.bottom * imageRect.height,
)

private fun findCropHandle(point: Offset, crop: Rect, hitTarget: Float): PhotoCropHandle {
    val nearLeft = abs(point.x - crop.left) <= hitTarget
    val nearRight = abs(point.x - crop.right) <= hitTarget
    val nearTop = abs(point.y - crop.top) <= hitTarget
    val nearBottom = abs(point.y - crop.bottom) <= hitTarget
    val withinHorizontalReach = point.x in (crop.left - hitTarget)..(crop.right + hitTarget)
    val withinVerticalReach = point.y in (crop.top - hitTarget)..(crop.bottom + hitTarget)
    return when {
        nearLeft && nearTop -> PhotoCropHandle.TOP_LEFT
        nearRight && nearTop -> PhotoCropHandle.TOP_RIGHT
        nearLeft && nearBottom -> PhotoCropHandle.BOTTOM_LEFT
        nearRight && nearBottom -> PhotoCropHandle.BOTTOM_RIGHT
        nearLeft && withinVerticalReach -> PhotoCropHandle.LEFT
        nearRight && withinVerticalReach -> PhotoCropHandle.RIGHT
        nearTop && withinHorizontalReach -> PhotoCropHandle.TOP
        nearBottom && withinHorizontalReach -> PhotoCropHandle.BOTTOM
        crop.contains(point) -> PhotoCropHandle.MOVE
        else -> PhotoCropHandle.NONE
    }
}

@Composable
fun FoodPhotoCropDialog(
    source: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    val context = LocalContext.current.applicationContext
    val density = LocalDensity.current
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
    var cropBounds by remember(source) { mutableStateOf(PhotoCropBounds()) }
    var saving by remember { mutableStateOf(false) }
    var saveFailed by remember { mutableStateOf(false) }
    val handleHitTarget = with(density) { 32.dp.toPx() }
    val cropWorkspaceInset = with(density) { 32.dp.toPx() }

    LaunchedEffect(bitmap) { cropBounds = PhotoCropBounds() }

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
                                        val rect = calculatePhotoCropRect(image.width, image.height, cropBounds)
                                        val cropped = Bitmap.createBitmap(
                                            image,
                                            rect.left,
                                            rect.top,
                                            rect.width,
                                            rect.height,
                                        )
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
                            .onSizeChanged { viewport = it }
                            .pointerInput(image, viewport) {
                                var activeHandle = PhotoCropHandle.NONE
                                detectDragGestures(
                                    onDragStart = { start ->
                                        val imageRect = calculateFitImageRect(
                                            image.width,
                                            image.height,
                                            viewport,
                                            cropWorkspaceInset,
                                        )
                                        activeHandle = findCropHandle(
                                            start,
                                            cropDisplayRect(imageRect, cropBounds),
                                            handleHitTarget,
                                        )
                                    },
                                    onDragEnd = { activeHandle = PhotoCropHandle.NONE },
                                    onDragCancel = { activeHandle = PhotoCropHandle.NONE },
                                ) { change, dragAmount ->
                                    if (activeHandle == PhotoCropHandle.NONE) return@detectDragGestures
                                    change.consume()
                                    val imageRect = calculateFitImageRect(
                                        image.width,
                                        image.height,
                                        viewport,
                                        cropWorkspaceInset,
                                    )
                                    if (imageRect.width <= 0f || imageRect.height <= 0f) return@detectDragGestures
                                    cropBounds = dragPhotoCropBounds(
                                        bounds = cropBounds,
                                        handle = activeHandle,
                                        deltaX = dragAmount.x / imageRect.width,
                                        deltaY = dragAmount.y / imageRect.height,
                                        minimumWidth = max(32f / image.width, 0.04f),
                                        minimumHeight = max(32f / image.height, 0.04f),
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            bitmap = image.asImageBitmap(),
                            contentDescription = stringResource(R.string.food_photo),
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().padding(32.dp),
                        )
                        Canvas(Modifier.fillMaxSize()) {
                            val imageRect = calculateFitImageRect(
                                image.width,
                                image.height,
                                viewport,
                                cropWorkspaceInset,
                            )
                            val crop = cropDisplayRect(imageRect, cropBounds)
                            val shade = Color.Black.copy(alpha = 0.58f)
                            drawRect(shade, Offset(imageRect.left, imageRect.top), androidx.compose.ui.geometry.Size(imageRect.width, crop.top - imageRect.top))
                            drawRect(shade, Offset(imageRect.left, crop.bottom), androidx.compose.ui.geometry.Size(imageRect.width, imageRect.bottom - crop.bottom))
                            drawRect(shade, Offset(imageRect.left, crop.top), androidx.compose.ui.geometry.Size(crop.left - imageRect.left, crop.height))
                            drawRect(shade, Offset(crop.right, crop.top), androidx.compose.ui.geometry.Size(imageRect.right - crop.right, crop.height))

                            val borderWidth = 2.dp.toPx()
                            drawRect(Color.White, crop.topLeft, crop.size, style = Stroke(borderWidth))
                            val gridColor = Color.White.copy(alpha = 0.55f)
                            listOf(crop.left + crop.width / 3f, crop.left + crop.width * 2f / 3f).forEach { x ->
                                drawLine(gridColor, Offset(x, crop.top), Offset(x, crop.bottom), 1.dp.toPx(), StrokeCap.Round)
                            }
                            listOf(crop.top + crop.height / 3f, crop.top + crop.height * 2f / 3f).forEach { y ->
                                drawLine(gridColor, Offset(crop.left, y), Offset(crop.right, y), 1.dp.toPx(), StrokeCap.Round)
                            }

                            val handleRadius = 9.dp.toPx()
                            val handlePoints = listOf(
                                crop.topLeft,
                                Offset(crop.center.x, crop.top),
                                crop.topRight,
                                Offset(crop.left, crop.center.y),
                                Offset(crop.right, crop.center.y),
                                crop.bottomLeft,
                                Offset(crop.center.x, crop.bottom),
                                crop.bottomRight,
                            )
                            handlePoints.forEach { point ->
                                drawCircle(Color.White, handleRadius, point)
                                drawCircle(Color(0xFF16713A), handleRadius * 0.52f, point)
                            }
                        }
                    }
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        enabled = bitmap != null && !saving,
                        onClick = { cropBounds = PhotoCropBounds() },
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
