package com.UIN.Tool.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.UIN.Tool.ui.theme.*

// ==================== 统一间距常量 ====================
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

// ==================== 统一组件库 ====================
// 合并 UIComponents + GlassComponents
// 所有颜色、形状从 MaterialTheme 和主题文件读取
// 无硬编码色值（除透明度和特殊场景外）
// ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░

object UIComponents {

    // ======================== 签名元素：连接符 ⟩ ========================

    @Composable
    fun ConnectorMark(
        modifier: Modifier = Modifier,
        color: Color = ConnectorColor,
        size: Dp = 18.dp
    ) {
        Text(
            text = "⟩",
            style = ConnectorStyle.copy(
                fontSize = size.value.sp,
                color = color
            ),
            modifier = modifier
        )
    }

    // ======================== 按钮 ========================

    @Composable
    fun PrimaryButton(
        text: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        icon: ImageVector? = null,
        loading: Boolean = false
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier
                .height(42.dp)
                .defaultMinSize(minWidth = 72.dp),
            shape = ButtonShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.outlineVariant,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                icon?.let {
                    Icon(it, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(Spacing.xs))
                }
                Text(text, style = MaterialTheme.typography.labelLarge)
            }
        }
    }

    @Composable
    fun SecondaryButton(
        text: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        icon: ImageVector? = null
    ) {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier
                .height(42.dp)
                .defaultMinSize(minWidth = 72.dp),
            shape = ButtonShape,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            border = BorderStroke(
                width = 1.dp,
                color = if (enabled) MaterialTheme.colorScheme.outline
                        else MaterialTheme.colorScheme.outlineVariant
            ),
        ) {
            icon?.let {
                Icon(it, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(Spacing.xs))
            }
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }

    @Composable
    fun TextButton(
        text: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true
    ) {
        androidx.compose.material3.TextButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.height(42.dp),
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.primary,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }

    @Composable
    fun GhostButton(
        text: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        icon: ImageVector? = null
    ) {
        TextButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.height(42.dp),
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.primary,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            icon?.let {
                Icon(it, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(Spacing.xs))
            }
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }

    @Composable
    fun IconButton(
        icon: ImageVector,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        tint: Color? = null,
        contentDescription: String? = null
    ) {
        androidx.compose.material3.IconButton(
            onClick = onClick,
            modifier = modifier.size(40.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint ?: MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }

    // ======================== 卡片 ========================
    // 用细边框替代投影——更干净、更技术
    // 所有卡片无阴影，有边框

    @Composable
    fun Card(
        modifier: Modifier = Modifier,
        onClick: (() -> Unit)? = null,
        @Suppress("UNUSED_PARAMETER")
        elevation: Dp? = null,
        shape: Shape? = null,
        content: @Composable ColumnScope.() -> Unit
    ) {
        val finalShape = shape ?: CardShape
        val surfaceColor = MaterialTheme.colorScheme.surface
        val borderColor = MaterialTheme.colorScheme.outline

        val baseMod = modifier
            .clip(finalShape)
            .background(surfaceColor, finalShape)

        val clickMod = if (onClick != null) {
            baseMod.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
        } else baseMod

        Surface(
            modifier = clickMod,
            color = surfaceColor,
            shape = finalShape,
            border = BorderStroke(0.5.dp, borderColor),
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.md),
                content = content
            )
        }
    }

    // 保留 GlassCard 名称但改造成透明/线框风格的卡片变体
    @Composable
    fun GlassCard(
        modifier: Modifier = Modifier,
        onClick: (() -> Unit)? = null,
        content: @Composable ColumnScope.() -> Unit
    ) {
        val shape = CardShape
        val bg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)

        val baseMod = modifier
            .clip(shape)
            .background(bg, shape)

        val clickMod = if (onClick != null) {
            baseMod.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
        } else baseMod

        Surface(
            modifier = clickMod,
            color = bg,
            shape = shape,
            border = BorderStroke(0.5.dp, borderColor),
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.md),
                content = content
            )
        }
    }

    // ======================== 输入框 ========================

    @Composable
    fun TextInput(
        value: String,
        onValueChange: (String) -> Unit,
        modifier: Modifier = Modifier,
        label: String? = null,
        placeholder: String? = null,
        leadingIcon: ImageVector? = null,
        trailingIcon: ImageVector? = null,
        singleLine: Boolean = true,
        isError: Boolean = false,
        supportingText: String? = null,
        enabled: Boolean = true
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            label = label?.let { { Text(it) } },
            placeholder = placeholder?.let { { Text(it) } },
            leadingIcon = leadingIcon?.let { { Icon(it, null, modifier = Modifier.size(20.dp)) } },
            trailingIcon = trailingIcon?.let { { Icon(it, null, modifier = Modifier.size(20.dp)) } },
            singleLine = singleLine,
            isError = isError,
            supportingText = supportingText?.let { { Text(it) } },
            enabled = enabled,
            shape = InputShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                cursorColor = MaterialTheme.colorScheme.primary,
            ),
            textStyle = MaterialTheme.typography.bodyMedium,
        )
    }

    // ======================== 对话框 ========================

    @Composable
    fun ConfirmDialog(
        title: String,
        message: String,
        onConfirm: () -> Unit,
        onDismiss: () -> Unit,
        confirmText: String = "确定",
        dismissText: String = "取消"
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                )
            },
            text = {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        text = dismissText,
                        onClick = onDismiss
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    PrimaryButton(
                        text = confirmText,
                        onClick = onConfirm
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = DialogShape,
        )
    }

    @Composable
    fun LoadingDialog(
        message: String,
        onCancel: (() -> Unit)? = null
    ) {
        Dialog(
            onDismissRequest = { onCancel?.invoke() },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Surface(
                shape = DialogShape,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
                shadowElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.xl),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.height(Spacing.md))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (onCancel != null) {
                        Spacer(Modifier.height(Spacing.md))
                        GhostButton(
                            text = "取消",
                            onClick = onCancel
                        )
                    }
                }
            }
        }
    }

    // ======================== 列表项 ========================

    @Composable
    fun ListItem(
        leadingContent: @Composable (() -> Unit)? = null,
        title: String,
        subtitle: String? = null,
        trailingContent: @Composable (() -> Unit)? = null,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        Card(
            modifier = modifier,
            onClick = onClick
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                leadingContent?.invoke()
                if (leadingContent != null) Spacer(Modifier.width(Spacing.sm))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    subtitle?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                trailingContent?.invoke()
            }
        }
    }

    // ======================== 加载指示器 ========================

    @Composable
    fun FullScreenLoading(message: String = "加载中...") {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.height(Spacing.md))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    @Composable
    fun LinearProgressIndicator(progress: Float) {
        androidx.compose.material3.LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(1.5.dp))
        )
    }

    // ======================== 文本 ========================

    @Composable
    fun TitleText(
        text: String,
        modifier: Modifier = Modifier,
        color: Color? = null,
        textAlign: TextAlign? = null
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineMedium,
            color = color ?: MaterialTheme.colorScheme.onBackground,
            modifier = modifier,
            textAlign = textAlign
        )
    }

    @Composable
    fun BodyText(
        text: String,
        modifier: Modifier = Modifier,
        color: Color? = null,
        textAlign: TextAlign? = null
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = color ?: MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
            textAlign = textAlign
        )
    }

    @Composable
    fun CaptionText(
        text: String,
        modifier: Modifier = Modifier,
        color: Color? = null,
        textAlign: TextAlign? = null
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color ?: MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = modifier,
            textAlign = textAlign
        )
    }

    @Composable
    fun SectionTitle(
        text: String,
        modifier: Modifier = Modifier,
        color: Color? = null
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = color ?: MaterialTheme.colorScheme.onBackground,
            modifier = modifier.padding(vertical = Spacing.xs)
        )
    }

    @Composable
    fun CodeText(
        text: String,
        modifier: Modifier = Modifier,
        color: Color? = null
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace
            ),
            color = color ?: MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier
        )
    }

    // ======================== 开关 ========================

    @Composable
    fun ToggleSwitch(
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true
    ) {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                uncheckedTrackColor = MaterialTheme.colorScheme.outlineVariant
            )
        )
    }

    // ======================== 标签/Chip ========================

    @Composable
    fun Chip(
        label: String,
        selected: Boolean,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        leadingIcon: @Composable (() -> Unit)? = null
    ) {
        val bg by animateColorAsState(
            targetValue = if (selected) MaterialTheme.colorScheme.primary
                          else MaterialTheme.colorScheme.surface,
            label = "chip_bg"
        )
        val fg by animateColorAsState(
            targetValue = if (selected) MaterialTheme.colorScheme.onPrimary
                          else MaterialTheme.colorScheme.onSurface,
            label = "chip_fg"
        )

        AssistChip(
            onClick = onClick,
            label = { Text(text = label, style = MaterialTheme.typography.labelMedium) },
            modifier = modifier,
            enabled = enabled,
            leadingIcon = leadingIcon,
            colors = AssistChipDefaults.assistChipColors(
                containerColor = bg,
                labelColor = fg,
                leadingIconContentColor = fg
            ),
            border = if (!selected) AssistChipDefaults.assistChipBorder(
                borderColor = MaterialTheme.colorScheme.outline,
                enabled = enabled
            ) else null,
            shape = ChipShape
        )
    }

    // ======================== 空状态 ========================

    @Composable
    fun EmptyState(
        title: String,
        description: String? = null,
        icon: ImageVector = Icons.Default.Info,
        modifier: Modifier = Modifier
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(Spacing.xxl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(Spacing.md))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (!description.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    // ======================== 分割线 ========================

    @Composable
    fun Divider(
        modifier: Modifier = Modifier
    ) {
        HorizontalDivider(
            modifier = modifier,
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outline
        )
    }

    // ======================== 小节头部（带连接符） ========================

    @Composable
    fun SectionHeader(
        title: String,
        modifier: Modifier = Modifier
    ) {
        Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ConnectorMark(size = 16.dp)
            Spacer(Modifier.width(Spacing.sm))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }

    // ======================== 插件首字母头像 ========================

    @Composable
    fun PluginAvatar(
        name: String,
        modifier: Modifier = Modifier,
        size: Dp = 44.dp
    ) {
        Box(
            modifier = modifier
                .size(size)
                .background(
                    MaterialTheme.colorScheme.primaryContainer,
                    CardShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
