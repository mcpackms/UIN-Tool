package com.UIN.Tool.ui.screen.permission

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.UIN.Tool.core.di.ServiceLocator
import com.UIN.Tool.domain.model.PluginInfo
import com.UIN.Tool.plugin.PluginPermissionManager
import com.UIN.Tool.ui.components.Spacing
import com.UIN.Tool.ui.components.UIComponents
import com.UIN.Tool.ui.theme.Shape
import com.UIN.Tool.ui.theme.UINToolTheme
import kotlinx.coroutines.launch

class PluginPermissionDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pluginId = intent.getStringExtra("plugin_id") ?: ""
        val pluginManager = ServiceLocator.getPluginManager()
        val plugin = pluginManager.getPluginInfo(pluginId)

        setContent {
            UINToolTheme {
                PluginPermissionDetailScreen(
                    plugin = plugin,
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginPermissionDetailScreen(
    plugin: PluginInfo?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pluginManager = ServiceLocator.getPluginManager()

    var permissions by remember { mutableStateOf(emptyMap<String, Boolean>()) }
    var isRequesting by remember { mutableStateOf(false) }
    var progressMessage by remember { mutableStateOf("") }

    fun loadPermissions() {
        if (plugin != null) permissions = pluginManager.getPluginPermissionStatus(plugin.pluginId)
    }

    LaunchedEffect(plugin) { loadPermissions() }

    fun requestAllPermissions() {
        if (plugin == null || isRequesting) return
        isRequesting = true
        progressMessage = "正在请求权限…"
        pluginManager.requestPluginPermissionsByGroups(
            plugin.pluginId,
            onProgress = { group, current, total ->
                progressMessage = "（$current/$total）$group"
            },
            onComplete = { granted ->
                isRequesting = false
                loadPermissions()
                progressMessage = if (granted) "所有权限已授予" else "部分权限被拒绝"
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        UIComponents.ConnectorMark(
                            size = 14.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(Spacing.sm))
                        Text(
                            if (plugin != null) "${plugin.name}" else "权限详情",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                },
                navigationIcon = {
                    UIComponents.IconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        onClick = onBack,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                },
                actions = {
                    if (plugin != null && permissions.isNotEmpty()) {
                        UIComponents.IconButton(
                            icon = Icons.Default.Refresh,
                            onClick = { loadPermissions() },
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(Spacing.md)
        ) {
            if (plugin == null) {
                UIComponents.EmptyState(
                    title = "插件不存在",
                    description = "未找到对应的插件信息",
                    icon = Icons.Default.Info
                )
                return@Scaffold
            }

            // 插件信息
            UIComponents.Card(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(
                        text = plugin.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "ID: ${plugin.pluginId} | 版本: ${plugin.versionName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "声明权限: ${plugin.permissions.size} 项",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(Spacing.md))

            // 权限列表
            if (permissions.isEmpty()) {
                UIComponents.EmptyState(
                    title = "该插件没有声明任何权限",
                    icon = Icons.Default.Security
                )
            } else {
                // 统计
                val grantedCount = permissions.values.count { it }
                val totalCount = permissions.size

                UIComponents.Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "权限状态",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "$grantedCount / $totalCount 已授予",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = if (grantedCount == totalCount)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.tertiary
                        )
                    }
                }

                Spacer(Modifier.height(Spacing.sm))

                // 进度消息
                if (progressMessage.isNotEmpty()) {
                    Text(
                        text = progressMessage,
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            progressMessage.contains("已授予") -> MaterialTheme.colorScheme.primary
                            progressMessage.contains("被拒绝") -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(Shape.InputShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(Spacing.sm)
                    )
                    Spacer(Modifier.height(Spacing.sm))
                }

                // 权限列表
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    items(permissions.entries.toList()) { (permission, granted) ->
                        PermissionDetailItemCard(
                            permission = permission,
                            granted = granted,
                            onRequest = {
                                if (!granted && !isRequesting) {
                                    pluginManager.requestPluginPermissions(plugin.pluginId) { _ ->
                                        loadPermissions()
                                    }
                                }
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(Spacing.md))

            // 操作按钮
            if (permissions.isNotEmpty()) {
                val allGranted = permissions.values.all { it }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    UIComponents.PrimaryButton(
                        text = if (allGranted) "所有权限已授予" else "一键授权所有",
                        onClick = { requestAllPermissions() },
                        modifier = Modifier.weight(1f),
                        enabled = !allGranted && !isRequesting,
                        loading = isRequesting
                    )
                    UIComponents.SecondaryButton(
                        text = "刷新",
                        icon = Icons.Default.Refresh,
                        onClick = { loadPermissions() },
                        modifier = Modifier.weight(0.4f),
                        enabled = !isRequesting
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionDetailItemCard(
    permission: String,
    granted: Boolean,
    onRequest: () -> Unit
) {
    UIComponents.Card(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            UIComponents.ConnectorMark(
                size = 10.dp,
                color = if (granted)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )
            Spacer(Modifier.width(Spacing.sm))

            // 权限信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = PluginPermissionManager.getPermissionDisplayName(permission),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (granted) FontWeight.Medium else FontWeight.Normal,
                    color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = PluginPermissionManager.getPermissionDescription(permission),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
                if (PluginPermissionManager.isSpecialPermission(permission)) {
                    Text(
                        text = "特殊权限：需在系统设置中手动开启",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            // 状态标签或操作按钮
            if (granted) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = Shape.ChipShape
                ) {
                    Text(
                        text = "已授予",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 2.dp)
                    )
                }
            } else {
                UIComponents.PrimaryButton(
                    text = "授权",
                    onClick = onRequest,
                    modifier = Modifier.height(32.dp)
                )
            }
        }
    }
}
