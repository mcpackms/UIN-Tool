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

// 独立圆角常量（供自定义组件使用）
val ShapeXs = 2.dp
val ShapeSm = 4.dp
val ShapeMd = 6.dp
val ShapeLg = 8.dp
val ShapeRound = 999.dp

// 卡片、输入框、对话框专用
val CardShape = RoundedCornerShape(ShapeSm)
val InputShape = RoundedCornerShape(ShapeSm)
val DialogShape = RoundedCornerShape(ShapeMd)
val ChipShape = RoundedCornerShape(ShapeSm)
val ButtonShape = RoundedCornerShape(ShapeSm)
val NavRailShape = RoundedCornerShape(0.dp)
