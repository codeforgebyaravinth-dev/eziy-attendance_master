package `in`.eziy.attendancemaster.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Employee Badge ID: ${state.employeeId}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
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
