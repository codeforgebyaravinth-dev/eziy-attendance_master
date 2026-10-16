package `in`.eziy.attendancemaster.ui.intercom

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
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

@Composable
fun SipIntercomScreen(
    viewModel: SipIntercomViewModel
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state.toastMessage) {
        state.toastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
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
                            Text("Connecting to SIP Server...")
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
            // REGISTERED STAGE: ACTIVE INTERCOM DIALER & CALL VIEW
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = EziyGreen.copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhoneInTalk,
                                    contentDescription = "Online",
                                    tint = EziyGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "🟢 SIP ONLINE",
                                    color = EziyGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        TextButton(onClick = viewModel::disconnectSip) {
                            Text("Disconnect", color = EziyRed, fontSize = 12.sp)
                        }
                    }

                    Text(
                        text = "${state.sipUsername}@${state.sipDomain}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // DIALPAD CARD
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
                        text = "Dial Extension / Reception",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    EziyTextField(
                        value = state.targetAddress,
                        onValueChange = viewModel::onTargetAddressChanged,
                        label = "Extension ID / Target SIP URI"
                    )

                    if (state.isCalling) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Dialing ${state.targetAddress}...")
                        }
                    } else if (!state.isCallConnected) {
                        EziyButton(
                            text = "CALL RECEPTION / FRONT GATE",
                            onClick = viewModel::startCall,
                            containerColor = EziyGreen
                        )
                    }
                }
            }

            // ACTIVE CALL OVERLAY CARD
            if (state.isCallConnected) {
                val mins = state.callDurationSeconds / 60
                val secs = state.callDurationSeconds % 60
                val timerFormatted = String.format("%02d:%02d", mins, secs)

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
                            text = "CONNECTED INTERCOM CALL",
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
                                    imageVector = if (state.isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
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
        }
    }
}
