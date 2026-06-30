package com.example

import android.annotation.SuppressLint
import android.graphics.Bitmap
import kotlinx.coroutines.delay
import android.graphics.Matrix
import android.util.Log
import android.util.Size
import android.view.ViewGroup
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.ImageAnalysis
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import java.util.concurrent.TimeUnit
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.BentoPrimary
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

@SuppressLint("UnsafeOptInUsageError")
@Composable
fun CameraStreamView(
    viewModel: WebcamViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val isFrontCamera by viewModel.isFrontCamera.collectAsStateWithLifecycle()
    val selectedCameraId by viewModel.selectedCameraId.collectAsStateWithLifecycle()
    val isTorchOn by viewModel.isTorchOn.collectAsStateWithLifecycle()
    val isMirrorEnabled by viewModel.isMirrorEnabled.collectAsStateWithLifecycle()
    val isGridEnabled by viewModel.isGridEnabled.collectAsStateWithLifecycle()
    val jpegQuality by viewModel.jpegQuality.collectAsStateWithLifecycle()
    val resWidth by viewModel.resolutionWidth.collectAsStateWithLifecycle()
    val resHeight by viewModel.resolutionHeight.collectAsStateWithLifecycle()
    val isAutofocusEnabled by viewModel.isAutofocusEnabled.collectAsStateWithLifecycle()

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    var cameraInstance by remember { mutableStateOf<Camera?>(null) }
    var tapOffset by remember { mutableStateOf<Offset?>(null) }

    var isLifecycleResumed by remember {
        mutableStateOf(
            lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        )
    }

    var zoomValue by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isAutofocusEnabled, cameraInstance) {
        val control = cameraInstance?.cameraControl ?: return@LaunchedEffect
        if (isAutofocusEnabled) {
            try {
                control.cancelFocusAndMetering()
            } catch (e: Exception) {
                Log.e("CameraStreamView", "Failed to cancel focus and metering: ${e.message}")
            }
        }
    }

    LaunchedEffect(cameraInstance, zoomValue) {
        val control = cameraInstance?.cameraControl
        if (control != null) {
            try {
                control.setLinearZoom(zoomValue)
            } catch (e: Exception) {
                Log.e("CameraStreamView", "Failed to set zoom: ${e.message}")
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            isLifecycleResumed = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        isLifecycleResumed = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    // Handle Torch changes dynamically
    LaunchedEffect(isTorchOn, cameraInstance) {
        val currentCamera = cameraInstance ?: return@LaunchedEffect
        if (currentCamera.cameraInfo.hasFlashUnit()) {
            try {
                currentCamera.cameraControl.enableTorch(isTorchOn)
            } catch (e: Exception) {
                Log.e("CameraStreamView", "Failed to toggle torch: ${e.message}")
            }
        }
    }

    // Rebind CameraX whenever parameters change
    LaunchedEffect(isFrontCamera, selectedCameraId, resWidth, resHeight, jpegQuality, isMirrorEnabled, isLifecycleResumed) {
        if (!isLifecycleResumed) {
            try {
                if (cameraProviderFuture.isDone) {
                    cameraProviderFuture.get().unbindAll()
                }
            } catch (e: Exception) {
                // Ignore
            }
            cameraInstance = null
            return@LaunchedEffect
        }

        // Add a slight delay to allow any transition/animation to finish and avoid AppOps camera warnings
        delay(400)

        cameraProviderFuture.addListener({
            try {
                if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    Log.e("CameraStreamView", "Camera permission check failed at bind time.")
                    return@addListener
                }

                val cameraProvider = cameraProviderFuture.get()
                cameraProvider.unbindAll()

                // Query and set available cameras on the device dynamically
                val cameraInfos = cameraProvider.availableCameraInfos
                val cameraOptions = cameraInfos.mapNotNull { info ->
                    try {
                        val id = Camera2CameraInfo.from(info).cameraId
                        val lens = info.lensFacing
                        val lensStr = when (lens) {
                            CameraSelector.LENS_FACING_FRONT -> "Frontal"
                            CameraSelector.LENS_FACING_BACK -> "Trasera"
                            else -> "Externa"
                        }
                        CameraDeviceOption(id = id, lensFacing = lens, name = "$lensStr (ID: $id)")
                    } catch (e: Exception) {
                        null
                    }
                }
                if (cameraOptions.isNotEmpty()) {
                    viewModel.setAvailableCameras(cameraOptions)
                }

                val cameraSelector = if (selectedCameraId != null && cameraOptions.any { it.id == selectedCameraId }) {
                    CameraSelector.Builder()
                        .addCameraFilter { cameraInfosList ->
                            cameraInfosList.filter { info ->
                                try {
                                    Camera2CameraInfo.from(info).cameraId == selectedCameraId
                                } catch (e: Exception) {
                                    false
                                }
                            }
                        }
                        .build()
                } else {
                    if (isFrontCamera) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }
                }

                val preview = Preview.Builder().build().apply {
                    setSurfaceProvider(previewView.surfaceProvider)
                }

                val targetSize = Size(resWidth, resHeight)

                val imageAnalysis = ImageAnalysis.Builder()
                    .setTargetResolution(targetSize)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                    try {
                        val bitmap = imageProxy.toBitmap()
                        if (bitmap != null) {
                            val rotation = imageProxy.imageInfo.rotationDegrees
                            val matrix = Matrix()
                            if (rotation != 0) {
                                matrix.postRotate(rotation.toFloat())
                            }
                            if (isMirrorEnabled) {
                                matrix.postScale(-1f, 1f)
                            }

                            val processedBitmap = if (rotation != 0 || isMirrorEnabled) {
                                Bitmap.createBitmap(
                                    bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                                )
                            } else {
                                bitmap
                            }

                            val stream = ByteArrayOutputStream()
                            processedBitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, stream)
                            val jpegBytes = stream.toByteArray()

                            if (processedBitmap != bitmap) {
                                processedBitmap.recycle()
                            }
                            bitmap.recycle()

                            viewModel.mjpegServer.updateFrame(jpegBytes)
                            viewModel.recordFrameCaptured()
                        }
                    } catch (e: Exception) {
                        Log.e("CameraStreamView", "Analysis error: ${e.message}")
                    } finally {
                        imageProxy.close()
                    }
                }

                cameraInstance = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                Log.e("CameraStreamView", "Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .pointerInput(isAutofocusEnabled, cameraInstance) {
                detectTapGestures { offset ->
                    val currentCamera = cameraInstance ?: return@detectTapGestures
                    val factory = previewView.meteringPointFactory
                    val point = factory.createPoint(offset.x, offset.y)

                    val actionBuilder = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
                    if (!isAutofocusEnabled) {
                        actionBuilder.disableAutoCancel()
                    } else {
                        actionBuilder.setAutoCancelDuration(4, TimeUnit.SECONDS)
                    }

                    val action = actionBuilder.build()
                    try {
                        currentCamera.cameraControl.startFocusAndMetering(action)
                        tapOffset = offset
                    } catch (e: Exception) {
                        Log.e("CameraStreamView", "Failed to trigger focus and metering: ${e.message}")
                    }
                }
            }
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        if (isGridEnabled) {
            CameraGridOverlay(modifier = Modifier.fillMaxSize())
        }

        tapOffset?.let { offset ->
            FocusRing(
                offset = offset,
                onAnimationEnd = { tapOffset = null }
            )
        }

        // Thin vertical zoom slider overlay
        VerticalZoomBar(
            zoomValue = zoomValue,
            onZoomChange = { zoomValue = it },
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp)
        )
    }
}

