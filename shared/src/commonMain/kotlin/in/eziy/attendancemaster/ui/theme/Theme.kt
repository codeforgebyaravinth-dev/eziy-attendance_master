package `in`.eziy.attendancemaster.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = EziyNavy,
    onPrimary = EziySurface,
    primaryContainer = EziyCyanLight,
    onPrimaryContainer = EziyNavy,
    secondary = EziyGreen,
    onSecondary = EziySurface,
    error = EziyRed,
    onError = EziySurface,
    background = EziyBackground,
    onBackground = EziyTextPrimary,
    surface = EziySurface,
    onSurface = EziyTextPrimary,
    onSurfaceVariant = EziyTextSecondary
)

@Composable
fun EziyTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
