// app/src/main/java/com/UIN/Tool/ui/screen/dev/PluginWizardSteps.kt
package com.UIN.Tool.ui.screen.dev

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.UIN.Tool.ui.components.Spacing
import com.UIN.Tool.ui.components.UIComponents
import com.UIN.Tool.utils.FileUtils
import com.UIN.Tool.utils.formatFileSize
import java.io.File

// ==================== 辅助函数 ====================

private fun loadBitmapFromFile(path: String): Bitmap? {
    return try {
        BitmapFactory.decodeFile(path)
    } catch (e: Exception) {
        null
    }
}

private fun handleIconSelection(
    context: android.content.Context,
    uri: Uri,
    onIconSelected: (String) -> Unit
) {
    try {
        val tempFile = File(context.cacheDir, "temp_icon_${System.currentTimeMillis()}.png")
        if (FileUtils.copyUriToFile(context, uri, tempFile)) {
            onIconSelected(tempFile.absolutePath)
        }
    } catch (e: Exception) {
        // 忽略异常
    }
}

private fun handleResourceSelection(
    context: android.content.Context,
    uri: Uri,
    onResourceAdded: (String) -> Unit
) {
    try {
        val fileName = uri.lastPathSegment?.substringAfterLast("/") ?: "resource"
        val tempFile = File(context.cacheDir, "temp_res_${System.currentTimeMillis()}_$fileName")
        if (FileUtils.copyUriToFile(context, uri, tempFile)) {
            onResourceAdded(tempFile.absolutePath)
        }
    } catch (e: Exception) {
        // 忽略异常
    }
}

// ==================== 步骤1：配置信息 ====================

@Composable
fun PluginConfigStep(
    pluginId: String,
    onPluginIdChange: (String) -> Unit,
    pluginName: String,
    onPluginNameChange: (String) -> Unit,
    pluginAuthor: String,
    onPluginAuthorChange: (String) -> Unit,
    pluginDescription: String,
    onPluginDescriptionChange: (String) -> Unit,
    pluginVersion: String,
    onPluginVersionChange: (String) -> Unit,
    pluginVersionName: String,
    onPluginVersionNameChange: (String) -> Unit,
    mainClass: String,
    onMainClassChange: (String) -> Unit,
    entryPath: String,
    onEntryPathChange: (String) -> Unit,
    uiType: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        UIComponents.SectionHeader("基本信息")

        UIComponents.TextInput(
            value = pluginId,
            onValueChange = onPluginIdChange,
            label = "插件 ID",
            placeholder = "com.example.myplugin",
            modifier = Modifier.fillMaxWidth(),
            isError = pluginId.isNotEmpty() && !pluginId.matches(Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$")),
            supportingText = if (pluginId.isNotEmpty() && !pluginId.matches(Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$"))) {
                "格式应为域名倒序，如 com.example.myplugin"
            } else null
        )

        UIComponents.TextInput(
            value = pluginName,
            onValueChange = onPluginNameChange,
            label = "插件名称",
            placeholder = "我的插件",
            modifier = Modifier.fillMaxWidth()
        )

        UIComponents.TextInput(
            value = pluginAuthor,
            onValueChange = onPluginAuthorChange,
            label = "作者",
            placeholder = "开发者",
            modifier = Modifier.fillMaxWidth()
        )

        UIComponents.TextInput(
            value = pluginDescription,
            onValueChange = onPluginDescriptionChange,
            label = "描述",
            placeholder = "这是一个示例插件",
            modifier = Modifier.fillMaxWidth(),
            singleLine = false
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            UIComponents.TextInput(
                value = pluginVersion,
                onValueChange = onPluginVersionChange,
                label = "版本号",
                placeholder = "1",
                modifier = Modifier.weight(1f)
            )
            UIComponents.TextInput(
                value = pluginVersionName,
                onValueChange = onPluginVersionNameChange,
                label = "版本名",
                placeholder = "1.0.0",
                modifier = Modifier.weight(1f)
            )
        }

        if (uiType == "native") {
            UIComponents.TextInput(
                value = mainClass,
                onValueChange = onMainClassChange,
                label = "主类名",
                placeholder = "com.example.MainPlugin",
                modifier = Modifier.fillMaxWidth(),
                isError = mainClass.isNotEmpty() && !mainClass.contains("."),
                supportingText = if (mainClass.isNotEmpty() && !mainClass.contains(".")) {
                    "必须包含包名，如 com.example.MainPlugin"
                } else null
            )
        } else {
            UIComponents.TextInput(
                value = entryPath,
                onValueChange = onEntryPathChange,
                label = "入口文件",
                placeholder = "web/index.html",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ==================== 步骤2：图标选择 ====================

@Composable
fun PluginIconStep(
    iconPath: String,
    onIconSelected: (String) -> Unit
) {
    val context = LocalContext.current

    val iconPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            handleIconSelection(context, uri, onIconSelected)
        }
    }

    val bitmap = if (iconPath.isNotEmpty() && File(iconPath).exists()) {
        loadBitmapFromFile(iconPath)
    } else {
        null
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        UIComponents.SectionHeader("选择图标")

        Spacer(Modifier.height(Spacing.lg))

        // 图标预览
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(Spacing.sm))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "图标预览",
                    modifier = Modifier.size(80.dp)
                )
            } else {
                Icon(
                    Icons.Default.Image,
                    contentDescription = "图标预览",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.md))

        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            UIComponents.PrimaryButton(
                text = if (iconPath.isNotEmpty()) "更换图标" else "选择图标",
                icon = Icons.Default.FileUpload,
                onClick = { iconPickerLauncher.launch("image/*") }
            )
            if (iconPath.isNotEmpty()) {
                UIComponents.SecondaryButton(
                    text = "移除图标",
                    onClick = { onIconSelected("") }
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.sm))

        UIComponents.CaptionText("建议 128×128 PNG")
    }
}

