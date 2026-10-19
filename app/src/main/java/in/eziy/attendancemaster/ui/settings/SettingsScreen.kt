package `in`.eziy.attendancemaster.ui.settings

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.eziy.attendancemaster.ui.components.EziyButton
import `in`.eziy.attendancemaster.ui.components.EziyHeader
import `in`.eziy.attendancemaster.ui.components.EziyTextField
import `in`.eziy.attendancemaster.ui.theme.EziyRed

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: (() -> Unit)? = null,
    onLogout: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            Toast.makeText(context, "Settings Saved", Toast.LENGTH_SHORT).show()
            viewModel.resetSaveState()
            onNavigateBack?.invoke()
        }
    }

    LaunchedEffect(state.isLoggedOut) {
        if (state.isLoggedOut) {
            onLogout()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        EziyHeader()

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onNavigateBack != null) {
                Button(onClick = onNavigateBack) {
                    Text("← Back")
                }
                Spacer(modifier = Modifier.width(16.dp))
            }
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        // MAIN ODOO API SETTINGS CARD
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
                    text = "Odoo ERP Connection",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                EziyTextField(
                    value = state.serverUrl,
                    onValueChange = viewModel::onServerUrlChange,
                    label = "Server URL"
                )

                EziyTextField(
                    value = state.companyName,
                    onValueChange = viewModel::onCompanyNameChange,
                    label = "Company Name"
                )

                EziyTextField(
                    value = state.employeeName,
                    onValueChange = viewModel::onEmployeeNameChange,
                    label = "Employee Name"
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Employee Badge ID: ${state.employeeId}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // SIP INTERCOM CONFIGURATION CARD
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
                    text = "📞 SIP VoIP Intercom Configuration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Configure your office PBX or Linphone extension for doorphone and reception VoIP calls.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                EziyTextField(
                    value = state.sipDomain,
                    onValueChange = viewModel::onSipDomainChange,
                    label = "SIP Domain / Server IP (e.g. sip.linphone.org)"
                )

                EziyTextField(
                    value = state.sipUsername,
                    onValueChange = viewModel::onSipUsernameChange,
                    label = "Extension ID / Username (e.g. 101)"
                )

                EziyTextField(
                    value = state.sipPassword,
                    onValueChange = viewModel::onSipPasswordChange,
                    label = "SIP Password",
                    isPassword = true
                )
            }
        }

        EziyButton(
            text = "SAVE CHANGES",
            onClick = viewModel::saveSettings
        )

        EziyButton(
            text = "LOG OUT / CHANGE EMPLOYEE",
            onClick = viewModel::logout,
            containerColor = EziyRed,
            contentColor = MaterialTheme.colorScheme.onError
        )
    }
}
