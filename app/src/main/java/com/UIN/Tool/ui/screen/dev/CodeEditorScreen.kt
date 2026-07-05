package com.UIN.Tool.ui.screen.dev

import android.graphics.Typeface
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ScrollView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.UIN.Tool.log.Logger
import com.UIN.Tool.ui.components.Spacing
import com.UIN.Tool.ui.components.UIComponents
import com.UIN.Tool.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeEditorScreen(
    fileList: List<String>,
    fileContents: Map<String, String>,
    uiType: String,
    mainClass: String,
    pluginName: String,
    pluginId: String,
    onSave: (List<String>, Map<String, String>) -> Unit,
    onCancel: () -> Unit
) {
    var currentFile by remember { mutableStateOf(fileList.firstOrNull() ?: "") }
    var files by remember { mutableStateOf(fileList.toMutableList()) }
    var contents by remember { mutableStateOf(fileContents.toMutableMap()) }
    var hasChanges by remember { mutableStateOf(false) }

    var isDarkTheme by remember { mutableStateOf(true) }  // 开发工具 → 默认暗色
    var showAddFileDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf<String?>(null) }

    var sidebarWidth by remember { mutableStateOf(220.dp) }
    var isSidebarVisible by remember { mutableStateOf(true) }
    val minSidebarWidth = 0.dp
    val maxSidebarWidth = 300.dp

    var editorInstance by remember { mutableStateOf<PinchZoomEditText?>(null) }

    val currentContent = contents[currentFile] ?: ""
    var editedContent by remember { mutableStateOf(currentContent) }

    // 编辑器配色——暗色主题专用
    val editorBg = if (isDarkTheme) Color(0xFF1A1D23) else Color(0xFFF5F5F0)
    val editorText = if (isDarkTheme) Color(0xFFE2E4E8) else Color(0xFF1A1A1A)
    val sidebarBg = if (isDarkTheme) Color(0xFF121418) else MaterialTheme.colorScheme.surfaceVariant

    LaunchedEffect(currentFile) {
        editedContent = contents[currentFile] ?: ""
        hasChanges = false
        editorInstance?.setText(editedContent)
        editorInstance?.resetZoom()
    }

    fun saveCurrentFile() {
        val newContent = editorInstance?.text?.toString() ?: editedContent
        if (currentFile.isNotEmpty()) {
            contents[currentFile] = newContent
            editedContent = newContent
            hasChanges = false
            Logger.i("CodeEditor", "保存文件: $currentFile")
        }
    }

    fun addFile(fileName: String, content: String = "") {
        if (fileName.isNotEmpty() && !files.contains(fileName)) {
            files.add(fileName)
            contents[fileName] = content
            currentFile = fileName
            editedContent = content
            hasChanges = false
            Logger.i("CodeEditor", "添加文件: $fileName")
        }
    }

    fun deleteFile(fileName: String) {
        if (files.size <= 1) return
        files.remove(fileName)
        contents.remove(fileName)
        if (currentFile == fileName) {
            currentFile = files.firstOrNull() ?: ""
            editedContent = contents[currentFile] ?: ""
        }
        Logger.i("CodeEditor", "删除文件: $fileName")
    }

    fun getFileIcon(fileName: String): String = when {
        fileName.endsWith(".java") -> "J"
        fileName.endsWith(".kt") || fileName.endsWith(".kts") -> "K"
        fileName.endsWith(".xml") -> "X"
        fileName.endsWith(".html") -> "H"
        fileName.endsWith(".css") -> "C"
        fileName.endsWith(".js") -> "JS"
        fileName.endsWith(".json") -> "{"
        else -> "F"
    }

    var isDragging by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        UIComponents.ConnectorMark(
                            size = 16.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(Spacing.sm))
                        Text(
                            text = pluginName.ifEmpty { "代码编辑器" },
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (hasChanges) {
                            Spacer(Modifier.width(Spacing.sm))
                            Text(
                                text = "●",
                                style = MaterialTheme.typography.titleMedium,
                                color = WorkbenchAmber
                            )
                        }
                    }
                },
                navigationIcon = {
                    UIComponents.IconButton(
                        icon = if (isSidebarVisible) Icons.Default.MenuOpen else Icons.Default.Menu,
                        onClick = {
                            isSidebarVisible = !isSidebarVisible
                            sidebarWidth = if (isSidebarVisible) 220.dp else 0.dp
                        },
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                },
                actions = {
                    UIComponents.IconButton(
                        icon = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                        onClick = { isDarkTheme = !isDarkTheme },
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    UIComponents.IconButton(
                        icon = Icons.Default.Add,
                        onClick = { showAddFileDialog = true },
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    UIComponents.IconButton(
                        icon = Icons.Default.Save,
                        onClick = { saveCurrentFile() },
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(Spacing.xs))
                    UIComponents.PrimaryButton(
                        text = "完成",
                        onClick = {
                            saveCurrentFile()
                            onSave(files, contents)
                        },
                        modifier = Modifier.padding(end = Spacing.sm)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface,
                    titleContentColor = DarkTextPrimary,
                    navigationIconContentColor = DarkTextPrimary,
                    actionIconContentColor = DarkTextPrimary
                )
            )
        }
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(editorBg)
        ) {
            // 侧边栏
            Box(
                modifier = Modifier
                    .width(sidebarWidth)
                    .fillMaxHeight()
                    .background(sidebarBg)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // 侧边栏标题
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "文件",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "${files.size}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }

                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outline
                    )

                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(files) { file ->
                            CodeFileItem(
                                fileName = file,
                                isSelected = file == currentFile,
                                hasChanges = file == currentFile && hasChanges,
                                onClick = {
                                    saveCurrentFile()
                                    currentFile = file
                                },
                                onDelete = { showDeleteConfirm = file }
                            )
                        }
                    }
                }

                // 拖动把手
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(6.dp)
                        .draggable(
                            orientation = Orientation.Horizontal,
                            state = rememberDraggableState { delta ->
                                isDragging = true
                                val newWidth = (sidebarWidth + delta.dp)
                                    .coerceIn(minSidebarWidth, maxSidebarWidth)
                                sidebarWidth = newWidth
                                isSidebarVisible = newWidth >= 20.dp
                            },
                            onDragStopped = {
                                isDragging = false
                                if (sidebarWidth < 20.dp) {
                                    sidebarWidth = 0.dp
                                    isSidebarVisible = false
                                } else if (sidebarWidth < 100.dp) {
                                    sidebarWidth = 100.dp
                                    isSidebarVisible = true
                                }
                            }
                        )
                        .background(
                            if (isDragging) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            else Color.Transparent
                        )
                )
            }

            // 分隔线
            if (isSidebarVisible) {
                VerticalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // 代码编辑器区域
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(editorBg)
            ) {
                // 状态栏
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isDarkTheme) Color(0xFF1A1D23) else Color(0xFFF5F5F0))
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        UIComponents.ConnectorMark(
                            size = 10.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Spacer(Modifier.width(Spacing.sm))
                        Text(
                            text = currentFile.takeLast(50),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace
                            ),
                            color = if (isDarkTheme) DarkTextSecondary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        if (hasChanges) {
                            Text(
                                text = "未保存",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp
                                ),
                                color = WorkbenchAmber
                            )
                        }
                        Text(
                            text = "${editedContent.lines().size} 行",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            ),
                            color = if (isDarkTheme) DarkTextSecondary.copy(alpha = 0.6f)
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }

                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline
                )

                // 编辑器主体
                val context = LocalContext.current
                AndroidView(
                    factory = { ctx ->
                        FrameLayout(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )

                            val scrollView = ScrollView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }

                            val editor = PinchZoomEditText(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT
                                )
                                setPadding(24, 16, 24, 16)
                                setTextSize(16f)
                                setTypeface(Typeface.MONOSPACE)
                                setHorizontallyScrolling(true)
                                minLines = 30
                                applyEditorTheme(isDarkTheme)

                                addTextChangedListener(object : TextWatcher {
                                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                                    override fun afterTextChanged(s: Editable?) {
                                        val newText = s?.toString() ?: ""
                                        if (newText != editedContent) {
                                            editedContent = newText
                                            hasChanges = true
                                        }
                                    }
                                })
                            }

                            scrollView.addView(editor)
                            editorInstance = editor
                            addView(scrollView)
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { view ->
                        val editor = editorInstance
                        if (editor != null) {
                            val newContent = contents[currentFile] ?: ""
                            if (editor.text.toString() != newContent) {
                                editor.setText(newContent)
                                editedContent = newContent
                                hasChanges = false
                                editor.applyEditorTheme(isDarkTheme)
                            } else {
                                editor.applyEditorTheme(isDarkTheme)
                            }
                        }
                    }
                )
            }
        }
    }

    // 添加文件对话框
    if (showAddFileDialog) {
        var newFileName by remember { mutableStateOf("") }
        var newFileContent by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddFileDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = DialogShape,
            title = {
                Text("添加新文件", style = MaterialTheme.typography.headlineSmall)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    UIComponents.TextInput(
                        value = newFileName,
                        onValueChange = { newFileName = it },
                        label = "文件名",
                        placeholder = if (uiType == "web") "web/new.html"
                                      else "src/.../NewClass.java",
                        modifier = Modifier.fillMaxWidth()
                    )
                    UIComponents.TextInput(
                        value = newFileContent,
                        onValueChange = { newFileContent = it },
                        label = "文件内容（可选）",
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false
                    )
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    UIComponents.TextButton(
                        text = "取消",
                        onClick = { showAddFileDialog = false }
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    UIComponents.PrimaryButton(
                        text = "添加",
                        onClick = {
                            if (newFileName.isNotEmpty()) {
                                addFile(newFileName, newFileContent)
                                showAddFileDialog = false
                            }
                        }
                    )
                }
            }
        )
    }

    // 删除确认对话框
    if (showDeleteConfirm != null) {
        UIComponents.ConfirmDialog(
            title = "确认删除",
            message = "确定要删除「${showDeleteConfirm}」吗？",
            onConfirm = {
                deleteFile(showDeleteConfirm!!)
                showDeleteConfirm = null
            },
            onDismiss = { showDeleteConfirm = null }
        )
    }
}

