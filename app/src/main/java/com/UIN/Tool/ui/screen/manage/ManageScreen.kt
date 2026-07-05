package com.UIN.Tool.ui.screen.manage

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.UIN.Tool.core.di.ServiceLocator
import com.UIN.Tool.core.update.UpdateChecker
import com.UIN.Tool.core.update.UpdateDownloader
import com.UIN.Tool.data.local.PreferenceManager
import com.UIN.Tool.domain.model.ReleaseInfo
import com.UIN.Tool.log.Logger
import com.UIN.Tool.plugin.PluginManager
import com.UIN.Tool.ui.components.Spacing
import com.UIN.Tool.ui.components.UIComponents
import com.UIN.Tool.ui.log.LogViewerActivity
import com.UIN.Tool.ui.screen.backup.BackupManagerActivity
import com.UIN.Tool.ui.screen.docs.DocBrowserActivity
import com.UIN.Tool.ui.screen.permission.PermissionManagerActivity
import com.UIN.Tool.ui.theme.CardShape
import com.UIN.Tool.utils.formatFileSize
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

data class ManageMenuItem(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val description: String,
    val onClick: () -> Unit
)

@Composable
fun ManageScreen(
    checkUpdate: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pluginManager = ServiceLocator.getPluginManager()
    val preferenceManager = PreferenceManager(context)

    // ==================== 更新相关状态 ====================
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateMessage by remember { mutableStateOf("") }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateChecker by remember { mutableStateOf<UpdateChecker?>(null) }
    var updateDownloader by remember { mutableStateOf<UpdateDownloader?>(null) }
    var showProgressDialog by remember { mutableStateOf(false) }
    var progressMessage by remember { mutableStateOf("") }
    var showDeveloperOptions by remember { mutableStateOf(false) }
    var downloadTarget by remember { mutableStateOf<ReleaseInfo?>(null) }

    var hasCheckedUpdate by remember { mutableStateOf(false) }
    var hasNewVersion by remember { mutableStateOf(false) }
    var shouldShowUpdateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        updateChecker = UpdateChecker(context, preferenceManager)
        updateDownloader = UpdateDownloader(context)
    }

    // ==================== 检查更新 ====================
    fun performCheckUpdate(showNoUpdateToast: Boolean = false) {
        if (isCheckingUpdate) return
        isCheckingUpdate = true
        updateMessage = "正在检查更新..."

        updateChecker?.setOnUpdateListener(object : UpdateChecker.OnUpdateListener {
            override fun onCheckStart() {}

            override fun onCheckSuccess(
                releases: List<ReleaseInfo>,
                hasNewer: Boolean,
                forceUpdate: Boolean
            ) {
                isCheckingUpdate = false
                hasCheckedUpdate = true
                if (hasNewer && releases.isNotEmpty()) {
                    hasNewVersion = true
                    shouldShowUpdateDialog = true
                    val latest = releases.first()
                    updateMessage = """
                        发现新版本 v${latest.versionName}

                        大小: ${latest.getFormattedSize()}
                        发布: ${latest.getFormattedDate()}

                        ${if (forceUpdate) "⚠️ 此版本为强制更新"
                          else "选择「下载更新」开始安装"}

                        ${latest.releaseNotes?.take(200) ?: ""}
                    """.trimIndent()
                    downloadTarget = latest
                    showUpdateDialog = true
                } else {
                    hasNewVersion = false
                    shouldShowUpdateDialog = false
                    if (showNoUpdateToast) {
                        Toast.makeText(context, "当前已是最新版本", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onCheckFailed(error: String) {
                isCheckingUpdate = false
                hasCheckedUpdate = true
                if (showNoUpdateToast) {
                    Toast.makeText(context, "检查更新失败: $error", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onNoUpdate(currentVersion: String) {
                isCheckingUpdate = false
                hasCheckedUpdate = true
                hasNewVersion = false
                shouldShowUpdateDialog = false
                if (showNoUpdateToast) {
                    Toast.makeText(context, "当前已是最新版本 (v$currentVersion)", Toast.LENGTH_SHORT).show()
                }
            }
        })

        updateChecker?.checkUpdate()
    }

    LaunchedEffect(checkUpdate) {
        if (checkUpdate && !hasCheckedUpdate) {
            delay(300)
            performCheckUpdate(showNoUpdateToast = false)
        }
    }

    fun startDownload(release: ReleaseInfo) {
        showProgressDialog = true
        progressMessage = "正在下载 ${release.versionName}..."
        updateDownloader?.setOnDownloadListener(object : UpdateDownloader.OnDownloadListener {
            override fun onStart() {}
            override fun onProgress(progress: Int, downloaded: Long, total: Long) {
                progressMessage = "下载中 ${release.versionName}  $progress%  ${formatFileSize(downloaded)}/${formatFileSize(total)}"
            }
            override fun onSuccess(file: File) {
                showProgressDialog = false
                updateDownloader?.installApk(file)
            }
            override fun onFailed(error: String) {
                showProgressDialog = false
                updateMessage = "下载失败: $error"
                showUpdateDialog = true
            }
        })
        updateDownloader?.startDownload(release.downloadUrl, release.versionName)
    }

    fun openWidgetConfig() {
        try {
            Logger.i("ManageScreen", "打开小部件配置")
            val intent = Intent(context, WidgetConfigActivity::class.java)
            context.startActivity(intent)
        } catch (e: Exception) {
            Logger.e("ManageScreen", "打开小部件配置失败", e)
            Toast.makeText(context, "打开配置失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // ==================== 菜单项 ====================
    val menuItems = listOf(
        ManageMenuItem("plugin_manage", "插件管理", Icons.Default.Extension, "导入、导出、卸载插件") {
            try { context.startActivity(Intent(context, PluginManageActivity::class.java)) }
            catch (e: Exception) { Toast.makeText(context, "功能开发中", Toast.LENGTH_SHORT).show() }
        },
        ManageMenuItem("permission", "权限管理", Icons.Default.Security, "管理插件权限") {
            try { context.startActivity(Intent(context, PermissionManagerActivity::class.java)) }
            catch (e: Exception) { Toast.makeText(context, "功能开发中", Toast.LENGTH_SHORT).show() }
        },
        ManageMenuItem("docs", "文档中心", Icons.Default.Info, "使用帮助和开发文档") {
            try { context.startActivity(Intent(context, DocBrowserActivity::class.java)) }
            catch (e: Exception) { Toast.makeText(context, "功能开发中", Toast.LENGTH_SHORT).show() }
        },
        ManageMenuItem("logs", "运行日志", Icons.Default.BugReport, "查看、导出、清空日志") {
            context.startActivity(Intent(context, LogViewerActivity::class.java))
        },
        ManageMenuItem("backup", "备份恢复", Icons.Default.Backup, "备份恢复插件和配置") {
            try { context.startActivity(Intent(context, BackupManagerActivity::class.java)) }
            catch (e: Exception) { Toast.makeText(context, "功能开发中", Toast.LENGTH_SHORT).show() }
        },
        ManageMenuItem("ui_config", "UI 个性化", Icons.Default.Palette, "主题、颜色、圆角设置") {
            try { context.startActivity(Intent(context, UIConfigActivity::class.java)) }
            catch (e: Exception) { Toast.makeText(context, "功能开发中", Toast.LENGTH_SHORT).show() }
        },
        ManageMenuItem("widget_config", "小部件配置", Icons.Default.Widgets, "配置快捷方式和小部件") {
            try { context.startActivity(Intent(context, WidgetConfigActivity::class.java)) }
            catch (e: Exception) { Toast.makeText(context, "功能开发中", Toast.LENGTH_SHORT).show() }
        },
        ManageMenuItem("update", "检查更新", Icons.Default.Update, "检查 GitHub 最新版本") {
            performCheckUpdate(showNoUpdateToast = true)
        },
        ManageMenuItem("github_mirror", "GitHub 加速", Icons.Default.Settings, "配置镜像站加速下载") {
            try { context.startActivity(Intent(context, GitHubMirrorActivity::class.java)) }
            catch (e: Exception) { Toast.makeText(context, "功能开发中", Toast.LENGTH_SHORT).show() }
        },
        ManageMenuItem("developer", "开发者选项", Icons.Default.DeveloperMode, "签名验证、调试设置") {
            showDeveloperOptions = true
        },
    )

    // ==================== UI ====================
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(Spacing.md)
    ) {
        UIComponents.SectionHeader("管理")

        Spacer(Modifier.height(Spacing.sm))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            modifier = Modifier.fillMaxSize()
        ) {
            items(menuItems) { item ->
                ManageMenuCard(item = item)
            }
        }
    }

    // ==================== 更新对话框 ====================
    if (showUpdateDialog && shouldShowUpdateDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isCheckingUpdate) { showUpdateDialog = false; shouldShowUpdateDialog = false }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = com.UIN.Tool.ui.theme.DialogShape,
            title = {
                Text(
                    text = if (isCheckingUpdate) "检查更新" else "更新信息",
                    style = MaterialTheme.typography.headlineSmall,
                )
            },
            text = {
                Text(
                    text = updateMessage,
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                if (!isCheckingUpdate && downloadTarget != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        UIComponents.GhostButton(
                            text = "忽略",
                            onClick = { showUpdateDialog = false; shouldShowUpdateDialog = false }
                        )
                        Spacer(Modifier.width(Spacing.sm))
                        UIComponents.PrimaryButton(
                            text = "下载更新",
                            onClick = {
                                downloadTarget?.let { startDownload(it) }
                                showUpdateDialog = false; shouldShowUpdateDialog = false
                            }
                        )
                    }
                } else {
                    UIComponents.PrimaryButton(
                        text = "确定",
                        onClick = { showUpdateDialog = false; shouldShowUpdateDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        )
    }

    // ==================== 进度对话框 ====================
    if (showProgressDialog) {
        UIComponents.LoadingDialog(
            message = progressMessage,
            onCancel = {
                showProgressDialog = false
                updateDownloader?.cancelDownload()
            }
        )
    }

    // ==================== 开发者选项 ====================
    if (showDeveloperOptions) {
        var ignoreSignature by remember {
            mutableStateOf(PluginManager.isIgnoreSignatureWarning())
        }
        AlertDialog(
            onDismissRequest = { showDeveloperOptions = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = com.UIN.Tool.ui.theme.DialogShape,
            title = {
                Text("开发者选项", style = MaterialTheme.typography.headlineSmall)
            },
            text = {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = ignoreSignature,
                            onCheckedChange = { ignoreSignature = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary,
                                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Text(
                            text = "忽略签名验证（仅开发用）",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        text = "开启后可安装未签名插件，但存在安全风险",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    UIComponents.GhostButton(
                        text = "取消",
                        onClick = { showDeveloperOptions = false }
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    UIComponents.PrimaryButton(
                        text = "保存",
                        onClick = {
                            PluginManager.setIgnoreSignatureWarning(ignoreSignature)
                            Toast.makeText(context,
                                if (ignoreSignature) "已忽略签名验证" else "已启用签名验证",
                                Toast.LENGTH_SHORT).show()
                            showDeveloperOptions = false
                        }
                    )
                }
            }
        )
    }
}

// ==================== 管理菜单卡片 ====================

@Composable
private fun ManageMenuCard(
    item: ManageMenuItem,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "menu_scale"
    )

    UIComponents.Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable { item.onClick(); isPressed = true },
        shape = CardShape,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.sm),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 图标容器（极简风格）
            Box(
                modifier = Modifier.size(36.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    item.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.height(Spacing.xs))

            Text(
                text = item.title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )

            Text(
                text = item.description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 1
            )
        }
    }
}
