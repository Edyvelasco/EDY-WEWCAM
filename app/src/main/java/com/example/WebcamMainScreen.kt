package com.example

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Grid3x3
import androidx.compose.material.icons.filled.GridOff
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.BentoBackground
import com.example.ui.theme.BentoCardBg
import com.example.ui.theme.BentoBorder
import com.example.ui.theme.BentoSelected
import com.example.ui.theme.BentoPrimary
import com.example.ui.theme.BentoDarkText
import com.example.ui.theme.BentoSecondaryText
import com.example.ui.theme.BentoLiveRed
import com.example.ui.theme.BentoAlertRed
import com.example.ui.theme.BentoWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebcamMainScreen(
    viewModel: WebcamViewModel,
    hasCameraPermission: Boolean,
    onRequestPermission: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current

    // Keep screen turned on while using the app as a webcam
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose {
            view.keepScreenOn = false
        }
    }

    val isRunning by viewModel.isServerRunning.collectAsStateWithLifecycle()
    val serverIp by viewModel.serverIp.collectAsStateWithLifecycle()
    val serverPort by viewModel.serverPort.collectAsStateWithLifecycle()
    val fps by viewModel.fps.collectAsStateWithLifecycle()

    val clientsCount by viewModel.mjpegServer.activeClientsCount.collectAsStateWithLifecycle()
    val rawDataRate by viewModel.mjpegServer.dataRateBytesPerSec.collectAsStateWithLifecycle()

    val speedFormatted = remember(rawDataRate) {
        val kb = rawDataRate / 1024.0
        if (kb >= 1024.0) {
            String.format("%.1f MB/s", kb / 1024.0)
        } else {
            String.format("%.1f KB/s", kb)
        }
    }

    var showSettings by remember { mutableStateOf(false) }
    var activeTab by remember { mutableIntStateOf(0) }

    // Start server automatically if permission is granted
    LaunchedEffect(hasCameraPermission) {
        if (hasCameraPermission && !isRunning) {
            viewModel.startServer()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. Full-screen Camera Preview
        if (hasCameraPermission) {
            CameraStreamView(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BentoBackground),
                contentAlignment = Alignment.Center
            ) {
                PermissionRequestPlaceholder(onRequestPermission = onRequestPermission)
            }
        }

        // 2. HUD Overlay (Only if permission is granted)
        if (hasCameraPermission) {
            // Elegant Translucent Status HUD on Top
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp)
                    .align(Alignment.TopCenter),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Logo and Live badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(BentoPrimary, Color(0xFF8B5CF6))
                                    ),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Iriun Pro",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (isRunning) "TRANSMITIENDO" else "INACTIVO",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isRunning) BentoLiveRed else Color.Gray,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    // Floating stats
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LiveBadgeOverlay(isRunning = isRunning, clientsCount = clientsCount)
                    }
                }

                // Sub-stats tray (Translucent Glass card)
                if (isRunning) {
                    val resWidth by viewModel.resolutionWidth.collectAsStateWithLifecycle()
                    val resHeight by viewModel.resolutionHeight.collectAsStateWithLifecycle()
                    Row(
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .align(Alignment.End),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "FPS: $fps",
                            color = if (fps >= 24) Color(0xFF22C55E) else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Box(modifier = Modifier.size(1.dp, 10.dp).background(Color.White.copy(alpha = 0.3f)))
                        Text(
                            text = "RES: ${resWidth}x${resHeight}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Box(modifier = Modifier.size(1.dp, 10.dp).background(Color.White.copy(alpha = 0.3f)))
                        Text(
                            text = speedFormatted,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Elegant Translucent Control Panel on Bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp)
                    .align(Alignment.BottomCenter)
            ) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .widthIn(max = 340.dp)
                        .fillMaxWidth()
                        .height(54.dp)
                        .background(Color(0xE01A1C1E), RoundedCornerShape(27.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(27.dp))
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left: Camera Flip
                    IconButton(
                        onClick = { viewModel.toggleCamera() },
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = Color.White.copy(alpha = 0.8f)
                        ),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlipCameraAndroid,
                            contentDescription = "Girar Cámara",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Torch Toggle
                    val isFrontCamera by viewModel.isFrontCamera.collectAsStateWithLifecycle()
                    val isTorchOn by viewModel.isTorchOn.collectAsStateWithLifecycle()
                    IconButton(
                        onClick = { viewModel.toggleTorch() },
                        enabled = !isFrontCamera,
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = if (isTorchOn) BentoPrimary else Color.White.copy(alpha = 0.8f),
                            disabledContentColor = Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Linterna",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Center: Large Stream Action Button
                    Button(
                        onClick = {
                            if (isRunning) {
                                viewModel.stopServer()
                            } else {
                                viewModel.startServer()
                            }
                        },
                        modifier = Modifier
                            .height(38.dp)
                            .weight(1f)
                            .padding(horizontal = 6.dp)
                            .testTag("server_toggle_button"),
                        shape = RoundedCornerShape(19.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRunning) BentoAlertRed else BentoPrimary,
                            contentColor = Color.White
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isRunning) "STOP" else "STREAM",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Toggles (Mirror/Grid quick button)
                    val isGridEnabled by viewModel.isGridEnabled.collectAsStateWithLifecycle()
                    IconButton(
                        onClick = { viewModel.toggleGrid() },
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = if (isGridEnabled) BentoPrimary else Color.White.copy(alpha = 0.8f)
                        ),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = if (isGridEnabled) Icons.Default.Grid3x3 else Icons.Default.GridOff,
                            contentDescription = "Rejilla",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Right: Open Settings Dialog/Overlay
                    IconButton(
                        onClick = { showSettings = true },
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = Color.White.copy(alpha = 0.8f)
                        ),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Ajustes",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 3. Sliding/Animated Settings Overlay Panel
        AnimatedVisibility(
            visible = showSettings,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
            ) {
                // Bottom Half sheet or Scrollable Panel
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.85f)
                        .align(Alignment.BottomCenter)
                        .background(BentoBackground, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                        .border(
                            width = 1.dp,
                            color = BentoBorder,
                            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                        )
                        .padding(top = 16.dp)
                ) {
                    // Header of the sheet
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Ajustes de CamLink",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = BentoDarkText
                            )
                            Text(
                                text = "Configuración de red y calidad de vídeo",
                                fontSize = 12.sp,
                                color = BentoSecondaryText
                            )
                        }
                        IconButton(
                            onClick = { showSettings = false },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = BentoCardBg,
                                contentColor = BentoDarkText
                            ),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar")
                        }
                    }

                    Divider(color = BentoBorder, modifier = Modifier.padding(top = 8.dp))

                    // Scrollable Settings Content
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Connections panel (USB / Wi-Fi)
                        ConnectionSetupPanel(
                            activeTab = activeTab,
                            onTabSelected = { activeTab = it },
                            serverIp = serverIp,
                            serverPort = serverPort,
                            clientsCount = clientsCount,
                            isRunning = isRunning,
                            viewModel = viewModel,
                            context = context
                        )

                        // Quality panel
                        VideoQualitySettingsPanel(viewModel = viewModel)

                        // Camera Selector panel
                        CameraSelectorPanel(viewModel = viewModel)

                        // Focus Mode panel
                        FocusModeSettingsPanel(viewModel = viewModel)

                        // Grid / Mirror toggles
                        GridTogglesBentoCard(viewModel = viewModel)

                        // Helpful Note
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = BentoCardBg),
                            shape = RoundedCornerShape(24.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BentoBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = BentoPrimary,
                                    modifier = Modifier.size(24.dp)
                               )
                                Column {
                                    Text(
                                        text = "Pantalla Activa",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = BentoDarkText
                                    )
                                    Text(
                                        text = "La aplicación evita que la pantalla del móvil se apague automáticamente mientras estás transmitiendo.",
                                        fontSize = 12.sp,
                                        color = BentoSecondaryText,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun LiveBadgeOverlay(isRunning: Boolean, clientsCount: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Row(
        modifier = Modifier
            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    color = if (isRunning) {
                        if (clientsCount > 0) BentoLiveRed.copy(alpha = alpha) else Color.Yellow.copy(alpha = alpha)
                    } else {
                        Color.Gray
                    },
                    shape = CircleShape
                )
        )
        Text(
            text = if (isRunning) {
                if (clientsCount > 0) "LIVE ($clientsCount)" else "LISTO"
            } else {
                "STANDBY"
            },
            color = BentoWhite,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun GridTogglesBentoCard(viewModel: WebcamViewModel) {
    val isMirrorEnabled by viewModel.isMirrorEnabled.collectAsStateWithLifecycle()
    val isGridEnabled by viewModel.isGridEnabled.collectAsStateWithLifecycle()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BentoCardBg),
        shape = RoundedCornerShape(28.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BentoBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mirror
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { viewModel.toggleMirror() }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.NetworkCheck,
                    contentDescription = null,
                    tint = if (isMirrorEnabled) BentoPrimary else BentoSecondaryText,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Giro Espejo",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = BentoDarkText
                    )
                    Text(
                        text = if (isMirrorEnabled) "Habilitado" else "Deshabilitado",
                        fontSize = 11.sp,
                        color = BentoSecondaryText
                    )
                }
            }

            // Grid
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { viewModel.toggleGrid() }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isGridEnabled) Icons.Default.Grid3x3 else Icons.Default.GridOff,
                    contentDescription = null,
                    tint = if (isGridEnabled) BentoPrimary else BentoSecondaryText,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Rejilla de Enfoque",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = BentoDarkText
                    )
                    Text(
                        text = if (isGridEnabled) "Visible" else "Oculta",
                        fontSize = 11.sp,
                        color = BentoSecondaryText
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionRequestPlaceholder(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BentoBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = BentoPrimary,
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Permiso de Cámara Requerido",
            color = BentoDarkText,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Esta aplicación requiere acceso a la cámara de tu móvil para transmitir vídeo de alta calidad a tu ordenador.",
            color = BentoSecondaryText,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(18.dp))
        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(
                containerColor = BentoPrimary,
                contentColor = BentoWhite
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(text = "Permitir Acceso", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ConnectionSetupPanel(
    activeTab: Int,
    onTabSelected: (Int) -> Unit,
    serverIp: String?,
    serverPort: Int,
    clientsCount: Int,
    isRunning: Boolean,
    viewModel: WebcamViewModel,
    context: Context
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BentoCardBg),
        shape = RoundedCornerShape(28.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BentoBorder)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = Color.Transparent,
                contentColor = BentoPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                        color = BentoPrimary,
                        height = 3.dp
                    )
                },
                divider = {
                    Divider(color = BentoBorder)
                }
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { onTabSelected(0) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Conexión Wi-Fi", fontWeight = FontWeight.SemiBold, color = BentoDarkText)
                        }
                    }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { onTabSelected(1) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Usb, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Conexión USB", fontWeight = FontWeight.SemiBold, color = BentoDarkText)
                        }
                    }
                )
            }

            Box(modifier = Modifier.padding(16.dp)) {
                if (activeTab == 0) {
                    WifiSettingsView(
                        serverIp = serverIp,
                        serverPort = serverPort,
                        isRunning = isRunning,
                        viewModel = viewModel,
                        context = context
                    )
                } else {
                    UsbSettingsView(
                        serverPort = serverPort,
                        isRunning = isRunning,
                        viewModel = viewModel,
                        context = context
                    )
                }
            }
        }
    }
}

