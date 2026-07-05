package com.UIN.Tool.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ==================== 新设计系统色板 ====================
// 暖暗色 + 冷翠绿 —— 开发者工具，但不过分冰冷
// ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░

// —— 基础色 ——
val WorkbenchGreen = Color(0xFF4AE0C5)    // 主色：翠绿，鲜活的技术感
val WorkbenchGreenDark = Color(0xFF0D8A7A) // 亮色主色
val WorkbenchAmber = Color(0xFFF5A623)    // 强调色：琥珀，温暖交互
val WorkbenchAmberDark = Color(0xFFD4890B)

// —— 暗色模式 ——
val DarkBg = Color(0xFF0E1217)            // 暖调深灰背景
val DarkSurface = Color(0xFF1A1F26)        // 面板色
val DarkSurfaceElevated = Color(0xFF222830) // 抬升面板
val DarkBorder = Color(0xFF272C35)         // 细边框
val DarkTextPrimary = Color(0xFFE8EAED)    // 正文主色
val DarkTextSecondary = Color(0xFF9AA0AB)  // 辅助文字
val DarkTextDisabled = Color(0xFF505864)   // 禁用文字
val DarkGreenDim = Color(0xFF2D8F7E)       // 主色暗化（容器、辉光）
val DarkAmberDim = Color(0xFF8A6D2B)       // 强调色暗化

// —— 亮色模式 ——
val LightBg = Color(0xFFF5F5F0)           // 暖白背景
val LightSurface = Color(0xFFFFFFFF)
val LightBorder = Color(0xFFE2E4E0)        // 细边框
val LightTextPrimary = Color(0xFF1A1A1A)
val LightTextSecondary = Color(0xFF6B7280)
val LightTextDisabled = Color(0xFFB0B5BC)
val LightGreenDim = Color(0xFFD1F2EB)      // 主色容器
val LightAmberDim = Color(0xFFFEF0D5)

// —— 语义色 ——
val StatusError = Color(0xFFE5484D)
val StatusWarning = Color(0xFFF5A623)
val StatusSuccess = Color(0xFF30A46B)
val StatusInfo = Color(0xFF3B82F6)

// ==================== Material 3 配色方案 ====================

private val LightColorScheme = lightColorScheme(
    primary = WorkbenchGreenDark,
    onPrimary = Color.White,
    primaryContainer = LightGreenDim,
    onPrimaryContainer = Color(0xFF0B3D33),
    secondary = WorkbenchAmberDark,
    onSecondary = Color.White,
    secondaryContainer = LightAmberDim,
    onSecondaryContainer = Color(0xFF3D2E0A),
    tertiary = StatusInfo,
    onTertiary = Color.White,
    background = LightBg,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = Color(0xFFEEEFEC),
    onSurfaceVariant = LightTextSecondary,
    error = StatusError,
    onError = Color.White,
    outline = LightBorder,
    outlineVariant = Color(0xFFD0D2CE),
    inverseSurface = DarkBg,
    inverseOnSurface = DarkTextPrimary,
    inversePrimary = WorkbenchGreen,
    surfaceTint = WorkbenchGreenDark,
)

private val DarkColorScheme = darkColorScheme(
    primary = WorkbenchGreen,
    onPrimary = Color(0xFF00382F),
    primaryContainer = DarkGreenDim,
    onPrimaryContainer = Color(0xFFB0F0E0),
    secondary = WorkbenchAmber,
    onSecondary = Color(0xFF2D1F00),
    secondaryContainer = DarkAmberDim,
    onSecondaryContainer = Color(0xFFFDE2A0),
    tertiary = StatusInfo,
    onTertiary = Color.White,
    background = DarkBg,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = DarkTextSecondary,
    error = StatusError,
    onError = Color.White,
    outline = DarkBorder,
    outlineVariant = Color(0xFF2F3540),
    inverseSurface = LightSurface,
    inverseOnSurface = LightTextPrimary,
    inversePrimary = WorkbenchGreenDark,
    surfaceTint = WorkbenchGreen,
)

// ==================== 签名色：连接符 ⟩ 专用色 ====================
// 在所有界面中保持一致
val ConnectorColor get() = WorkbenchGreen
val ConnectorColorAmber get() = WorkbenchAmber

// ==================== 主题函数 ====================

@Composable
fun UINToolTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = WorkbenchTypography,
        shapes = WorkbenchShapes,
        content = content
    )
}
