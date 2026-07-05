package com.UIN.Tool.ui.screen.backup

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.UIN.Tool.core.di.ServiceLocator
import com.UIN.Tool.domain.model.BackupInfo
import com.UIN.Tool.log.Logger
import com.UIN.Tool.ui.components.Spacing
import com.UIN.Tool.ui.components.UIComponents
import com.UIN.Tool.utils.Constants
import com.UIN.Tool.utils.FileUtils
import com.UIN.Tool.utils.UIConfig
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> String.format("%.2f KB", size / 1024.0)
        else -> String.format("%.2f MB", size / (1024.0 * 1024.0))
    }
}

private fun addFileToZip(
    zos: java.util.zip.ZipOutputStream,
    file: File,
    entryName: String
) {
    if (!file.exists()) return
    try {
        zos.putNextEntry(java.util.zip.ZipEntry(entryName))
        java.io.FileInputStream(file).use { fis -> fis.copyTo(zos) }
        zos.closeEntry()
    } catch (e: Exception) {
        Logger.e("Backup", "添加文件到ZIP失败: $entryName", e)
    }
}

private fun addDirToZip(
    zos: java.util.zip.ZipOutputStream,
    dir: File,
    basePath: String
) {
    if (!dir.exists()) return
    dir.listFiles()?.forEach { file ->
        if (file.name.startsWith(".")) return@forEach
        val entryName = basePath + file.name
        if (file.isDirectory()) {
            zos.putNextEntry(java.util.zip.ZipEntry("$entryName/"))
            zos.closeEntry()
            addDirToZip(zos, file, "$entryName/")
        } else {
            try {
                zos.putNextEntry(java.util.zip.ZipEntry(entryName))
                java.io.FileInputStream(file).use { fis -> fis.copyTo(zos) }
                zos.closeEntry()
            } catch (e: Exception) {
                Logger.e("Backup", "添加文件失败: $entryName", e)
            }
        }
    }
}