// ==================== 步骤3：原生代码 ====================

@Composable
fun NativeCodeStep(
    onOpenEditor: () -> Unit,
    fileCount: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        UIComponents.SectionHeader("代码编辑")

        UIComponents.Card(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    text = "原生插件开发提示",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                val hints = listOf(
                    "必须实现 PluginInterface 接口的所有方法",
                    "UI 必须通过 Java 代码动态创建（不能使用 XML）",
                    "示例代码已包含基础的 LinearLayout + Button",
                    "可添加任何原生 Android 控件",
                    "修改后需重新编译生成 DEX"
                )
                hints.forEach { hint ->
                    Row(verticalAlignment = Alignment.Top) {
                        UIComponents.ConnectorMark(
                            size = 10.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        )
                        Spacer(Modifier.width(Spacing.xs))
                        Text(
                            text = hint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(Spacing.xs))

                Text(
                    text = "当前已生成 ${fileCount} 个代码文件",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        UIComponents.PrimaryButton(
            text = "打开代码编辑器（${fileCount} 个文件）",
            icon = Icons.Default.Edit,
            onClick = onOpenEditor,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ==================== 步骤3：Web配置 ====================

@Composable
fun WebConfigStep(
    templateType: Int,
    onTemplateTypeChange: (Int) -> Unit,
    onImportWebProject: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        UIComponents.SectionHeader("Web 插件配置")

        UIComponents.Card(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    text = "Web 插件说明",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                val hints = listOf(
                    "Web 插件不需要编写 Java 代码",
                    "编辑 web/index.html、web/style.css、web/script.js",
                    "JavaScript 可通过 UINPlugin.callHost() 调用原生功能",
                    "修改 HTML/CSS/JS 后无需重新编译",
                    "可直接导入已有的 HTML/CSS/JS 项目"
                )
                hints.forEach { hint ->
                    Row(verticalAlignment = Alignment.Top) {
                        UIComponents.ConnectorMark(
                            size = 10.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        )
                        Spacer(Modifier.width(Spacing.xs))
                        Text(
                            text = hint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Text(
            text = "选择模板类型",
            style = MaterialTheme.typography.labelLarge
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            listOf("完整模板", "空白模板", "导入项目", "跳过").forEachIndexed { i, label ->
                UIComponents.Chip(
                    label = label,
                    selected = templateType == i,
                    onClick = { onTemplateTypeChange(i) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (templateType == 2) {
            UIComponents.PrimaryButton(
                text = "选择 ZIP 文件导入",
                icon = Icons.Default.FileUpload,
                onClick = onImportWebProject,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ==================== 步骤4：资源文件 ====================

@Composable
fun ResourcesStep(
    resourcePaths: List<String>,
    onResourceAdded: (String) -> Unit,
    onResourceRemoved: (Int) -> Unit
) {
    val context = LocalContext.current

    val resourcePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            handleResourceSelection(context, uri, onResourceAdded)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        UIComponents.SectionHeader("资源文件")
        UIComponents.CaptionText("添加图片、音频等资源文件（可选）")

        if (resourcePaths.isEmpty()) {
            UIComponents.Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(Modifier.height(Spacing.xs))
                        UIComponents.CaptionText("暂无资源文件")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                items(resourcePaths.indices.toList()) { index ->
                    val path = resourcePaths[index]
                    UIComponents.Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UIComponents.ConnectorMark(
                                size = 10.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            )
                            Spacer(Modifier.width(Spacing.sm))
                            Text(
                                text = File(path).name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            UIComponents.IconButton(
                                icon = Icons.Default.Close,
                                onClick = { onResourceRemoved(index) },
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            )
                        }
                    }
                }
            }
        }

        UIComponents.PrimaryButton(
            text = "添加资源文件",
            icon = Icons.Default.Add,
            onClick = { resourcePickerLauncher.launch("*/*") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ==================== 步骤5：打包 ====================

@Composable
fun PackageStep(
    isCompiling: Boolean,
    compileMessage: String,
    compileProgress: Int,
    tpkFile: File?
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        UIComponents.SectionHeader("生成项目文件")

        UIComponents.Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(Spacing.md)
                ) {
                    when {
                        isCompiling -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(36.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 3.dp
                            )
                            Spacer(Modifier.height(Spacing.md))
                            UIComponents.LinearProgressIndicator(progress = compileProgress / 100f)
                            Spacer(Modifier.height(Spacing.sm))
                            Text(
                                text = compileMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$compileProgress%",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        tpkFile != null && tpkFile.exists() -> {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(Spacing.sm))
                            Text(
                                text = "打包成功",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(Spacing.xs))
                            Text(
                                text = tpkFile.absolutePath,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    fontSize = 10.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "大小: ${formatFileSize(tpkFile.length())}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        else -> {
                            Icon(
                                Icons.Default.Build,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                            Spacer(Modifier.height(Spacing.sm))
                            Text(
                                text = "点击「完成」按钮开始生成项目文件",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (compileMessage.isNotEmpty() && !compileMessage.contains("成功")) {
                                Spacer(Modifier.height(Spacing.xs))
                                Text(
                                    text = compileMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}