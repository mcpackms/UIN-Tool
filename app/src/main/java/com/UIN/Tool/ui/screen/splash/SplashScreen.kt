package com.UIN.Tool.ui.screen.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.UIN.Tool.ui.components.Spacing
import com.UIN.Tool.ui.components.UIComponents
import com.UIN.Tool.ui.theme.ConnectorColor
import com.UIN.Tool.utils.Constants
import kotlinx.coroutines.delay
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text

@Composable
fun SplashScreen(
    onNavigate: () -> Unit
) {
    // 脉冲动画 —— 连接符 ⟩ 像心跳一样呼吸
    val infiniteTransition = rememberInfiniteTransition(label = "splash")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // 标题淡入
    var titleVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        titleVisible = true
        delay(1200)
        onNavigate()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 连接符动画（签名）
            Text(
                text = "⟩",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Light,
                    fontSize = 48.sp,
                    color = ConnectorColor
                ),
                modifier = Modifier.alpha(pulseAlpha)
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            Text(
                text = "UIN Tool",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            Text(
                text = "插件框架 · 扩展你的设备",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Spacing.lg))

            UIComponents.Divider(modifier = Modifier.width(40.dp))

            Spacer(modifier = Modifier.height(Spacing.sm))

            Text(
                text = "v${Constants.APP_VERSION}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}
