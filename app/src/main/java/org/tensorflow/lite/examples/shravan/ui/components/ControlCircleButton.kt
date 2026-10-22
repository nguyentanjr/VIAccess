package org.tensorflow.lite.examples.shravan.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlCircleButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.size(80.dp).padding(4.dp),
        onClick = { if (enabled) onClick() },
        enabled = enabled,
        shape = androidx.compose.foundation.shape.CircleShape,
        color = Color.Black.copy(alpha = 0.6f),
        contentColor = Color.White
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(40.dp))
        }
    }
}
