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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SipIntercomScreen(
    viewModel: SipIntercomViewModel
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showDialSheet by remember { mutableStateOf(false) }

    var remoteTextureView by remember { mutableStateOf<TextureView?>(null) }
    var localTextureView by remember { mutableStateOf<TextureView?>(null) }
    var isSwapped by remember { mutableStateOf(false) }
    var isPipVisible by remember { mutableStateOf(true) }

    // Pulsing Animation for Samsung-Style Incoming Call UI
    val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    LaunchedEffect(state.toastMessage) {
        state.toastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    // SAMSUNG ONEUI ANIMATED INCOMING CALL OVERLAY DIALOG
    if (state.isIncomingCall) {
        Dialog(
            onDismissRequest = { /* Prevent dismiss */ },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF0F172A))
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 40.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = EziyGreen.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, EziyGreen.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "INCOMING INTERCOM CALL",
                                color = EziyGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Ringing...",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 16.sp
                        )
                    }

                    // Pulsing Avatar Circle
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .size(150.dp)
                                    .scale(pulseScale)
                                    .clip(CircleShape)
                                    .background(EziyGreen.copy(alpha = 0.2f))
                            )
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(CircleShape)
                                    .background(EziyNavy),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhoneInTalk,
                                    contentDescription = "Incoming",
                                    tint = Color.White,
                                    modifier = Modifier.size(52.dp)
                                )
                            }
                        }

                        Text(
                            text = state.incomingCallerAddress,
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 30.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // DECLINE BUTTON
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            FloatingActionButton(
                                onClick = viewModel::declineIncomingCall,
                                containerColor = EziyRed,
                                contentColor = Color.White,
                                modifier = Modifier.size(68.dp)
                            ) {
                                Icon(Icons.Default.CallEnd, contentDescription = "Decline", modifier = Modifier.size(32.dp))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("DECLINE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // ANSWER VOICE BUTTON
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            FloatingActionButton(
                                onClick = { viewModel.answerIncomingCall(isVideo = false) },
                                containerColor = EziyGreen,
                                contentColor = Color.White,
                                modifier = Modifier.size(68.dp)
                            ) {
                                Icon(Icons.Default.PhoneInTalk, contentDescription = "Answer Voice", modifier = Modifier.size(32.dp))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("VOICE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // ANSWER VIDEO BUTTON
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            FloatingActionButton(
                                onClick = { viewModel.answerIncomingCall(isVideo = true) },
                                containerColor = EziyNavy,
                                contentColor = Color.White,
                                modifier = Modifier.size(68.dp)
                            ) {
                                Icon(Icons.Default.Videocam, contentDescription = "Answer Video", modifier = Modifier.size(32.dp))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("VIDEO", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // FULL-SCREEN PROFESSIONAL CALLING OVERLAY DIALOG WITH PIP SWAPPING & SIGNAL INDICATOR
    if (state.isCalling || state.isCallConnected) {
        Dialog(
            onDismissRequest = { /* Prevent dismiss during call */ },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFF0B132B)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Video Rendering Surfaces if Video Call
                    if (state.isVideoCall) {
                        // MAIN BACKGROUND VIDEO WINDOW
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

                        // PIP WINDOW IN BOTTOM RIGHT (SWAPPABLE & HIDEABLE)
                        if (isPipVisible) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(bottom = 120.dp, end = 20.dp)
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

                                // Swap Hint & Hide Overlay Controls
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.4f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("TAP SWAP", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    IconButton(
                                        onClick = { isPipVisible = false },
                                        modifier = Modifier.size(16.dp)
                                    ) {
                                        Icon(Icons.Default.VisibilityOff, contentDescription = "Hide", tint = Color.White, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        }
                    }

                    // Centered Caller Info & Controls Overlay
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top Bar Header with Signal Indicator
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 30.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (state.isVideoCall) "EZIY VIDEO INTERCOM" else "EZIY VOICE INTERCOM",
                                    color = EziyGreen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp
                                )
                                val mins = state.callDurationSeconds / 60
                                val secs = state.callDurationSeconds % 60
                                val statusStr = if (state.isCallConnected) {
                                    String.format(Locale.US, "%02d:%02d", mins, secs)
                                } else {
                                    "Connecting / Dialing..."
                                }
                                Text(
                                    text = statusStr,
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.Black.copy(alpha = 0.4f),
                                    border = BorderStroke(1.dp, EziyGreen.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = "📶 HD 30 FPS",
                                        color = EziyGreen,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                if (state.isVideoCall && !isPipVisible) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = { isPipVisible = true },
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.2f))
                                    ) {
                                        Icon(Icons.Default.Visibility, contentDescription = "Show PiP", tint = Color.White)
                                    }
                                }
                            }
                        }

                        // Centered Avatar & Caller Information Card
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .background(EziyNavy),
                                contentAlignment = Alignment.Center
                            ) {
                                val initial = state.targetAddress.take(1).uppercase()
                                Text(
                                    text = if (initial.isBlank()) "I" else initial,
                                    color = Color.White,
                                    fontSize = 38.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = state.targetAddress,
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )

                            if (state.isVideoCall) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.Black.copy(alpha = 0.4f)
                                ) {
                                    Text(
                                        text = "📷 Remote Video Stream Active",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        // Bottom Action Glassmorphism Bar
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(bottom = 20.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = Color.Black.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = viewModel::toggleMute,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.15f))
                                    ) {
                                        Icon(
                                            imageVector = if (state.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                            contentDescription = "Mute",
                                            tint = if (state.isMuted) EziyRed else Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    // Mid-Call Video Toggle Button (Voice <-> Video)
                                    IconButton(
                                        onClick = viewModel::toggleMidCallVideo,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(if (state.isVideoCall) EziyNavy else Color.White.copy(alpha = 0.15f))
                                    ) {
                                        Icon(
                                            imageVector = if (state.isVideoCall) Icons.Default.Videocam else Icons.Default.VideocamOff,
                                            contentDescription = "Toggle Video",
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    if (state.isVideoCall) {
                                        IconButton(
                                            onClick = viewModel::switchCamera,
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .background(Color.White.copy(alpha = 0.15f))
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Cameraswitch,
                                                contentDescription = "Switch Camera",
                                                tint = Color.White,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = viewModel::toggleSpeaker,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.15f))
                                    ) {
                                        Icon(
                                            imageVector = if (state.isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                                            contentDescription = "Speaker",
                                            tint = if (state.isSpeakerOn) EziyGreen else Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    FloatingActionButton(
                                        onClick = viewModel::hangUp,
                                        containerColor = EziyRed,
                                        contentColor = Color.White,
                                        modifier = Modifier.size(60.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CallEnd,
                                            contentDescription = "End Call",
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
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
            EziyHeader(titleText = "SIP VoIP Intercom")

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
                            text = "Enter your office PBX or Linphone SIP extension credentials to enable VoIP doorphone and reception calls.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        EziyTextField(
                            value = state.sipDomain,
                            onValueChange = viewModel::onDomainChanged,
                            label = "SIP Domain / Server IP (e.g. sip.linphone.org)"
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
                                Text("Connecting Linphone SDK...")
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
                // Top Header Pill Status
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
                                text = "${state.sipUsername}@${state.sipDomain}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        TextButton(onClick = viewModel::disconnectSip) {
                            Text("Logout", color = EziyRed, fontSize = 12.sp)
                        }
                    }
                }

                // SPEED DIAL QUICK CHIPS
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
                            text = "⚡ Quick Intercom Speed-Dial",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Reception Chip
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.startCall("reception", isVideo = false) },
                                shape = RoundedCornerShape(12.dp),
                                color = EziyNavy.copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, EziyNavy.copy(alpha = 0.3f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("🏢", fontSize = 20.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Reception", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = EziyNavy)
                                }
                            }

                            // Gate Chip
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.startCall("gate", isVideo = false) },
                                shape = RoundedCornerShape(12.dp),
                                color = EziyGreen.copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, EziyGreen.copy(alpha = 0.3f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("🚪", fontSize = 20.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Front Gate", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = EziyGreen)
                                }
                            }

                            // Security Chip
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.startCall("security", isVideo = false) },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("🛡️", fontSize = 20.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Security", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // RECENT CALL LOGS LIST
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
                            text = "🕒 Recent Intercom Call Logs",
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
