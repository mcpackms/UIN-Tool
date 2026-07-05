// app/src/main/java/com/UIN/Tool/ui/screen/permission/PluginPermissionScreen.kt
package com.UIN.Tool.ui.screen.permission

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.UIN.Tool.core.di.ServiceLocator
import com.UIN.Tool.domain.model.PluginInfo
import com.UIN.Tool.plugin.PluginPermissionManager
import com.UIN.Tool.ui.components.Spacing
import com.UIN.Tool.ui.components.UIComponents
import com.UIN.Tool.ui.theme.Shape
import com.UIN.Tool.utils.PermissionUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginPermissionScreen() {
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    val pluginManager = ServiceLocator.getPluginManager()

    var plugins by remember { mutableStateOf<List<PluginInfo>>(emptyList()) }
    var selectedPluginId by remember { mutableStateOf<String?>(null) }
    var pluginPermissions by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(false) }

    fun loadPluginPermissions(pluginId: String) {
        val perms = PluginPermissionManager.getPluginDeclaredPermissions(context, pluginId)
        val status = perms.associateWith { permission ->
            PermissionUtils.hasPermission(context, permission) ||
                    PermissionUtils.hasSpecialPermission(context, permission)
        }
        pluginPermissions = status
    }

    fun refreshPermissions() {
        selectedPluginId?.let { loadPluginPermissions(it) }
    }

    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        refreshPermissions()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        refreshPermissions()
        Toast.makeText(
            context,
            if (granted) "权限已授予" else "权限被拒绝",
            Toast.LENGTH_SHORT
        ).show()
    }

    fun requestAllPermissions() {
        selectedPluginId?.let { pluginId ->
            isLoading = true
            PluginPermissionManager.requestPermissions(context, pluginId) { allGranted ->
                isLoading = false
                refreshPermissions()
                Toast.makeText(
                    context,
                    if (allGranted) "所有权限已授予" else "部分权限被拒绝",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun togglePermission(permission: String) {
        if (PermissionUtils.isSpecialPermission(permission)) {
            if (activity != null) {
                PermissionUtils.requestSpecialPermission(
                    activity,
                    permission,
                    settingsLauncher
                )
            }
            return
        }
        permissionLauncher.launch(permission)
    }

    LaunchedEffect(Unit) {
        pluginManager.refreshPlugins()
        plugins = pluginManager.plugins.value
        if (plugins.isNotEmpty()) {
            selectedPluginId = plugins.first().pluginId
            loadPluginPermissions(plugins.first().pluginId)
        }
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
                        Text("插件权限", style = MaterialTheme.typography.titleMedium)
                    }
                },
                navigationIcon = {
                    UIComponents.IconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        onClick = { activity?.finish() },
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                },
                actions = {
                    UIComponents.IconButton(
                        icon = Icons.Default.Refresh,
                        onClick = { refreshPermissions() },
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
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
            if (plugins.isEmpty()) {
                UIComponents.Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Extension,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(Spacing.sm))
                        Text(
                            text = "暂无已安装插件",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "请先在管理页面导入插件",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
                return@Scaffold
            }

            // 插件选择下拉框
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "选择插件",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                var expanded by remember { mutableStateOf(false) }
                val selectedPlugin = plugins.find { it.pluginId == selectedPluginId }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedPlugin?.name ?: "选择插件",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = Shape.InputShape,
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        plugins.forEach { plugin ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        plugin.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    selectedPluginId = plugin.pluginId
                                    loadPluginPermissions(plugin.pluginId)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // 一键授权
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                UIComponents.PrimaryButton(
                    text = "一键授权所有权限",
                    icon = Icons.Default.Check,
                    onClick = { requestAllPermissions() },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading && selectedPluginId != null,
                    loading = isLoading
                )
                UIComponents.SecondaryButton(
                    text = "刷新",
                    icon = Icons.Default.Refresh,
                    onClick = { refreshPermissions() },
                    modifier = Modifier.weight(0.5f)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            // 插件信息
            selectedPluginId?.let { pluginId ->
                val plugin = plugins.find { it.pluginId == pluginId }
                if (plugin != null) {
                    UIComponents.Card(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            UIComponents.ConnectorMark(
                                size = 10.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            )
                            Spacer(Modifier.width(Spacing.sm))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = plugin.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "ID: ${plugin.pluginId} | 版本: ${plugin.versionName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "声明权限: ${plugin.permissions.size} 个",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(Spacing.sm))
                }
            }

            // 权限列表
            when {
                selectedPluginId == null -> {
                    UIComponents.EmptyState(
                        title = "请选择插件",
                        icon = Icons.Default.Security
                    )
                }
                pluginPermissions.isEmpty() -> {
                    UIComponents.EmptyState(
                        title = "该插件没有声明权限",
                        subtitle = "插件在 plugin.json 中声明所需权限",
                        icon = Icons.Default.Security
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        items(pluginPermissions.entries.toList()) { (permission, granted) ->
                            UIComponents.Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { if (!granted) togglePermission(permission) }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    UIComponents.ConnectorMark(
                                        size = 8.dp,
                                        color = if (granted)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                    )
                                    Spacer(Modifier.width(Spacing.sm))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = PermissionUtils.getPermissionDisplayName(permission),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = if (granted) FontWeight.Medium else FontWeight.Normal,
                                            color = if (granted)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = permission,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                        if (PermissionUtils.isSpecialPermission(permission)) {
                                            Text(
                                                text = "特殊权限，需在系统设置中手动开启",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.tertiary
                                            )
                                        }
                                    }
                                    Checkbox(
                                        checked = granted,
                                        onCheckedChange = { if (!granted) togglePermission(permission) },
                                        colors = CheckboxDefaults.colors(
                                            checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                                            checkedColor = MaterialTheme.colorScheme.primary,
                                            uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        enabled = !isLoading
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}