@Composable
fun BackupScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pluginManager = ServiceLocator.getPluginManager()

    var backups by remember { mutableStateOf<List<BackupInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var progressMessage by remember { mutableStateOf("") }
    var progressValue by remember { mutableStateOf(0f) }
    var showProgress by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<BackupInfo?>(null) }
    var restoreTarget by remember { mutableStateOf<BackupInfo?>(null) }
    var showRestoreConfirm by remember { mutableStateOf(false) }

    var includeUiConfig by remember { mutableStateOf(true) }
    var includeSettings by remember { mutableStateOf(true) }

    val backupDir = File(Constants.BACKUP_DIR)

    fun loadBackups() {
        try {
            if (!backupDir.exists()) backupDir.mkdirs()
            val files = backupDir.listFiles()
                ?.filter { it.isFile && it.name.endsWith(".zip") }
                ?.map { file ->
                    val pluginCount = try {
                        val parts = file.nameWithoutExtension.split("_")
                        parts.getOrNull(parts.size - 2)?.toIntOrNull() ?: 0
                    } catch (e: Exception) { 0 }
                    BackupInfo(
                        file = file,
                        name = file.name,
                        size = file.length(),
                        date = file.lastModified(),
                        pluginCount = pluginCount
                    )
                }
                ?.sortedByDescending { it.date }
                ?: emptyList()
            backups = files
        } catch (e: Exception) {
            Logger.e("BackupScreen", "加载备份列表失败", e)
        }
    }

    fun createBackup() {
        scope.launch {
            try {
                isLoading = true
                showProgress = true
                progressValue = 0f
                progressMessage = "正在创建备份…"

                val plugins = pluginManager.plugins.value
                val pluginDir = File(Constants.PLUGIN_DIR)
                if (!pluginDir.exists()) {
                    progressMessage = "没有插件可备份"
                    showProgress = false
                    isLoading = false
                    Toast.makeText(context, "没有插件可备份", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val backupFile = File(backupDir, "UIN_Tool_Backup_${plugins.size}_$timestamp.zip")

                java.util.zip.ZipOutputStream(java.io.FileOutputStream(backupFile)).use { zos ->
                    progressValue = 0.1f
                    progressMessage = "正在打包 ${plugins.size} 个插件…"
                    addDirToZip(zos, pluginDir, "plugins/")

                    if (includeUiConfig) {
                        progressValue = 0.4f
                        progressMessage = "正在备份 UI 配置…"
                        val uiConfigFile = File(context.filesDir, "ui_config.json")
                        if (uiConfigFile.exists()) addFileToZip(zos, uiConfigFile, "config/ui_config.json")
                    }

                    if (includeSettings) {
                        progressValue = 0.55f
                        progressMessage = "正在备份应用设置…"
                        val prefsDir = File(context.filesDir.parent, "shared_prefs")
                        if (prefsDir.exists()) {
                            prefsDir.listFiles()?.forEach { prefFile ->
                                if (prefFile.name.endsWith(".xml"))
                                    addFileToZip(zos, prefFile, "config/shared_prefs/${prefFile.name}")
                            }
                        }
                    }

                    progressValue = 0.7f
                    progressMessage = "正在备份工作目录配置…"
                    val configJson = org.json.JSONObject().apply {
                        put("work_folder", Constants.WORK_DIR)
                        put("backup_time", timestamp)
                        put("plugin_count", plugins.size)
                        put("app_version", Constants.APP_VERSION)
                        put("app_version_code", Constants.APP_VERSION_CODE)
                    }
                    val tempConfig = File(context.cacheDir, "temp_config.json")
                    tempConfig.writeText(configJson.toString(4))
                    addFileToZip(zos, tempConfig, "config/work_folder.json")
                    tempConfig.delete()

                    if (includeSettings) {
                        progressValue = 0.8f
                        progressMessage = "正在备份应用设置…"
                        val prefs = context.getSharedPreferences("uin_tool_prefs", Context.MODE_PRIVATE)
                        val settings = org.json.JSONObject().apply {
                            put("view_mode", prefs.getString("view_mode", "list"))
                            put("use_custom_icon_tint", prefs.getBoolean("use_custom_icon_tint", true))
                            put("current_theme", prefs.getString("current_theme", "default"))
                        }
                        val tempSettings = File(context.cacheDir, "temp_settings.json")
                        tempSettings.writeText(settings.toString(4))
                        addFileToZip(zos, tempSettings, "config/settings.json")
                        tempSettings.delete()
                    }

                    progressValue = 0.85f
                    progressMessage = "正在备份镜像配置…"
                    val mirrorPrefs = context.getSharedPreferences("github_mirror", Context.MODE_PRIVATE)
                    val mirrorConfig = org.json.JSONObject().apply {
                        put("enabled_mirrors", mirrorPrefs.getString("enabled_mirrors", ""))
                        put("use_cdn", mirrorPrefs.getBoolean("use_cdn", true))
                    }
                    val tempMirror = File(context.cacheDir, "temp_mirror.json")
                    tempMirror.writeText(mirrorConfig.toString(4))
                    addFileToZip(zos, tempMirror, "config/github_mirror.json")
                    tempMirror.delete()
                }

                progressValue = 1f
                progressMessage = "完成！"

                loadBackups()
                Toast.makeText(context, "备份成功（${formatFileSize(backupFile.length())}）", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Logger.e("Backup", "创建备份异常", e)
                progressMessage = "备份失败: ${e.message}"
                Toast.makeText(context, "备份失败: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                showProgress = false
                isLoading = false
                progressValue = 0f
            }
        }
    }

    fun restoreBackup(backup: BackupInfo) {
        scope.launch {
            try {
                isLoading = true
                showProgress = true
                progressValue = 0f
                progressMessage = "正在恢复备份…"

                val tempDir = File(Constants.TEMP_DIR, "restore_${System.currentTimeMillis()}")
                tempDir.mkdirs()

                progressValue = 0.1f
                progressMessage = "解压备份文件…"
                java.util.zip.ZipFile(backup.file).use { zipFile ->
                    val entries = zipFile.entries()
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        val targetFile = File(tempDir, entry.name)
                        if (entry.isDirectory) targetFile.mkdirs()
                        else {
                            targetFile.parentFile?.mkdirs()
                            zipFile.getInputStream(entry).use { input -> targetFile.outputStream().use { output -> input.copyTo(output) } }
                        }
                    }
                }

                progressValue = 0.3f
                progressMessage = "解压完成，准备恢复…"

                val pluginsBackup = File(tempDir, "plugins")
                if (pluginsBackup.exists()) {
                    progressValue = 0.4f
                    progressMessage = "正在恢复插件…"
                    val pluginDir = File(Constants.PLUGIN_DIR)
                    if (pluginDir.exists()) FileUtils.deleteRecursively(pluginDir)
                    pluginDir.mkdirs()
                    FileUtils.copyDirectory(pluginsBackup, pluginDir)
                }

                val uiConfigBackup = File(tempDir, "config/ui_config.json")
                if (uiConfigBackup.exists()) {
                    progressValue = 0.6f
                    progressMessage = "正在恢复 UI 配置…"
                    val destUiConfig = File(context.filesDir, "ui_config.json")
                    uiConfigBackup.copyTo(destUiConfig, overwrite = true)
                    // reload
                    try {
                        val uiConfig = UIConfig.getInstance()
                        val configJson = destUiConfig.readText()
                        val obj = org.json.JSONObject(configJson)
                        val theme = obj.optJSONObject("theme")
                        if (theme != null) {
                            val keys = theme.keys()
                            while (keys.hasNext()) {
                                val key = keys.next()
                                val value = theme.getString(key)
                                when (key) {
                                    "primary" -> uiConfig.updateColor("primary", value)
                                    "primary_dark" -> uiConfig.updateColor("primary_dark", value)
                                    "primary_light" -> uiConfig.updateColor("primary_light", value)
                                    "accent" -> uiConfig.updateColor("accent", value)
                                    "success" -> uiConfig.updateColor("success", value)
                                    "warning" -> uiConfig.updateColor("warning", value)
                                    "error" -> uiConfig.updateColor("error", value)
                                    "info" -> uiConfig.updateColor("info", value)
                                    "text_primary" -> uiConfig.updateColor("text_primary", value)
                                    "text_secondary" -> uiConfig.updateColor("text_secondary", value)
                                    "text_hint" -> uiConfig.updateColor("text_hint", value)
                                    "text_primary_inverse" -> uiConfig.updateColor("text_primary_inverse", value)
                                    "background" -> uiConfig.updateColor("background", value)
                                    "surface" -> uiConfig.updateColor("surface", value)
                                    "surface_variant" -> uiConfig.updateColor("surface_variant", value)
                                    "divider" -> uiConfig.updateColor("divider", value)
                                    "glass_background" -> uiConfig.updateColor("glass_background", value)
                                    "disabled" -> uiConfig.updateColor("disabled", value)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Logger.e("Backup", "恢复UI配置失败", e)
                    }
                }

                val prefsBackupDir = File(tempDir, "config/shared_prefs")
                if (prefsBackupDir.exists()) {
                    progressValue = 0.7f
                    progressMessage = "正在恢复应用设置…"
                    val destPrefsDir = File(context.filesDir.parent, "shared_prefs")
                    destPrefsDir.mkdirs()
                    prefsBackupDir.listFiles()?.forEach { prefFile ->
                        prefFile.copyTo(File(destPrefsDir, prefFile.name), overwrite = true)
                    }
                }

                File(tempDir, "config/work_folder.json").takeIf { it.exists() }?.let { f ->
                    progressValue = 0.8f
                    progressMessage = "正在恢复工作目录配置…"
                    try {
                        val config = org.json.JSONObject(f.readText())
                        val workFolder = config.optString("work_folder")
                        if (workFolder.isNotEmpty()) {
                            context.getSharedPreferences("uin_tool_prefs", Context.MODE_PRIVATE)
                                .edit().putString("work_folder", workFolder).apply()
                        }
                    } catch (_: Exception) { }
                }

                File(tempDir, "config/settings.json").takeIf { it.exists() }?.let { f ->
                    progressValue = 0.85f
                    progressMessage = "正在恢复应用设置…"
                    try {
                        val settings = org.json.JSONObject(f.readText())
                        context.getSharedPreferences("uin_tool_prefs", Context.MODE_PRIVATE).edit().apply {
                            putString("view_mode", settings.optString("view_mode", "list"))
                            putBoolean("use_custom_icon_tint", settings.optBoolean("use_custom_icon_tint", true))
                            putString("current_theme", settings.optString("current_theme", "default"))
                        }.apply()
                    } catch (_: Exception) { }
                }

                File(tempDir, "config/github_mirror.json").takeIf { it.exists() }?.let { f ->
                    progressValue = 0.9f
                    progressMessage = "正在恢复镜像配置…"
                    try {
                        val config = org.json.JSONObject(f.readText())
                        context.getSharedPreferences("github_mirror", Context.MODE_PRIVATE).edit().apply {
                            putString("enabled_mirrors", config.optString("enabled_mirrors", ""))
                            putBoolean("use_cdn", config.optBoolean("use_cdn", true))
                        }.apply()
                    } catch (_: Exception) { }
                }

                progressValue = 0.95f
                progressMessage = "清理临时文件…"
                FileUtils.deleteRecursively(tempDir)

                progressMessage = "刷新插件列表…"
                pluginManager.refreshPlugins()

                progressValue = 1f
                progressMessage = "恢复完成！"
                loadBackups()
                Toast.makeText(context, "恢复成功", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Logger.e("Backup", "恢复备份异常", e)
                progressMessage = "恢复失败: ${e.message}"
                Toast.makeText(context, "恢复失败: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                showProgress = false
                isLoading = false
                showRestoreConfirm = false
                progressValue = 0f
            }
        }
    }

    fun deleteBackup(backup: BackupInfo) {
        try {
            if (backup.file.delete()) {
                loadBackups()
                Toast.makeText(context, "已删除", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "删除失败", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Logger.e("BackupScreen", "删除备份失败", e)
            Toast.makeText(context, "删除失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) { loadBackups() }

    // ==================== UI ====================
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.md)
    ) {
        // 标题
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                UIComponents.ConnectorMark(
                    size = 16.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    text = "备份恢复",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            if (!isLoading) {
                UIComponents.IconButton(
                    icon = Icons.Default.Refresh,
                    onClick = { loadBackups() }
                )
            }
        }

        Spacer(Modifier.height(Spacing.sm))

        // 创建备份按钮
        UIComponents.PrimaryButton(
            text = "创建备份",
            icon = Icons.Default.Backup,
            onClick = { createBackup() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )

        Spacer(Modifier.height(Spacing.sm))

        // 备份选项
        UIComponents.Card(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    text = "⟩ 备份选项",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = includeUiConfig,
                        onCheckedChange = { includeUiConfig = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                            uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Text(
                        "包含 UI 配置",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = includeSettings,
                        onCheckedChange = { includeSettings = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                            uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Text(
                        "包含应用设置",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // 进度条
        if (showProgress) {
            Spacer(Modifier.height(Spacing.sm))
            UIComponents.Card(modifier = Modifier.fillMaxWidth()) {
                Column {
                    UIComponents.LinearProgressIndicator(progress = progressValue)
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        text = progressMessage,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(Spacing.sm))

        // 备份列表
        if (backups.isEmpty() && !showProgress) {
            UIComponents.EmptyState(
                title = "暂无备份文件",
                description = "点击「创建备份」来备份您的插件",
                icon = Icons.Default.Backup
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                items(backups) { backup ->
                    BackupItemCard(
                        backup = backup,
                        onRestore = {
                            restoreTarget = backup
                            showRestoreConfirm = true
                        },
                        onDelete = { deleteTarget = backup }
                    )
                }
            }
        }
    }

    // 对话框
    deleteTarget?.let { backup ->
        UIComponents.ConfirmDialog(
            title = "确认删除",
            message = "确定要删除备份「${backup.name}」吗？",
            onConfirm = {
                deleteBackup(backup)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null }
        )
    }

    if (showRestoreConfirm && restoreTarget != null) {
        UIComponents.ConfirmDialog(
            title = "确认恢复",
            message = "恢复操作将覆盖现有插件、配置和 UI 主题。确定要继续吗？",
            onConfirm = {
                restoreTarget?.let { restoreBackup(it) }
                restoreTarget = null
            },
            onDismiss = {
                showRestoreConfirm = false
                restoreTarget = null
            }
        )
    }
}

// ==================== 备份卡片 ====================

@Composable
fun BackupItemCard(
    backup: BackupInfo,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    UIComponents.Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            UIComponents.ConnectorMark(
                size = 12.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            )
            Spacer(Modifier.width(Spacing.sm))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = backup.name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(
                        text = backup.getFormattedDate(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "·",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Text(
                        text = backup.getFormattedSize(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (backup.pluginCount > 0) {
                        Text(
                            text = "·",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Text(
                            text = "${backup.pluginCount} 个插件",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                UIComponents.SecondaryButton(
                    text = "恢复",
                    onClick = onRestore,
                    modifier = Modifier.height(32.dp)
                )
                UIComponents.GhostButton(
                    text = "删除",
                    onClick = onDelete,
                    modifier = Modifier.height(32.dp)
                )
            }
        }
    }
}