@Composable
fun WifiSettingsView(
    serverIp: String?,
    serverPort: Int,
    isRunning: Boolean,
    viewModel: WebcamViewModel,
    context: Context
) {
    val clipboardManager = LocalClipboardManager.current
    val streamUrl = if (!serverIp.isNullOrEmpty()) "http://$serverIp:$serverPort/stream" else "Sin conexión Wi-Fi"

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Transmisión Wi-Fi Local",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = BentoDarkText
                )
                Text(
                    text = "Asegúrate de estar en la misma red Wi-Fi",
                    fontSize = 12.sp,
                    color = BentoSecondaryText
                )
            }
            Box(
                modifier = Modifier
                    .background(
                        if (serverIp != null) BentoPrimary.copy(alpha = 0.1f) else BentoAlertRed.copy(alpha = 0.1f),
                        RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (serverIp != null) "Wi-Fi Conectado" else "Sin Wi-Fi",
                    color = if (serverIp != null) BentoPrimary else BentoAlertRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Port selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Puerto de Red:",
                color = BentoDarkText,
                fontSize = 14.sp
            )
            var portInput by remember(serverPort) { mutableStateOf(serverPort.toString()) }
            OutlinedTextField(
                value = portInput,
                onValueChange = {
                    portInput = it
                    val newPort = it.toIntOrNull()
                    if (newPort != null) {
                        viewModel.setPort(newPort)
                    }
                },
                enabled = !isRunning,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.width(100.dp),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = BentoDarkText,
                    unfocusedTextColor = BentoDarkText,
                    focusedBorderColor = BentoPrimary,
                    unfocusedBorderColor = BentoBorder
                )
            )
        }

        // Copyable URL Card
        if (serverIp != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BentoSelected.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .border(1.dp, BentoBorder, RoundedCornerShape(12.dp))
                    .clickable {
                        clipboardManager.setText(AnnotatedString(streamUrl))
                        Toast
                            .makeText(context, "URL copiada al portapapeles", Toast.LENGTH_SHORT)
                            .show()
                    }
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Dirección de Flujo (OBS):",
                        color = BentoPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copiar",
                        tint = BentoPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = streamUrl,
                    color = BentoDarkText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Action Steps
        Column(
            modifier = Modifier.padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StepItem(number = "1", text = "Abre tu programa de streaming u OBS Studio en tu PC.")
            StepItem(number = "2", text = "Añade una fuente de tipo Navegador (Browser Source).")
            StepItem(number = "3", text = "Copia la dirección de flujo de arriba en el campo URL.")
            StepItem(number = "4", text = "Establece el ancho y alto igual a la resolución seleccionada.")
        }
    }
}