@Composable
fun VerticalZoomBar(
    zoomValue: Float,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeColor = BentoPrimary
    val inactiveColor = Color.White.copy(alpha = 0.35f)

    Box(
        modifier = modifier
            .width(44.dp)
            .fillMaxHeight(0.65f)
            .background(Color.Black.copy(alpha = 0.45f), shape = RoundedCornerShape(22.dp))
            .padding(vertical = 12.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            // Zoom In icon at the top
            Icon(
                imageVector = Icons.Default.ZoomIn,
                contentDescription = "Aumentar Zoom",
                tint = Color.White,
                modifier = Modifier
                    .size(20.dp)
                    .clickable { onZoomChange((zoomValue + 0.1f).coerceIn(0f, 1f)) }
            )

            // The main interactive slider area
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .width(32.dp)
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                val heightPx = constraints.maxHeight.toFloat()
                val density = LocalDensity.current
                val heightDp = with(density) { heightPx.toDp() }

                // Track
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(inactiveColor, shape = RoundedCornerShape(2.dp))
                )

                // Active Track (Bottom up)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .width(4.dp)
                        .fillMaxHeight(zoomValue.coerceIn(0f, 1f))
                        .background(activeColor, shape = RoundedCornerShape(2.dp))
                )

                // Thumb
                val thumbSize = 20.dp
                // Calculate vertical offset relative to BottomCenter.
                // Zoom 0f is at the bottom, offset = 0.dp
                // Zoom 1f is at the top, offset = heightDp
                val yOffset = - (heightDp * zoomValue.coerceIn(0f, 1f))

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = yOffset)
                        .size(thumbSize)
                        .background(Color.White, shape = CircleShape)
                        .border(2.dp, activeColor, CircleShape)
                )

                // Transparent gesture overlay matching the BoxWithConstraints size
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(heightPx) {
                            detectTapGestures { offset ->
                                if (heightPx > 0) {
                                    val fraction = 1f - (offset.y / heightPx).coerceIn(0f, 1f)
                                    onZoomChange(fraction)
                                }
                            }
                        }
                        .pointerInput(heightPx) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                if (heightPx > 0) {
                                    val fraction = 1f - (change.position.y / heightPx).coerceIn(0f, 1f)
                                    onZoomChange(fraction)
                                }
                            }
                        }
                )
            }

            // Zoom Out icon at the bottom
            Icon(
                imageVector = Icons.Default.ZoomOut,
                contentDescription = "Disminuir Zoom",
                tint = Color.White,
                modifier = Modifier
                    .size(20.dp)
                    .clickable { onZoomChange((zoomValue - 0.1f).coerceIn(0f, 1f)) }
            )
        }
    }
}

