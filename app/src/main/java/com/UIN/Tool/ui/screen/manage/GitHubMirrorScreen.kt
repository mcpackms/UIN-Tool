package com.UIN.Tool.ui.screen.manage

import android.net.Uri
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.UIN.Tool.data.local.PreferenceManager
import com.UIN.Tool.data.remote.MirrorManager
import com.UIN.Tool.domain.model.MirrorItem
import com.UIN.Tool.log.Logger
import com.UIN.Tool.ui.components.Spacing
import com.UIN.Tool.ui.theme.Shape
import com.UIN.Tool.ui.components.UIComponents
import com.UIN.Tool.utils.Constants
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GitHubMirrorScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferenceManager = PreferenceManager(context)
    val mirrorManager = MirrorManager(OkHttpClient())

    var mirrors by remember { mutableStateOf<List<MirrorItem>>(emptyList()) }
    var enabledMirrors by remember { mutableStateOf<Set<String>>(emptySet()) }
    var useCdn by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showTestResult by remember { mutableStateOf<String?>(null) }

    fun loadMirrors() {
        val defaultMirrors = Constants.DEFAULT_MIRRORS.map { url ->
            MirrorItem(
                name = url.substringAfter("//").substringBefore("."),
                url = url,
                isDefault = true,
                remark = when {
                    url.contains("fastgit") -> "国内高速镜像"
                    url.contains("ghproxy") -> "代理加速"
                    url.contains("moeyy") -> "国内镜像"
                    else -> ""
                }
            )
        }
        mirrors = defaultMirrors
        val enabled = preferenceManager.getEnabledMirrors()
        enabledMirrors = if (enabled.isNotEmpty()) enabled.toSet() else defaultMirrors.take(3).map { it.url }.toSet()
        useCdn = preferenceManager.isUseCdn()
    }

    fun importMirrors(uri: Uri) {
        scope.launch {
            try {
                isLoading = true
                val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
                if (content.isNullOrEmpty()) {
                    showTestResult = "文件为空"
                    return@launch
                }
                var count = 0
                content.lines().forEach { line ->
                    val trimmed = line.trim()
                    if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                        mirrorManager.parseMirrorFromString(trimmed)?.let {
                            mirrors = mirrors + it
                            count++
                        }
                    }
                }
                showTestResult = "成功导入 $count 个镜像站"
            } catch (e: Exception) {
                showTestResult = "导入失败: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun exportMirrors(uri: Uri) {
        try {
            val content = StringBuilder()
            content.append("# GitHub 镜像站列表\n# 格式: 名称|URL|备注\n\n")
            mirrors.forEach {
                content.append("${it.name}|${it.url}")
                if (it.remark.isNotEmpty()) content.append("|${it.remark}")
                content.append("\n")
            }
            context.contentResolver.openOutputStream(uri)?.write(content.toString().toByteArray())
            showTestResult = "导出成功"
        } catch (e: Exception) {
            showTestResult = "导出失败: ${e.message}"
        }
    }

    fun testAllMirrors() {
        scope.launch {
            try {
                isLoading = true
                showTestResult = "正在测试镜像站…"
                val tested = mirrorManager.testMirrors(mirrors)
                mirrors = tested
                val reachableCount = tested.count { it.reachable == true }
                showTestResult = "测试完成，$reachableCount/${tested.size} 个可达"
            } catch (e: Exception) {
                showTestResult = "测试失败: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { importMirrors(it) } }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument()
    ) { uri: Uri? -> uri?.let { exportMirrors(it) } }

    LaunchedEffect(Unit) { loadMirrors() }

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
                        Text("GitHub 加速", style = MaterialTheme.typography.titleMedium)
                    }
                },
                navigationIcon = {
                    UIComponents.IconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        onClick = { navController.navigateUp() },
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                },
                actions = {
                    UIComponents.IconButton(
                        icon = Icons.Default.Refresh,
                        onClick = { loadMirrors() },
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
            // CDN 开关
            UIComponents.Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { useCdn = !useCdn }
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CDN 加速",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "使用 CDN 代理加速下载",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    UIComponents.ToggleSwitch(
                        checked = useCdn,
                        onCheckedChange = { useCdn = it }
                    )
                }
            }

            Spacer(Modifier.height(Spacing.md))

            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                UIComponents.SecondaryButton(
                    text = "添加",
                    icon = Icons.Default.Add,
                    onClick = { showAddDialog = true },
                    modifier = Modifier.weight(1f)
                )
                UIComponents.SecondaryButton(
                    text = "导入",
                    icon = Icons.Default.FileUpload,
                    onClick = { importLauncher.launch("text/plain") },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                )
            }

            Spacer(Modifier.height(Spacing.xs))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                UIComponents.SecondaryButton(
                    text = "导出",
                    icon = Icons.Default.FileDownload,
                    onClick = { exportLauncher.launch("mirrors_${System.currentTimeMillis()}.txt") },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                )
                UIComponents.GhostButton(
                    text = "测试",
                    icon = Icons.Default.Check,
                    onClick = { testAllMirrors() },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                )
            }

            UIComponents.GhostButton(
                text = "重置为默认",
                icon = Icons.Default.Refresh,
                onClick = { showResetDialog = true },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(Spacing.md))

            // 测试结果
            showTestResult?.let { result ->
                UIComponents.Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = result,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(Spacing.md)
                    )
                }
                Spacer(Modifier.height(Spacing.sm))
            }

            // 镜像列表
            if (mirrors.isEmpty()) {
                UIComponents.EmptyState(
                    title = "暂无镜像站",
                    icon = Icons.Default.Cloud
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    items(mirrors) { mirror ->
                        UIComponents.Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    enabledMirrors = if (enabledMirrors.contains(mirror.url)) {
                                        enabledMirrors - mirror.url
                                    } else {
                                        enabledMirrors + mirror.url
                                    }
                                }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                UIComponents.ConnectorMark(
                                    size = 10.dp,
                                    color = if (enabledMirrors.contains(mirror.url))
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                )
                                Spacer(Modifier.width(Spacing.sm))

                                Checkbox(
                                    checked = enabledMirrors.contains(mirror.url),
                                    onCheckedChange = {
                                        enabledMirrors = if (enabledMirrors.contains(mirror.url)) {
                                            enabledMirrors - mirror.url
                                        } else {
                                            enabledMirrors + mirror.url
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = MaterialTheme.colorScheme.primary,
                                        uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = mirror.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                                            if (mirror.isDefault) {
                                                Surface(
                                                    color = MaterialTheme.colorScheme.primaryContainer,
                                                    shape = Shape.ChipShape
                                                ) {
                                                    Text(
                                                        "默认",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                            mirror.reachable?.let {
                                                Surface(
                                                    color = if (it)
                                                        MaterialTheme.colorScheme.primaryContainer
                                                    else
                                                        MaterialTheme.colorScheme.errorContainer,
                                                    shape = Shape.ChipShape
                                                ) {
                                                    Text(
                                                        if (it) "可达" else "不可达",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (it)
                                                            MaterialTheme.colorScheme.onPrimaryContainer
                                                        else
                                                            MaterialTheme.colorScheme.onErrorContainer,
                                                        modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Text(
                                        text = mirror.url,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                    if (mirror.remark.isNotEmpty()) {
                                        Text(
                                            text = mirror.remark,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (!mirror.isDefault) {
                                    UIComponents.IconButton(
                                        icon = Icons.Default.Close,
                                        onClick = {
                                            mirrors = mirrors.filter { it.url != mirror.url }
                                            enabledMirrors = enabledMirrors - mirror.url
                                        },
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(Spacing.md))

            UIComponents.PrimaryButton(
                text = "保存设置",
                onClick = {
                    preferenceManager.setEnabledMirrors(enabledMirrors.toList())
                    preferenceManager.setUseCdn(useCdn)
                    navController.navigateUp()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )
        }
    }

    if (showResetDialog) {
        UIComponents.ConfirmDialog(
            title = "确认重置",
            message = "确定要重置为默认镜像站列表吗？",
            onConfirm = { loadMirrors(); showResetDialog = false },
            onDismiss = { showResetDialog = false }
        )
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var url by remember { mutableStateOf("") }
        var remark by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("添加镜像站") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    UIComponents.TextInput(
                        value = name,
                        onValueChange = { name = it },
                        label = "名称",
                        placeholder = "如: FastGit",
                        modifier = Modifier.fillMaxWidth()
                    )
                    UIComponents.TextInput(
                        value = url,
                        onValueChange = { url = it },
                        label = "URL",
                        placeholder = "https://example.com",
                        modifier = Modifier.fillMaxWidth()
                    )
                    UIComponents.TextInput(
                        value = remark,
                        onValueChange = { remark = it },
                        label = "备注（可选）",
                        placeholder = "国内镜像",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                UIComponents.PrimaryButton(
                    text = "添加",
                    onClick = {
                        if (name.isNotEmpty() && url.isNotEmpty()) {
                            mirrors = mirrors + MirrorItem(
                                name = name,
                                url = if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url",
                                remark = remark,
                                isDefault = false
                            )
                            showAddDialog = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            dismissButton = {
                UIComponents.TextButton(
                    text = "取消",
                    onClick = { showAddDialog = false }
                )
            }
        )
    }
}
