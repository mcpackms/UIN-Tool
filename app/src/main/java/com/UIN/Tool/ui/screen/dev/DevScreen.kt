package com.UIN.Tool.ui.screen.dev

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.UIN.Tool.log.Logger
import com.UIN.Tool.ui.components.Spacing
import com.UIN.Tool.ui.components.UIComponents
import com.UIN.Tool.utils.Constants
import java.io.File

@Composable
fun DevScreen() {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        UIComponents.SectionHeader("开发工台")

        // 创建插件卡片
        UIComponents.Card {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    text = "插件开发工具",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                UIComponents.PrimaryButton(
                    text = "创建原生插件",
                    icon = Icons.Default.Code,
                    onClick = {
                        try {
                            val intent = Intent(context, BasePluginWizardActivity::class.java)
                            intent.putExtra("ui_type", "native")
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Logger.e("DevScreen", "跳转原生插件向导失败", e)
                            Toast.makeText(context, "功能开发中: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                UIComponents.PrimaryButton(
                    text = "创建 Web 插件",
                    icon = Icons.Default.Language,
                    onClick = {
                        try {
                            val intent = Intent(context, BasePluginWizardActivity::class.java)
                            intent.putExtra("ui_type", "web")
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Logger.e("DevScreen", "跳转Web插件向导失败", e)
                            Toast.makeText(context, "功能开发中: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                UIComponents.SecondaryButton(
                    text = "导出模板",
                    icon = Icons.Default.FileDownload,
                    onClick = { exportTemplates(context) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // 快速开始卡片
        UIComponents.Card {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    text = "快速开始",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                val steps = listOf(
                    "选择「创建新插件」",
                    "选择插件类型（原生 / Web）",
                    "填写插件信息",
                    "编写代码",
                    "导出 TPK 文件",
                    "在「管理」中导入运行"
                )
                steps.forEachIndexed { index, step ->
                    Row(
                        verticalAlignment = Alignment.Top
                    ) {
                        UIComponents.ConnectorMark(
                            size = 12.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Spacer(Modifier.width(Spacing.sm))
                        Text(
                            text = step,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun exportTemplates(context: android.content.Context) {
    try {
        val templateDir = File(Constants.WORK_DIR, "templates")
        if (!templateDir.exists()) {
            templateDir.mkdirs()
        }

        val assetFiles = listOf(
            "templates/native_template.tpk" to "native_plugin_template.tpk",
            "templates/web_template.tpk" to "web_plugin_template.tpk",
            "docs/README.md" to "docs/README.md",
            "docs/Help.md" to "docs/Help.md",
            "docs/About.md" to "docs/About.md",
            "docs/CONTRIBUTORS.md" to "docs/CONTRIBUTORS.md",
            "docs/CHANGELOG.md" to "docs/CHANGELOG.md"
        )

        for ((assetPath, fileName) in assetFiles) {
            try {
                val destFile = File(templateDir, fileName)
                destFile.parentFile?.mkdirs()
                context.assets.open(assetPath).use { input ->
                    java.io.FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                Logger.w("DevScreen", "复制文件失败: $assetPath")
            }
        }

        Toast.makeText(context, "模板已导出到: ${templateDir.absolutePath}", Toast.LENGTH_LONG).show()
        Logger.success("DevScreen", "模板导出成功: ${templateDir.absolutePath}")

    } catch (e: Exception) {
        Logger.e("DevScreen", "导出模板失败", e)
        Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
