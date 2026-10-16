package `in`.eziy.attendancemaster.ui.intercom

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    LaunchedEffect(state.toastMessage) {
        state.toastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
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

    Scaffold(
        floatingActionButton = {
            if (state.isRegistered && !state.isCallConnected) {
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

            if (!state.isRegistered) {
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
                                color = EziyGreen.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "🟢 ONLINE",
                                    color = EziyGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
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

                // CALL STATUS DIALING CARD
                if (state.isCalling) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = "Dialing ${state.targetAddress}...",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // ACTIVE CONNECTED CALL OVERLAY
                if (state.isCallConnected) {
                    val mins = state.callDurationSeconds / 60
                    val secs = state.callDurationSeconds % 60
                    val timerFormatted = String.format(Locale.US, "%02d:%02d", mins, secs)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = EziyGreen.copy(alpha = 0.08f)),
                        border = BorderStroke(1.5.dp, EziyGreen)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = if (state.isVideoCall) "CONNECTED VIDEO CALL" else "CONNECTED VOICE CALL",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = EziyGreen
                            )

                            Text(
                                text = timerFormatted,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                IconButton(onClick = viewModel::toggleMute) {
                                    Icon(
                                        imageVector = if (state.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                        contentDescription = "Mute",
                                        tint = if (state.isMuted) EziyRed else EziyNavy
                                    )
                                }

                                IconButton(onClick = viewModel::toggleSpeaker) {
                                    Icon(
                                        imageVector = if (state.isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                                        contentDescription = "Speaker",
                                        tint = if (state.isSpeakerOn) EziyGreen else EziyNavy
                                    )
                                }
                            }

                            FloatingActionButton(
                                onClick = viewModel::hangUp,
                                containerColor = EziyRed,
                                contentColor = Color.White
                            ) {
                                Icon(imageVector = Icons.Default.CallEnd, contentDescription = "End Call")
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
