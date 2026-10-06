package `in`.eziy.attendancemaster.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = EziyNavy,
    onPrimary = Color.White,
    primaryContainer = EziyNavy,
    onPrimaryContainer = Color.White,
    secondary = EziyCyan,
    onSecondary = Color.Black,
    secondaryContainer = EziyCyanLight,
    background = Color.White,
    onBackground = Color(0xFF111827),
    surface = Color.White,
    onSurface = Color(0xFF111827),
    surfaceVariant = Color(0xFFF3F4F6),
    onSurfaceVariant = Color(0xFF1F2937)
)

private val DarkColorScheme = darkColorScheme(
    primary = EziyNavy,
    onPrimary = Color.White,
    primaryContainer = EziyNavy,
    onPrimaryContainer = Color.White,
    secondary = EziyCyan,
    onSecondary = Color.Black,
    secondaryContainer = EziyCyanLight,
    background = Color.White,
    onBackground = Color(0xFF111827),
    surface = Color.White,
    onSurface = Color(0xFF111827),
    surfaceVariant = Color(0xFFF3F4F6),
    onSurfaceVariant = Color(0xFF1F2937)
)

@Composable
fun EziyTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
