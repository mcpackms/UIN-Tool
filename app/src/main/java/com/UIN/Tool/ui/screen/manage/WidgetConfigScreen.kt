package com.UIN.Tool.ui.screen.manage

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.UIN.Tool.R
import com.UIN.Tool.core.di.ServiceLocator
import com.UIN.Tool.domain.model.PluginInfo
import com.UIN.Tool.log.Logger
import com.UIN.Tool.ui.components.Spacing
import com.UIN.Tool.ui.components.UIComponents
import com.UIN.Tool.ui.theme.Shape
import com.UIN.Tool.ui.theme.UINToolTheme
import com.UIN.Tool.widget.Widget1x1Provider
import com.UIN.Tool.widget.WidgetProvider

private const val TAG = "WidgetConfigScreen"
private const val NO_PLUGIN = ""

@OptIn(ExperimentalMaterial3Api::class)
class WidgetConfigActivity : ComponentActivity() {
    private lateinit var appWidgetManager: AppWidgetManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appWidgetManager = AppWidgetManager.getInstance(this)

        setContent {
            UINToolTheme {
                WidgetConfigScreen(
                    onBack = { finish() },
                    onAddWidget = { pinWidget() },
                    onAddShortcut = { pinShortcut() },
                    onRefresh = { refreshAllWidgets() }
                )
            }
        }
    }

    private fun pinWidget() {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                Toast.makeText(this, "Android 8.0+ 才支持此功能", Toast.LENGTH_LONG).show()
                return
            }
            val componentName = ComponentName(this, WidgetProvider::class.java)
            if (!appWidgetManager.isRequestPinAppWidgetSupported()) {
                Toast.makeText(this, "当前启动器不支持固定小部件，请长按桌面手动添加", Toast.LENGTH_LONG).show()
                return
            }
            appWidgetManager.requestPinAppWidget(componentName, null, null)
            Toast.makeText(this, "请在桌面放置小部件", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Logger.e(TAG, "添加小部件失败: ${e.message}", e)
            Toast.makeText(this, "添加失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun pinShortcut() {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                Toast.makeText(this, "Android 8.0+ 才支持此功能", Toast.LENGTH_LONG).show()
                return
            }
            val componentName = ComponentName(this, Widget1x1Provider::class.java)
            if (!appWidgetManager.isRequestPinAppWidgetSupported()) {
                Toast.makeText(this, "当前启动器不支持固定快捷方式，请长按桌面手动添加", Toast.LENGTH_LONG).show()
                return
            }
            appWidgetManager.requestPinAppWidget(componentName, null, null)
            Toast.makeText(this, "请在桌面放置快捷方式", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Logger.e(TAG, "添加快捷方式失败: ${e.message}", e)
            Toast.makeText(this, "添加失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun refreshAllWidgets() {
        try {
            WidgetProvider.forceRefreshAllWidgets(this)
            Widget1x1Provider.refresh1x1Widgets(this)
            Toast.makeText(this, "所有小部件已刷新", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "刷新失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetConfigScreen(
    onBack: () -> Unit,
    onAddWidget: () -> Unit,
    onAddShortcut: () -> Unit,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    val pluginManager = ServiceLocator.getPluginManager()

    var allPlugins by remember { mutableStateOf<List<PluginInfo>>(emptyList()) }
    var selectedWidgetPlugin1 by remember { mutableStateOf("") }
    var selectedWidgetPlugin2 by remember { mutableStateOf("") }
    var selectedWidgetPlugin3 by remember { mutableStateOf("") }
    var selectedShortcutPlugin by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        pluginManager.refreshPlugins()
        allPlugins = pluginManager.plugins.value
        val prefs = context.getSharedPreferences("widget_global_config", Context.MODE_PRIVATE)
        selectedWidgetPlugin1 = prefs.getString("global_plugin_1", "") ?: ""
        selectedWidgetPlugin2 = prefs.getString("global_plugin_2", "") ?: ""
        selectedWidgetPlugin3 = prefs.getString("global_plugin_3", "") ?: ""
        selectedShortcutPlugin = prefs.getString("shortcut_plugin", "") ?: ""
    }

    val pluginNames = listOf("-- 无 --") + allPlugins.map { it.name }

    fun getPluginIdByName(name: String): String =
        if (name == "-- 无 --" || name.isEmpty()) "" else allPlugins.find { it.name == name }?.pluginId ?: ""

    fun getPluginNameById(id: String): String =
        if (id.isEmpty()) "-- 无 --" else allPlugins.find { it.pluginId == id }?.name ?: "-- 无 --"

    fun saveConfig() {
        val prefs = context.getSharedPreferences("widget_global_config", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("global_plugin_1", selectedWidgetPlugin1)
            putString("global_plugin_2", selectedWidgetPlugin2)
            putString("global_plugin_3", selectedWidgetPlugin3)
            putString("shortcut_plugin", selectedShortcutPlugin)
        }.apply()
        WidgetProvider.forceRefreshAllWidgets(context)
        Widget1x1Provider.refresh1x1Widgets(context)
        Toast.makeText(context, "配置已保存并刷新", Toast.LENGTH_SHORT).show()
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
                        Text("小部件配置", style = MaterialTheme.typography.titleMedium)
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
                    UIComponents.IconButton(
                        icon = Icons.Default.Refresh,
                        onClick = onRefresh,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // 快捷方式配置
            item {
                UIComponents.Card(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            UIComponents.ConnectorMark(
                                size = 12.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(Spacing.sm))
                            Text(
                                text = "快捷方式配置",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            UIComponents.SecondaryButton(
                                text = "添加",
                                icon = Icons.Default.Add,
                                onClick = onAddShortcut,
                                modifier = Modifier.height(32.dp)
                            )
                        }

                        Spacer(Modifier.height(Spacing.sm))
                        UIComponents.Divider()
                        Spacer(Modifier.height(Spacing.sm))

                        Text(
                            text = "选择快捷方式绑定的插件",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(Spacing.sm))

                        PluginDropdownSelector(
                            current = selectedShortcutPlugin,
                            pluginNames = pluginNames,
                            onSelected = { selectedShortcutPlugin = getPluginIdByName(it) }
                        )

                        Spacer(Modifier.height(Spacing.sm))

                        Text(
                            text = "选择后，1×1 快捷方式将直接打开该插件",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // 3x3 小部件配置
            item {
                UIComponents.Card(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            UIComponents.ConnectorMark(
                                size = 12.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(Spacing.sm))
                            Text(
                                text = "3×3 小部件配置",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            UIComponents.SecondaryButton(
                                text = "添加",
                                icon = Icons.Default.Add,
                                onClick = onAddWidget,
                                modifier = Modifier.height(32.dp)
                            )
                        }

                        Spacer(Modifier.height(Spacing.sm))
                        UIComponents.Divider()
                        Spacer(Modifier.height(Spacing.sm))

                        Text(
                            text = "选择前 3 个位置显示的插件",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(Spacing.sm))

                        WidgetSlotSelector(
                            label = "位置 1",
                            current = selectedWidgetPlugin1,
                            pluginNames = pluginNames,
                            onSelected = { selectedWidgetPlugin1 = getPluginIdByName(it) }
                        )

                        Spacer(Modifier.height(Spacing.sm))

                        WidgetSlotSelector(
                            label = "位置 2",
                            current = selectedWidgetPlugin2,
                            pluginNames = pluginNames,
                            onSelected = { selectedWidgetPlugin2 = getPluginIdByName(it) }
                        )

                        Spacer(Modifier.height(Spacing.sm))

                        WidgetSlotSelector(
                            label = "位置 3",
                            current = selectedWidgetPlugin3,
                            pluginNames = pluginNames,
                            onSelected = { selectedWidgetPlugin3 = getPluginIdByName(it) }
                        )

                        Spacer(Modifier.height(Spacing.sm))

                        Text(
                            text = "未配置的位置将自动显示其他插件",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // 保存按钮
            item {
                UIComponents.PrimaryButton(
                    text = "保存配置",
                    icon = Icons.Default.Save,
                    onClick = { saveConfig() },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item { Spacer(Modifier.height(Spacing.md)) }
        }
    }
}

// ==================== 下拉选择器 ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginDropdownSelector(
    current: String,
    pluginNames: List<String>,
    onSelected: (String) -> Unit
) {
    val pluginManager = ServiceLocator.getPluginManager()
    var expanded by remember { mutableStateOf(false) }

    fun getPluginNameById(id: String): String =
        if (id.isEmpty()) "-- 无 --" else pluginManager.getPluginInfo(id)?.name ?: "-- 无 --"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = getPluginNameById(current),
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
            pluginNames.forEach { name ->
                DropdownMenuItem(
                    text = {
                        Text(
                            name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        onSelected(name)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetSlotSelector(
    label: String,
    current: String,
    pluginNames: List<String>,
    onSelected: (String) -> Unit
) {
    val pluginManager = ServiceLocator.getPluginManager()
    var expanded by remember { mutableStateOf(false) }

    fun getPluginNameById(id: String): String =
        if (id.isEmpty()) "-- 无 --" else pluginManager.getPluginInfo(id)?.name ?: "-- 无 --"

    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = getPluginNameById(current),
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
                pluginNames.forEach { name ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            onSelected(name)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
