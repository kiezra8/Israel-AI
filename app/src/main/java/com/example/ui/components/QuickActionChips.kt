package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ArcCyan
import com.example.ui.theme.ArcGold
import com.example.ui.theme.ArcSurfaceCard
import com.example.ui.theme.ArcSurfaceCardBorder
import com.example.ui.theme.ArcTextPrimary

data class QuickAction(
    val label: String,
    val command: String,
    val icon: ImageVector,
    val isPrimary: Boolean = false
)

@Composable
fun QuickActionChips(
    onActionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val actions = listOf(
        QuickAction("Device Status", "Check battery and device status", Icons.Default.Info, true),
        QuickAction("Open App", "Open YouTube", Icons.Default.Apps),
        QuickAction("Set Alarm 7 AM", "Set an alarm for 7 AM", Icons.Default.Alarm),
        QuickAction("Make Call", "Call Mom", Icons.Default.Call),
        QuickAction("Send SMS", "Send SMS to Mom saying I am home", Icons.Default.Sms),
        QuickAction("Notifications", "Read my notifications", Icons.Default.Notifications),
        QuickAction("Flashlight", "Turn on flashlight", Icons.Default.FlashOn),
        QuickAction("WhatsApp Reply", "Reply on WhatsApp saying got it", Icons.Default.Message),
        QuickAction("Web Search", "Search Google for latest tech news", Icons.Default.Search)
    )

    LazyRow(
        modifier = modifier.testTag("quick_action_chips"),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(actions) { action ->
            Box(
                modifier = Modifier
                    .background(
                        color = if (action.isPrimary) ArcCyan.copy(alpha = 0.15f) else ArcSurfaceCard,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = if (action.isPrimary) ArcCyan else ArcSurfaceCardBorder,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable { onActionClick(action.command) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = action.icon,
                        contentDescription = action.label,
                        tint = if (action.isPrimary) ArcCyan else ArcGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = action.label,
                        color = ArcTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