@Composable
fun UsbSettingsView(
    serverPort: Int,
    isRunning: Boolean,
    viewModel: WebcamViewModel,
    context: Context
) {
    val clipboardManager = LocalClipboardManager.current
    val adbCmd = "adb forward tcp:$serverPort tcp:$serverPort"
    val usbUrl = "http://localhost:$serverPort/stream"

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column {
            Text(
                text = "Conexión USB de Latencia Cero",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = BentoDarkText
            )
            Text(
                text = "La conexión por cable evita interferencias inalámbricas",
                fontSize = 12.sp,
                color = BentoSecondaryText
            )
        }

        // Port selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Puerto USB:",
                color = BentoDarkText,
                fontSize = 14.sp
            )
            var portInput by remember(serverPort) { mutableStateOf(serverPort.toString()) }
            OutlinedTextField(
                value = portInput,
                onValueChange = {
                    portInput = it
                    val newPort = it.toIntOrNull()
                    if (newPort != null) {
                        viewModel.setPort(newPort)
                    }
                },
                enabled = !isRunning,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.width(100.dp),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = BentoDarkText,
                    unfocusedTextColor = BentoDarkText,
                    focusedBorderColor = BentoPrimary,
                    unfocusedBorderColor = BentoBorder
                )
            )
        }

        // ADB Command card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(BentoSelected.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .border(1.dp, BentoBorder, RoundedCornerShape(12.dp))
                .clickable {
                    clipboardManager.setText(AnnotatedString(adbCmd))
                    Toast
                        .makeText(context, "Comando copiado al portapapeles", Toast.LENGTH_SHORT)
                        .show()
                }
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Comando ADB para tu PC:",
                    color = BentoPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copiar",
                    tint = BentoPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = adbCmd,
                color = BentoDarkText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        // Localhost URL
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(BentoCardBg, RoundedCornerShape(12.dp))
                .border(1.dp, BentoBorder, RoundedCornerShape(12.dp))
                .clickable {
                    clipboardManager.setText(AnnotatedString(usbUrl))
                    Toast
                        .makeText(context, "URL copiada al portapapeles", Toast.LENGTH_SHORT)
                        .show()
                }
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Dirección en tu ordenador (Localhost):",
                    color = BentoSecondaryText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copiar",
                    tint = BentoSecondaryText,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = usbUrl,
                color = BentoDarkText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        // Steps
        Column(
            modifier = Modifier.padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StepItem(number = "1", text = "Conecta el móvil al ordenador con el cable USB.")
            StepItem(number = "2", text = "Activa Depuración USB en Ajustes de desarrollador en el móvil.")
            StepItem(number = "3", text = "Abre un terminal de comandos en tu PC (Cmd, Powershell o Bash).")
            StepItem(number = "4", text = "Copia y ejecuta el comando ADB de arriba para crear el túnel de puertos.")
            StepItem(number = "5", text = "Abre OBS Studio, añade una fuente de Navegador y usa la dirección Localhost de arriba.")
        }
    }
}

