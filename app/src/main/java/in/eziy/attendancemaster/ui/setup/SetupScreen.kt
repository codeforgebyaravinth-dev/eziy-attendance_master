package `in`.eziy.attendancemaster.ui.setup

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.eziy.attendancemaster.ui.components.EziyButton
import `in`.eziy.attendancemaster.ui.components.EziyHeader
import `in`.eziy.attendancemaster.ui.components.EziyTextField

@Composable
fun SetupScreen(
    viewModel: SetupViewModel,
    onSetupSuccess: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            onSetupSuccess()
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        EziyHeader()

        Text(
            text = "First-time setup",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        EziyTextField(
            value = state.serverUrl,
            onValueChange = viewModel::onServerUrlChange,
            label = "Face Attendance URL (e.g. https://demo)",
            enabled = !state.isLoading
        )

        EziyTextField(
            value = state.companyName,
            onValueChange = viewModel::onCompanyNameChange,
            label = "Company Name",
            enabled = !state.isLoading
        )

        EziyTextField(
            value = state.employeeName,
            onValueChange = viewModel::onEmployeeNameChange,
            label = "Employee Name",
            enabled = !state.isLoading
        )

        EziyTextField(
            value = state.employeeId,
            onValueChange = viewModel::onEmployeeIdChange,
            label = "8-digit Employee ID / Badge ID",
            enabled = !state.isLoading
        )

        EziyTextField(
            value = state.pin,
            onValueChange = viewModel::onPinChange,
            label = "Attendance PIN",
            isPassword = true,
            enabled = !state.isLoading
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (state.isLoading) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        } else {
            EziyButton(
                text = "Authenticate & Continue",
                onClick = viewModel::authenticate
            )
        }
    }
}