// ==================== 文件树条目 ====================

@Composable
private fun CodeFileItem(
    fileName: String,
    isSelected: Boolean,
    hasChanges: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else Color.Transparent
            )
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 文件标记
        Text(
            text = "▶",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Light,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            ),
            modifier = Modifier.width(16.dp)
        )

        Spacer(Modifier.width(Spacing.xs))

        Text(
            text = fileName,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            ),
            color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )

        if (hasChanges) {
            Text(
                text = "●",
                style = MaterialTheme.typography.labelSmall,
                color = WorkbenchAmber
            )
            Spacer(Modifier.width(Spacing.xs))
        }

        UIComponents.IconButton(
            icon = Icons.Default.Close,
            onClick = onDelete,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        )
    }
}

// ==================== 编辑器主题应用 ====================

private fun EditText.applyEditorTheme(isDark: Boolean) {
    if (isDark) {
        setTextColor(android.graphics.Color.parseColor("#E2E4E8"))
        setBackgroundColor(android.graphics.Color.parseColor("#1A1D23"))
    } else {
        setTextColor(android.graphics.Color.parseColor("#1A1A1A"))
        setBackgroundColor(android.graphics.Color.parseColor("#F5F5F0"))
    }
}

// ==================== PinchZoomEditText ====================

class PinchZoomEditText(context: android.content.Context) : EditText(context) {