@Composable
fun StepItem(number: String, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(BentoPrimary.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                color = BentoPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            color = BentoSecondaryText,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
    }
}

@Composable
fun VideoQualitySettingsPanel(viewModel: WebcamViewModel) {
    val jpegQuality by viewModel.jpegQuality.collectAsStateWithLifecycle()
    val resWidth by viewModel.resolutionWidth.collectAsStateWithLifecycle()
    val resHeight by viewModel.resolutionHeight.collectAsStateWithLifecycle()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BentoCardBg),
        shape = RoundedCornerShape(28.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BentoBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.HelpOutline,
                    contentDescription = null,
                    tint = BentoPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Ajustes de Calidad de Vídeo",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = BentoDarkText
                )
            }

            // Resolution presets
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Resolución de Cámara:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BentoDarkText
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val resolutions = listOf(
                        Triple(3840, 2160, "3840x2160 (UW)"),
                        Triple(2160, 1080, "2160x1080 (UW)"),
                        Triple(1920, 1080, "1080p (Full HD)"),
                        Triple(1280, 720, "720p (HD)"),
                        Triple(640, 480, "480p (VGA)")
                    )

                    resolutions.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { (w, h, label) ->
                                val isSelected = resWidth == w && resHeight == h
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            color = if (isSelected) BentoSelected else BentoWhite,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) BentoPrimary else BentoBorder,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            viewModel.setResolution(w, h)
                                        }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = BentoDarkText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            // Add a balancing spacer if the row has an odd number of items
                            if (rowItems.size < 2) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // Slider Quality
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Calidad de Compresión JPEG:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BentoDarkText
                    )
                    Text(
                        text = "$jpegQuality%",
                        color = BentoPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Slider(
                    value = jpegQuality.toFloat(),
                    onValueChange = { viewModel.setJpegQuality(it.toInt()) },
                    valueRange = 30f..100f,
                    colors = SliderDefaults.colors(
                        activeTrackColor = BentoPrimary,
                        inactiveTrackColor = BentoBorder,
                        thumbColor = BentoPrimary
                    )
                )
                Text(
                    text = "Valores altos dan más detalle pero requieren mejor conexión.",
                    fontSize = 11.sp,
                    color = BentoSecondaryText
                )
            }
        }
    }
}

