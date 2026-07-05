package com.UIN.Tool.ui.screen.log

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.UIN.Tool.log.Logger
import com.UIN.Tool.ui.components.Spacing
import com.UIN.Tool.ui.components.UIComponents
import com.UIN.Tool.utils.Constants
import com.UIN.Tool.utils.CrashLogUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogViewerScreen(
    navController: NavController = rememberNavController()
) {
    val context = LocalContext.current
    var logLines by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showClearAllConfirm by remember { mutableStateOf(false) }
    var showCrashMessage by remember { mutableStateOf(CrashLogUtils.shouldNavigateToLogs(context)) }

    fun loadLogs() {
        isLoading = true
        try {
            val logFile = File(
                Constants.LOG_DIR,
                "uin_tool_${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}.log"
            )
            logLines = if (logFile.exists()) logFile.readLines().reversed() else emptyList()

            val crashLogFile = File(
                Constants.LOG_DIR,
                "crash_${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}.log"
            )
            if (crashLogFile.exists()) {
                val crashLines = crashLogFile.readLines().reversed()
                if (crashLines.isNotEmpty()) {
                    logLines = (crashLines + listOf("--- 分隔线 ---") + logLines)
                }
            }
        } catch (e: Exception) {
            Logger.e("LogViewer", "加载日志失败", e)
            logLines = listOf("加载日志失败: ${e.message}")
        }
        isLoading = false
    }

    LaunchedEffect(Unit) {
        if (showCrashMessage) {
            CrashLogUtils.clearNavigateFlag(context)
        }
        loadLogs()
    }

    fun exportLogs(uri: Uri) {
        try {
            val logFile = File(
                Constants.LOG_DIR,
                "uin_tool_${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}.log"
            )
            if (!logFile.exists()) {
                android.widget.Toast.makeText(context, "没有日志可导出", android.widget.Toast.LENGTH_SHORT).show()
                return
            }
            context.contentResolver.openOutputStream(uri)?.use { output ->
                logFile.inputStream().use { input -> input.copyTo(output) }
            }
            android.widget.Toast.makeText(context, "日志已导出", android.widget.Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "导出失败: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun clearAllLogs() {
        val logDir = File(Constants.LOG_DIR)
        var count = 0
        logDir.listFiles()?.forEach {
            if (it.isFile && it.name.endsWith(".log") && it.delete()) count++
        }
        loadLogs()
        android.widget.Toast.makeText(context, "已删除 $count 个日志文件", android.widget.Toast.LENGTH_SHORT).show()
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? -> if (uri != null) exportLogs(uri) }

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
                        Text("运行日志", style = MaterialTheme.typography.titleMedium)
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
                        onClick = { loadLogs() },
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    UIComponents.IconButton(
                        icon = Icons.Default.DeleteSweep,
                        onClick = { showClearAllConfirm = true },
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    UIComponents.IconButton(
                        icon = Icons.Default.FileDownload,
                        onClick = {
                            val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                            exportLauncher.launch("UIN_Tool_Log_$ts.txt")
                        },
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
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 崩溃提示
            if (showCrashMessage) {
                UIComponents.Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md, vertical = Spacing.xs)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "应用发生异常",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "请查看下方的崩溃日志了解详情",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                            )
                        }
                        UIComponents.IconButton(
                            icon = Icons.Default.Close,
                            onClick = { showCrashMessage = false },
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // 统计
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "共 ${logLines.size} 行",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "最新在上方",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            when {
                isLoading -> UIComponents.FullScreenLoading()
                logLines.isEmpty() -> {
                    UIComponents.EmptyState(
                        title = "暂无日志记录",
                        icon = Icons.Default.Info
                    )
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        horizontal = Spacing.md,
                        vertical = Spacing.xs
                    ),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    items(logLines) { line ->
                        val color = when {
                            line.contains("[ERROR]") || line.contains("[E]") ->
                                MaterialTheme.colorScheme.error
                            line.contains("[WARN]") || line.contains("[W]") ->
                                MaterialTheme.colorScheme.tertiary
                            line.contains("[SUCCESS]") || line.contains("[✓]") ->
                                MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                        val isHeader = line.contains("==================")

                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = if (isHeader) 11.5.sp else 10.5.sp,
                                fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = color.copy(alpha = if (isHeader) 1f else 0.85f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isHeader) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    else Color.Transparent
                                )
                                .padding(
                                    horizontal = Spacing.sm,
                                    vertical = if (isHeader) Spacing.sm else 2.dp
                                )
                        )
                    }
                }
            }
        }
    }

    if (showClearAllConfirm) {
        UIComponents.ConfirmDialog(
            title = "确认清空",
            message = "确定要清空所有历史日志文件吗？",
            onConfirm = { clearAllLogs(); showClearAllConfirm = false },
            onDismiss = { showClearAllConfirm = false }
        )
    }
}