    private var scaleGestureDetector: ScaleGestureDetector
    private var initialFontSize: Float = 16f
    private var currentScaleFactor: Float = 1f

    init {
        scaleGestureDetector = ScaleGestureDetector(
            context,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    currentScaleFactor *= detector.scaleFactor
                    currentScaleFactor = currentScaleFactor.coerceIn(0.5f, 3.0f)
                    setTextSize((initialFontSize * currentScaleFactor).coerceIn(8f, 48f))
                    requestLayout()
                    return true
                }

                override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                    initialFontSize = textSize
                    return true
                }

                override fun onScaleEnd(detector: ScaleGestureDetector) {}
            }
        )
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) return super.onTouchEvent(event)
        scaleGestureDetector.onTouchEvent(event)
        if (event.pointerCount >= 2) return true
        return super.onTouchEvent(event)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(
            widthMeasureSpec,
            MeasureSpec.makeMeasureSpec(
                MeasureSpec.getSize(heightMeasureSpec) * 2,
                MeasureSpec.UNSPECIFIED
            )
        )
    }

    fun resetZoom() {
        currentScaleFactor = 1f
        setTextSize(initialFontSize)
        requestLayout()
    }

    override fun setTextSize(size: Float) {
        super.setTextSize(size)
        initialFontSize = size
        currentScaleFactor = 1f
    }
}
