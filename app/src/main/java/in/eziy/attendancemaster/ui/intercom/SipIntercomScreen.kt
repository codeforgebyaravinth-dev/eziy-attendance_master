package `in`.eziy.attendancemaster.ui.intercom

import android.graphics.SurfaceTexture
import android.view.TextureView
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import `in`.eziy.attendancemaster.ui.components.EziyButton
import `in`.eziy.attendancemaster.ui.components.EziyHeader
import `in`.eziy.attendancemaster.ui.components.EziyTextField
import `in`.eziy.attendancemaster.ui.theme.EziyGreen
import `in`.eziy.attendancemaster.ui.theme.EziyNavy
import `in`.eziy.attendancemaster.ui.theme.EziyRed
import java.util.Locale

@Composable
fun SipIntercomCallOverlay(
    viewModel: SipIntercomViewModel
) {
    val state by viewModel.uiState.collectAsState()

    var remoteTextureView by remember { mutableStateOf<TextureView?>(null) }
    var localTextureView by remember { mutableStateOf<TextureView?>(null) }
    var isSwapped by remember { mutableStateOf(false) }
    var isPipVisible by remember { mutableStateOf(true) }

    // Pulsing Radial Aura Animation for Incoming Call
    val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    // GLOBAL INCOMING CALL OVERLAY DIALOG
    if (state.isIncomingCall) {
        Dialog(
            onDismissRequest = { /* Prevent dismiss */ },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF0A0F1D))
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Header Pill
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 24.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF1E293B).copy(alpha = 0.8f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(EziyGreen)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (state.isVideoCall) "INCOMING VIDEO CALL" else "INCOMING AUDIO CALL",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Caller Name & Subtitle
                        Text(
                            text = state.incomingCallerAddress,
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Intercom Extension • ${state.companyName}",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Badges Row
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("📶", fontSize = 10.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("HD VOICE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = "Security", tint = Color.White, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("ENCRYPTED", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Centered Animated Avatar Circle
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(240.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(Color(0xFF00AEEF).copy(alpha = 0.25f), Color.Transparent)
                                    )
                                )
                        )
                        Box(
                            modifier = Modifier
                                .size(170.dp)
                                .clip(CircleShape)
                                .background(EziyNavy),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = state.incomingCallerAddress.take(1).uppercase().ifBlank { "E" },
                                color = Color.White,
                                fontSize = 56.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Ringing Pill
                        Surface(
                            modifier = Modifier.align(Alignment.BottomCenter),
                            shape = RoundedCornerShape(16.dp),
                            color = Color.Black.copy(alpha = 0.6f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("📊", fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("CALL RINGING...", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Decline / Accept Buttons
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                FloatingActionButton(
                                    onClick = viewModel::declineIncomingCall,
                                    containerColor = EziyRed,
                                    contentColor = Color.White,
                                    modifier = Modifier.size(76.dp),
                                    shape = CircleShape
                                ) {
                                    Icon(Icons.Default.CallEnd, contentDescription = "Decline", modifier = Modifier.size(34.dp))
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Decline", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                FloatingActionButton(
                                    onClick = { viewModel.answerIncomingCall(isVideo = state.isVideoCall) },
                                    containerColor = EziyGreen,
                                    contentColor = Color.White,
                                    modifier = Modifier.size(76.dp),
                                    shape = CircleShape
                                ) {
                                    Icon(Icons.Default.PhoneInTalk, contentDescription = "Accept", modifier = Modifier.size(34.dp))
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Accept", color = EziyGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Text("^ SWIPE UP TO ANSWER", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                    }
                }
            }
        }
    }

    // GLOBAL ACTIVE VOICE / VIDEO CALL OVERLAY DIALOG
    if (state.isCalling || state.isCallConnected) {
        Dialog(
            onDismissRequest = { /* Prevent dismiss */ },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFF0F172A)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Full-Screen Video Surface
                    if (state.isVideoCall) {
                        AndroidView(
                            factory = { ctx ->
                                TextureView(ctx).apply {
                                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                                        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                                            if (!isSwapped) remoteTextureView = this@apply else localTextureView = this@apply
                                            viewModel.setVideoSurfaces(
                                                if (!isSwapped) remoteTextureView else localTextureView,
                                                if (!isSwapped) localTextureView else remoteTextureView
                                            )
                                        }
                                        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
                                        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true
                                        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Top-Right Rounded PiP Box ("🟢 You")
                        if (isPipVisible) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 60.dp, end = 20.dp)
                                    .size(width = 110.dp, height = 150.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .clickable { isSwapped = !isSwapped }
                            ) {
                                AndroidView(
                                    factory = { ctx ->
                                        TextureView(ctx).apply {
                                            surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                                                override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                                                    if (!isSwapped) localTextureView = this@apply else remoteTextureView = this@apply
                                                    viewModel.setVideoSurfaces(
                                                        if (!isSwapped) remoteTextureView else localTextureView,
                                                        if (!isSwapped) localTextureView else remoteTextureView
                                                    )
                                                }
                                                override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
                                                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true
                                                override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )

                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(6.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.Black.copy(alpha = 0.6f)
                                ) {
                                    Text("🟢 You", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        }
                    }

                    // Main Call Content Overlay
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top Connected Status Bar
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(top = 28.dp)
                        ) {
                            val mins = state.callDurationSeconds / 60
                            val secs = state.callDurationSeconds % 60
                            val timerStr = String.format(Locale.US, "%02d:%02d", mins, secs)

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.Black.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("🎧", fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (state.isCallConnected) "Connected • $timerStr" else "Calling...",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("📶 5G HD", color = EziyGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // VOICE CALL CENTERED AVATAR & NAME
                        if (!state.isVideoCall) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Box(
                                        modifier = Modifier
                                            .size(160.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.radialGradient(
                                                    colors = listOf(Color(0xFF00AEEF).copy(alpha = 0.2f), Color.Transparent)
                                                )
                                            )
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(120.dp)
                                            .clip(CircleShape)
                                            .background(EziyNavy),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = state.targetAddress.take(1).uppercase().ifBlank { "E" },
                                            color = Color.White,
                                            fontSize = 48.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = state.targetAddress,
                                        color = Color.White,
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (state.isCallConnected) "📊 Connected • HD Voice" else "📊 Calling...",
                                        color = EziyGreen,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            // CLEAN 3-BUTTON STREAMLINED ACTION ROW FOR VOICE CALL
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 1. Mute Button
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    IconButton(
                                        onClick = viewModel::toggleMute,
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(if (state.isMuted) EziyRed else Color.White.copy(alpha = 0.15f))
                                    ) {
                                        Icon(if (state.isMuted) Icons.Default.MicOff else Icons.Default.Mic, contentDescription = "Mute", tint = Color.White, modifier = Modifier.size(28.dp))
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Mute", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                                }

                                // 2. Video Upgrade Button
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    IconButton(
                                        onClick = viewModel::toggleMidCallVideo,
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.15f))
                                    ) {
                                        Icon(Icons.Default.Videocam, contentDescription = "Video", tint = Color.White, modifier = Modifier.size(28.dp))
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Video", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                                }

                                // 3. Speaker Button
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    IconButton(
                                        onClick = viewModel::toggleSpeaker,
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(if (state.isSpeakerOn) Color(0xFF7C3AED) else Color.White.copy(alpha = 0.15f))
                                    ) {
                                        Icon(if (state.isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff, contentDescription = "Speaker", tint = Color.White, modifier = Modifier.size(28.dp))
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Speaker", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                                }
                            }
                        } else {
                            // VIDEO CALL BOTTOM GLASSMORPHISM BAR
                            Spacer(modifier = Modifier.height(1.dp))
                        }

                        // Bottom Action Controls
                        if (state.isVideoCall) {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = Color.Black.copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = viewModel::toggleMute) {
                                        Icon(if (state.isMuted) Icons.Default.MicOff else Icons.Default.Mic, contentDescription = "Mute", tint = Color.White)
                                    }
                                    IconButton(onClick = viewModel::toggleMidCallVideo) {
                                        Icon(Icons.Default.Videocam, contentDescription = "Video", tint = Color.White)
                                    }
                                    IconButton(onClick = viewModel::switchCamera) {
                                        Icon(Icons.Default.Cameraswitch, contentDescription = "Camera", tint = Color.White)
                                    }
                                    IconButton(onClick = viewModel::toggleSpeaker) {
                                        Icon(if (state.isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff, contentDescription = "Speaker", tint = if (state.isSpeakerOn) EziyGreen else Color.White)
                                    }
                                    FloatingActionButton(
                                        onClick = viewModel::hangUp,
                                        containerColor = EziyRed,
                                        contentColor = Color.White,
                                        modifier = Modifier.size(52.dp),
                                        shape = CircleShape
                                    ) {
                                        Icon(Icons.Default.CallEnd, contentDescription = "End Call")
                                    }
                                }
                            }
                        } else {
                            // Voice Call Large Center Red FAB
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                FloatingActionButton(
                                    onClick = viewModel::hangUp,
                                    containerColor = EziyRed,
                                    contentColor = Color.White,
                                    modifier = Modifier.size(72.dp),
                                    shape = CircleShape
                                ) {
                                    Icon(Icons.Default.CallEnd, contentDescription = "End Call", modifier = Modifier.size(34.dp))
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("END CALL", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SipIntercomScreen(
    viewModel: SipIntercomViewModel
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showDialSheet by remember { mutableStateOf(false) }

    var editingSpeedDialItem by remember { mutableStateOf<SpeedDialItem?>(null) }
    var editName by remember { mutableStateOf("") }
    var editExt by remember { mutableStateOf("") }
    var editEmoji by remember { mutableStateOf("📞") }

    LaunchedEffect(state.toastMessage) {
        state.toastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    if (editingSpeedDialItem != null) {
        AlertDialog(
            onDismissRequest = { editingSpeedDialItem = null },
            title = { Text("Customize Speed Dial #${editingSpeedDialItem?.id}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    EziyTextField(
                        value = editEmoji,
                        onValueChange = { editEmoji = it },
                        label = "Icon Emoji (e.g. 🏢, 🚪, 🛡️)"
                    )
                    EziyTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = "Button Name (e.g. Reception)"
                    )
                    EziyTextField(
                        value = editExt,
                        onValueChange = { editExt = it },
                        label = "Target Extension (e.g. 101)"
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        editingSpeedDialItem?.let { item ->
                            viewModel.updateSpeedDialItem(item.id, editName, editExt, editEmoji)
                        }
                        editingSpeedDialItem = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EziyNavy)
                ) {
                    Text("SAVE")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingSpeedDialItem = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDialSheet) {
        ModalBottomSheet(
            onDismissRequest = { showDialSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Dial Extension / Target URI",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                EziyTextField(
                    value = state.targetAddress,
                    onValueChange = viewModel::onTargetAddressChanged,
                    label = "Target Extension (e.g. reception or 101)"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            showDialSheet = false
                            viewModel.startCall(isVideo = false)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EziyGreen)
                    ) {
                        Icon(Icons.Default.PhoneInTalk, contentDescription = "Voice")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("VOICE CALL", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            showDialSheet = false
                            viewModel.startCall(isVideo = true)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EziyNavy)
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = "Video")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("VIDEO CALL", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    val isConfigured = state.isRegistered || state.isConnecting || state.sipUsername.isNotBlank()
    val headerTitle = "${state.companyName.uppercase()} INTERCOM"

    Scaffold(
        floatingActionButton = {
            if (isConfigured && !state.isCallConnected && !state.isCalling && !state.isIncomingCall) {
                ExtendedFloatingActionButton(
                    onClick = { showDialSheet = true },
                    containerColor = EziyNavy,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.Dialpad, contentDescription = "Dial") },
                    text = { Text("DIAL EXTENSION", fontWeight = FontWeight.Bold) }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            EziyHeader(titleText = headerTitle)

            if (!isConfigured) {
                // UNREGISTERED STAGE: SIP LOGIN FORM
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Intercom SIP Login Setup",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Enter your office PBX extension credentials to enable VoIP doorphone and reception calls.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        EziyTextField(
                            value = state.sipDomain,
                            onValueChange = viewModel::onDomainChanged,
                            label = "SIP Domain / Server IP (e.g. 192.168.1.100 or vspl.com)"
                        )

                        EziyTextField(
                            value = state.sipUsername,
                            onValueChange = viewModel::onUsernameChanged,
                            label = "Extension ID / Username (e.g. 101)"
                        )

                        EziyTextField(
                            value = state.sipPassword,
                            onValueChange = viewModel::onPasswordChanged,
                            label = "SIP Password",
                            isPassword = true
                        )

                        if (state.isConnecting) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Connecting SIP Server...")
                            }
                        } else {
                            EziyButton(
                                text = "CONNECT SIP INTERCOM",
                                onClick = viewModel::connectSip,
                                containerColor = EziyNavy
                            )
                        }
                    }
                }
            } else {
                // REGISTERED STAGE: SLEEK DASHBOARD
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (state.isRegistered) EziyGreen.copy(alpha = 0.12f) else EziyNavy.copy(alpha = 0.12f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (state.isConnecting) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(12.dp),
                                            strokeWidth = 2.dp,
                                            color = EziyNavy
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text(
                                        text = if (state.isRegistered) "🟢 ONLINE" else "🔄 AUTO-CONNECTING...",
                                        color = if (state.isRegistered) EziyGreen else EziyNavy,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (state.sipDomain.isNotBlank()) "${state.sipUsername}@${state.sipDomain}" else state.sipUsername,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        TextButton(onClick = viewModel::disconnectSip) {
                            Text("Logout", color = EziyRed, fontSize = 12.sp)
                        }
                    }
                }

                // 3x3 GRID (9 SPEED DIAL CHIPS WITH EDIT MODAL)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚡ Quick Intercom Speed-Dial (3x3 Grid)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(text = "Tap to call", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        // 3x3 Grid
                        val chunked = state.speedDialItems.chunked(3)
                        chunked.forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowItems.forEach { item ->
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { viewModel.startCall(item.extension, isVideo = false) },
                                        shape = RoundedCornerShape(12.dp),
                                        color = EziyNavy.copy(alpha = 0.08f),
                                        border = BorderStroke(1.dp, EziyNavy.copy(alpha = 0.25f))
                                    ) {
                                        Box(modifier = Modifier.padding(8.dp)) {
                                            IconButton(
                                                onClick = {
                                                    editEmoji = item.iconEmoji
                                                    editName = item.name
                                                    editExt = item.extension
                                                    editingSpeedDialItem = item
                                                },
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .align(Alignment.TopEnd)
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = EziyNavy.copy(alpha = 0.7f), modifier = Modifier.size(12.dp))
                                            }

                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 4.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(item.iconEmoji, fontSize = 18.sp)
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(item.name, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = EziyNavy, maxLines = 1)
                                                Text(item.extension, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // CONTACTS DIRECTORY SECTION
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "👥 Intercom Contacts Directory",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        state.contacts.forEachIndexed { idx, contact ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(EziyNavy.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(contact.name.take(1).uppercase(), fontWeight = FontWeight.Bold, color = EziyNavy)
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(contact.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("${contact.role} • #${contact.extension}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = { viewModel.startCall(contact.extension, isVideo = false) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.PhoneInTalk, contentDescription = "Voice", tint = EziyGreen, modifier = Modifier.size(20.dp))
                                    }
                                    IconButton(
                                        onClick = { viewModel.startCall(contact.extension, isVideo = true) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Videocam, contentDescription = "Video", tint = EziyNavy, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                            if (idx < state.contacts.lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                            }
                        }
                    }
                }

                // SCROLLABLE RECENT CALL LOGS LIST
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "🕒 Recent Intercom Call History",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        if (state.callLogs.isEmpty()) {
                            Text(
                                text = "No recent intercom call history.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                state.callLogs.forEachIndexed { idx, log ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.startCall(log.extension, isVideo = log.isVideo) }
                                            .padding(vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (log.isOutgoing) Icons.AutoMirrored.Filled.CallMade else Icons.AutoMirrored.Filled.CallReceived,
                                                contentDescription = "Call Type",
                                                tint = if (log.isOutgoing) EziyGreen else EziyNavy,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(text = log.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                                Text(
                                                    text = "${log.extension} • ${log.durationText}",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Text(text = log.timeAgo, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (idx < state.callLogs.lastIndex) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
