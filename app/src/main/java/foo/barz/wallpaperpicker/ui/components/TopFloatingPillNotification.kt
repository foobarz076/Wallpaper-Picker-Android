package foo.barz.wallpaperpicker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay

/**
 * Top floating pill banner notification displayed when a bottom sheet is active,
 * preventing notification collision and bottom occlusion.
 */
@Composable
fun TopFloatingPillNotification(
    message: String?,
    onDismiss: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    var currentMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(message) {
        if (!message.isNullOrBlank()) {
            currentMessage = message
            visible = true
            delay(2400)
            visible = false
            delay(300) // Wait for slide-out animation to complete
            onDismiss()
        }
    }

    if (visible && currentMessage != null) {
        Popup(
            alignment = Alignment.TopCenter,
            properties = PopupProperties(focusable = false)
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 16.dp, start = 20.dp, end = 20.dp)
            ) {
                val icon = when {
                    currentMessage!!.contains("收藏") -> Icons.Default.Favorite
                    currentMessage!!.contains("失败") || currentMessage!!.contains("错误") -> Icons.Default.Error
                    currentMessage!!.contains("成功") || currentMessage!!.contains("下载") -> Icons.Default.CheckCircle
                    else -> Icons.Default.Info
                }
                val iconTint = when {
                    currentMessage!!.contains("收藏") -> Color(0xFFE53935)
                    currentMessage!!.contains("失败") || currentMessage!!.contains("错误") -> MaterialTheme.colorScheme.error
                    currentMessage!!.contains("成功") || currentMessage!!.contains("下载") -> Color(0xFF2E7D32)
                    else -> MaterialTheme.colorScheme.primary
                }

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.clickable {
                        visible = false
                        onDismiss()
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = currentMessage!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