@Composable
fun CameraGridOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val gridColor = Color.White.copy(alpha = 0.3f)
        val strokeWidthPx = 1.dp.toPx()

        // Horizontal gridlines
        drawLine(
            color = gridColor,
            start = Offset(0f, height / 3),
            end = Offset(width, height / 3),
            strokeWidth = strokeWidthPx
        )
        drawLine(
            color = gridColor,
            start = Offset(0f, height * 2 / 3),
            end = Offset(width, height * 2 / 3),
            strokeWidth = strokeWidthPx
        )

        // Vertical gridlines
        drawLine(
            color = gridColor,
            start = Offset(width / 3, 0f),
            end = Offset(width / 3, height),
            strokeWidth = strokeWidthPx
        )
        drawLine(
            color = gridColor,
            start = Offset(width * 2 / 3, 0f),
            end = Offset(width * 2 / 3, height),
            strokeWidth = strokeWidthPx
        )
    }
}

@Composable
fun FocusRing(offset: Offset, onAnimationEnd: () -> Unit) {
    val scale = remember { Animatable(1.8f) }
    val alpha = remember { Animatable(1f) }

    LaunchedEffect(offset) {
        scale.animateTo(1.0f, animationSpec = tween(300))
        delay(200)
        alpha.animateTo(0.0f, animationSpec = tween(300))
        onAnimationEnd()
    }

    Box(
        modifier = Modifier
            .offset(
                x = with(LocalDensity.current) { offset.x.toDp() } - 32.dp,
                y = with(LocalDensity.current) { offset.y.toDp() } - 32.dp
            )
            .size(64.dp)
            .graphicsLayer(
                scaleX = scale.value,
                scaleY = scale.value,
                alpha = alpha.value
            )
            .border(
                width = 2.dp,
                color = BentoPrimary,
                shape = CircleShape
            )
    )
}
