package com.UIN.Tool.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ==================== 新形状系统 ====================
// 精密工具感 → 极小圆角（4dp 基准）
// 边框替代投影 → 干净利落
// ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░

val WorkbenchShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(4.dp),
    large = RoundedCornerShape(6.dp),
    extraLarge = RoundedCornerShape(8.dp),
)

object Shape {
    // 独立圆角常量
    val Xs = 2.dp
    val Sm = 4.dp
    val Md = 6.dp
    val Lg = 8.dp
    val Round = 999.dp

    // 卡片、输入框、对话框专用
    val CardShape = RoundedCornerShape(Sm)
    val InputShape = RoundedCornerShape(Sm)
    val DialogShape = RoundedCornerShape(Md)
    val ChipShape = RoundedCornerShape(Sm)
    val ButtonShape = RoundedCornerShape(Sm)
    val NavRailShape = RoundedCornerShape(0.dp)
}

// Backward-compat aliases
val ShapeXs: androidx.compose.ui.unit.Dp get() = Shape.Xs
val ShapeSm: androidx.compose.ui.unit.Dp get() = Shape.Sm
val ShapeMd: androidx.compose.ui.unit.Dp get() = Shape.Md
val ShapeLg: androidx.compose.ui.unit.Dp get() = Shape.Lg
val ShapeRound: androidx.compose.ui.unit.Dp get() = Shape.Round
val CardShape: androidx.compose.foundation.shape.RoundedCornerShape get() = Shape.CardShape
val InputShape: androidx.compose.foundation.shape.RoundedCornerShape get() = Shape.InputShape
val DialogShape: androidx.compose.foundation.shape.RoundedCornerShape get() = Shape.DialogShape
val ChipShape: androidx.compose.foundation.shape.RoundedCornerShape get() = Shape.ChipShape
val ButtonShape: androidx.compose.foundation.shape.RoundedCornerShape get() = Shape.ButtonShape
val NavRailShape: androidx.compose.foundation.shape.RoundedCornerShape get() = Shape.NavRailShape
