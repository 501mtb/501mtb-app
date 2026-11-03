package mg.madagascar.mtb501

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ClubColors = darkColorScheme(
    primary = Color(0xFF06FF06),
    onPrimary = Color(0xFF001A00),
    background = Color(0xFF000000),
    onBackground = Color(0xFFE5E5E7),
    surface = Color(0xFF171717),
    onSurface = Color(0xFFE5E5E7),
    secondary = Color(0xFF8CFF8C)
)

@Composable
fun ClubTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ClubColors,
        content = content
    )
}
