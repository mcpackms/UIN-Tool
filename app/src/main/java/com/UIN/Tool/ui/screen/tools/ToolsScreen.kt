package com.UIN.Tool.ui.screen.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.UIN.Tool.core.di.ServiceLocator
import com.UIN.Tool.domain.model.PluginInfo
import com.UIN.Tool.plugin.PluginManager
import com.UIN.Tool.ui.components.Spacing
import com.UIN.Tool.ui.components.UIComponents
import com.UIN.Tool.ui.theme.CardShape
import com.UIN.Tool.utils.PluginShortcutHelper
import kotlinx.coroutines.launch

@Composable
fun ToolsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pluginManager = ServiceLocator.getPluginManager()
    val plugins by pluginManager.plugins.collectAsState()

    var searchText by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("全部") }
    var isGridView by remember { mutableStateOf(false) }
    var showShortcutDialog by remember { mutableStateOf<PluginInfo?>(null) }

    val categories = remember(plugins) {
        listOf("全部") + plugins.map { it.category }.distinct().sorted()
    }

    val filteredPlugins = remember(searchText, selectedCategory, plugins) {
        var result = if (searchText.isNotEmpty()) {
            pluginManager.searchPlugins(searchText)
        } else {
            plugins
        }
        if (selectedCategory != "全部") {
            result = result.filter { it.category == selectedCategory }
        }
        result
    }

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
            UIComponents.SectionHeader("工具箱")
            Row {
                UIComponents.IconButton(
                    icon = if (isSearching) Icons.Default.Close else Icons.Default.Search,
                    onClick = { isSearching = !isSearching; if (!isSearching) searchText = "" }
                )
                UIComponents.IconButton(
                    icon = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                    onClick = { isGridView = !isGridView }
                )
            }
        }

        // ======== 搜索栏 ========
        if (isSearching) {
            UIComponents.TextInput(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = "搜索插件...",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md),
                leadingIcon = Icons.Default.Search,
                singleLine = true
            )
            Spacer(Modifier.height(Spacing.sm))
        }

        // ======== 分类筛选 ========
        if (categories.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md)
                    .padding(bottom = Spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                categories.forEach { category ->
                    UIComponents.Chip(
                        label = category,
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category }
                    )
                }
            }
        }

        when {
            filteredPlugins.isEmpty() -> {
                UIComponents.EmptyState(
                    title = if (searchText.isNotEmpty()) "没有找到相关插件" else "暂无插件",
                    description = if (searchText.isNotEmpty()) "尝试其他关键词"
                                  else "在管理页面导入插件后即可使用",
                    icon = Icons.Default.Extension
                )
            }
            isGridView -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    contentPadding = PaddingValues(horizontal = Spacing.md),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredPlugins) { plugin ->
                        PluginGridCard(
                            plugin = plugin,
                            onClick = { pluginManager.openPlugin(plugin.pluginId, context) }
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                    contentPadding = PaddingValues(horizontal = Spacing.md),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredPlugins) { plugin ->
                        PluginListCard(
                            plugin = plugin,
                            onClick = { pluginManager.openPlugin(plugin.pluginId, context) },
                            onLongClick = { showShortcutDialog = plugin }
                        )
                    }
                }
            }
        }
    }

    // 快捷方式创建对话框
    showShortcutDialog?.let { plugin ->
        UIComponents.ConfirmDialog(
            title = "创建快捷方式",
            message = "是否在桌面创建「${plugin.name}」的快捷方式？",
            onConfirm = {
                scope.launch {
                    PluginShortcutHelper.createShortcut(context, plugin)
                    showShortcutDialog = null
                }
            },
            onDismiss = { showShortcutDialog = null }
        )
    }
}

// ==================== 列表卡片 ====================

@Composable
private fun PluginListCard(
    plugin: PluginInfo,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    UIComponents.Card(
        onClick = onClick
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 首字母头像 + 连接符
            UIComponents.PluginAvatar(
                name = plugin.name,
                size = 40.dp
            )

            Spacer(Modifier.width(Spacing.sm))

            // 连接符
            UIComponents.ConnectorMark(
                size = 12.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            )

            Spacer(Modifier.width(Spacing.sm))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = plugin.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(
                        text = "v${plugin.versionName}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (plugin.category.isNotEmpty()) {
                        Text(
                            text = plugin.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (plugin.isWebPlugin()) {
                        Text(
                            text = "Web",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            Spacer(Modifier.width(Spacing.sm))

            UIComponents.IconButton(
                icon = Icons.Default.Add,
                onClick = onLongClick,
                contentDescription = "创建快捷方式"
            )
        }
    }
}

// ==================== 网格卡片 ====================

@Composable
private fun PluginGridCard(
    plugin: PluginInfo,
    onClick: () -> Unit
) {
    UIComponents.Card(
        onClick = onClick
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(Spacing.sm)
        ) {
            UIComponents.PluginAvatar(
                name = plugin.name,
                size = 48.dp
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            Text(
                text = plugin.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "v${plugin.versionName}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                UIComponents.ConnectorMark(
                    size = 8.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
            }
        }
    }
}
