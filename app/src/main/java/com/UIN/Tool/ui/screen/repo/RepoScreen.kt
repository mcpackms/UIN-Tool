package com.UIN.Tool.ui.screen.repo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.UIN.Tool.core.di.ServiceLocator
import com.UIN.Tool.domain.model.RepoPluginInfo
import com.UIN.Tool.log.Logger
import com.UIN.Tool.plugin.PluginManager
import com.UIN.Tool.ui.components.Spacing
import com.UIN.Tool.ui.components.UIComponents
import com.UIN.Tool.ui.viewmodel.RepoViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun RepoScreen() {
    val context = LocalContext.current
    val repoViewModel: RepoViewModel = viewModel()
    val pluginManager = ServiceLocator.getPluginManager()

    LaunchedEffect(Unit) {
        repoViewModel.init(context)
        repoViewModel.loadPlugins()
        Logger.i("RepoScreen", "开始加载插件列表")
    }

    val uiState by repoViewModel.uiState.collectAsState()
    val filteredPlugins by repoViewModel.filteredPlugins.collectAsState()
    val installedIds by pluginManager.installedPluginIds.collectAsState()
    val downloadProgress by repoViewModel.downloadProgress.collectAsState()

    var searchText by remember { mutableStateOf("") }
    var showDetailDialog by remember { mutableStateOf<RepoPluginInfo?>(null) }
    var searchJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ======== 工具栏 ========
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            UIComponents.SectionHeader("插件仓库")
            UIComponents.IconButton(
                icon = Icons.Default.Refresh,
                onClick = { repoViewModel.refresh() }
            )
        }

        // ======== 搜索 ========
        UIComponents.TextInput(
            value = searchText,
            onValueChange = { text ->
                searchText = text
                searchJob?.cancel()
                searchJob = kotlinx.coroutines.CoroutineScope(
                    kotlinx.coroutines.Dispatchers.Main
                ).launch {
                    delay(300)
                    repoViewModel.searchPlugins(text)
                }
            },
            placeholder = "搜索插件...",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md),
            leadingIcon = Icons.Default.Search
        )

        Spacer(Modifier.height(Spacing.sm))

        // ======== 下载进度 ========
        if (downloadProgress.isDownloading) {
            UIComponents.Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "下载中: ${downloadProgress.pluginId}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (downloadProgress.total > 0) {
                            Text(
                                text = "${formatSize(downloadProgress.downloaded)} / ${formatSize(downloadProgress.total)}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = "${downloadProgress.progress}%",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(Spacing.xs))
                UIComponents.LinearProgressIndicator(
                    progress = downloadProgress.progress / 100f
                )
            }
            Spacer(Modifier.height(Spacing.sm))
        }

        // ======== 列表 ========
        when {
            uiState.isLoading && filteredPlugins.isEmpty() -> {
                UIComponents.FullScreenLoading("加载插件列表中...")
            }
            filteredPlugins.isEmpty() -> {
                UIComponents.EmptyState(
                    title = if (searchText.isNotEmpty()) "没有找到相关插件" else "暂无可用插件",
                    description = if (searchText.isNotEmpty()) "尝试其他关键词"
                                  else "请检查网络连接后刷新",
                    icon = Icons.Default.CloudOff
                )
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                    contentPadding = PaddingValues(horizontal = Spacing.md),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredPlugins) { plugin ->
                        RepoPluginCard(
                            plugin = plugin,
                            isInstalled = installedIds.contains(plugin.pluginId),
                            isDownloading = downloadProgress.isDownloading &&
                                            downloadProgress.pluginId == plugin.pluginId,
                            downloadProgress = downloadProgress.progress,
                            onInstall = { repoViewModel.downloadAndInstall(plugin) },
                            onOpen = { pluginManager.openPlugin(plugin.pluginId, context) },
                            onClick = { showDetailDialog = plugin }
                        )
                    }
                }
            }
        }
    }

    // 详情对话框
    showDetailDialog?.let { plugin ->
        AlertDialog(
            onDismissRequest = { showDetailDialog = null },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = com.UIN.Tool.ui.theme.DialogShape,
            title = {
                Text(
                    text = plugin.name,
                    style = MaterialTheme.typography.headlineSmall,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Row {
                        UIComponents.CodeText("ID: ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        UIComponents.CodeText(plugin.pluginId)
                    }
                    Row {
                        UIComponents.CodeText("版本: ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        UIComponents.CodeText(plugin.versionName)
                    }
                    Text(
                        text = "作者: ${plugin.author}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = plugin.getFormattedSize(),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "更新: ${plugin.getFormattedDate()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (plugin.description.isNotEmpty()) {
                        UIComponents.Divider()
                        Text(
                            text = plugin.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (plugin.updateLog.isNotEmpty()) {
                        UIComponents.Divider()
                        Text(
                            text = "更新日志",
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Text(
                            text = plugin.updateLog.take(300),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    UIComponents.GhostButton(
                        text = "关闭",
                        onClick = { showDetailDialog = null }
                    )
                    if (installedIds.contains(plugin.pluginId)) {
                        UIComponents.PrimaryButton(
                            text = "打开",
                            onClick = {
                                pluginManager.openPlugin(plugin.pluginId, context)
                                showDetailDialog = null
                            }
                        )
                    } else {
                        UIComponents.PrimaryButton(
                            text = "安装",
                            onClick = {
                                repoViewModel.downloadAndInstall(plugin)
                                showDetailDialog = null
                            }
                        )
                    }
                }
            }
        )
    }
}

// ==================== 仓库插件卡片 ====================

@Composable
private fun RepoPluginCard(
    plugin: RepoPluginInfo,
    isInstalled: Boolean,
    isDownloading: Boolean,
    downloadProgress: Int,
    onInstall: () -> Unit,
    onOpen: () -> Unit,
    onClick: () -> Unit
) {
    UIComponents.Card(
        onClick = onClick
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    UIComponents.ConnectorMark(
                        size = 14.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    Text(
                        text = plugin.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (isInstalled) {
                    Text(
                        text = "已安装",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                Text(
                    text = plugin.pluginId,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "v${plugin.versionName}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (plugin.description.isNotEmpty()) {
                Text(
                    text = plugin.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${plugin.author} · ${plugin.getFormattedDate()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    text = plugin.getFormattedSize(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                when {
                    isDownloading -> {
                        UIComponents.PrimaryButton(
                            text = "${downloadProgress}%",
                            onClick = {},
                            modifier = Modifier.height(36.dp).widthIn(min = 80.dp),
                            loading = true
                        )
                    }
                    isInstalled -> {
                        UIComponents.SecondaryButton(
                            text = "打开",
                            onClick = onOpen,
                            modifier = Modifier.height(36.dp)
                        )
                    }
                    else -> {
                        UIComponents.PrimaryButton(
                            text = "安装",
                            onClick = onInstall,
                            modifier = Modifier.height(36.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> String.format("%.2f KB", size / 1024.0)
        else -> String.format("%.2f MB", size / (1024.0 * 1024.0))
    }
}