@Composable
fun CameraSelectorPanel(viewModel: WebcamViewModel) {
    val availableCameras by viewModel.availableCameras.collectAsStateWithLifecycle()
    val selectedCameraId by viewModel.selectedCameraId.collectAsStateWithLifecycle()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BentoCardBg),
        shape = RoundedCornerShape(28.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BentoBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = null,
                    tint = BentoPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Selección de Cámara",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = BentoDarkText
                )
            }

            if (availableCameras.isEmpty()) {
                Text(
                    text = "No se detectaron cámaras disponibles. Asegúrate de conceder el permiso de cámara.",
                    fontSize = 13.sp,
                    color = BentoSecondaryText,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    availableCameras.forEach { camera ->
                        val isSelected = selectedCameraId == camera.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = if (isSelected) BentoSelected else BentoWhite,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) BentoPrimary else BentoBorder,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    viewModel.selectCamera(camera.id)
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                val icon = if (camera.lensFacing == 0) {
                                    Icons.Default.FlipCameraAndroid
                                } else {
                                    Icons.Default.PhotoCamera
                                }
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected) BentoPrimary else BentoSecondaryText,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = camera.name,
                                        color = BentoDarkText,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    val description = if (camera.lensFacing == 0) {
                                        "Cámara frontal del móvil"
                                    } else if (camera.lensFacing == 1) {
                                        "Cámara trasera principal"
                                    } else {
                                        "Cámara externa"
                                    }
                                    Text(
                                        text = description,
                                        color = BentoSecondaryText,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            RadioButton(
                                selected = isSelected,
                                onClick = { viewModel.selectCamera(camera.id) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = BentoPrimary,
                                    unselectedColor = BentoSecondaryText.copy(alpha = 0.6f)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FocusModeSettingsPanel(viewModel: WebcamViewModel) {
    val isAutofocusEnabled by viewModel.isAutofocusEnabled.collectAsStateWithLifecycle()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BentoCardBg),
        shape = RoundedCornerShape(28.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BentoBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FilterCenterFocus,
                    contentDescription = null,
                    tint = BentoPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Control de Enfoque",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = BentoDarkText
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    Pair(true, "Enfoque Automático (Continuo)"),
                    Pair(false, "Enfoque Manual (Pulsar para enfocar)")
                ).forEach { (isAuto, label) ->
                    val isSelected = isAutofocusEnabled == isAuto
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (isSelected) BentoSelected else BentoWhite,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) BentoPrimary else BentoBorder,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                if (isAutofocusEnabled != isAuto) {
                                    viewModel.toggleAutofocus()
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = label,
                                color = BentoDarkText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            val description = if (isAuto) {
                                "La cámara busca enfocar la escena continuamente."
                            } else {
                                "Toca cualquier parte de la vista previa para enfocar ahí."
                            }
                            Text(
                                text = description,
                                color = BentoSecondaryText,
                                fontSize = 11.sp
                            )
                        }
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                if (isAutofocusEnabled != isAuto) {
                                    viewModel.toggleAutofocus()
                                }
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = BentoPrimary,
                                unselectedColor = BentoSecondaryText.copy(alpha = 0.6f)
                            )
                        )
                    }
                }
            }
        }
    }
}
