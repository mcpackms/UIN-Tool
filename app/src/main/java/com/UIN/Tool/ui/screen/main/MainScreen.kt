package com.UIN.Tool.ui.screen.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.UIN.Tool.R
import com.UIN.Tool.ui.components.Spacing
import com.UIN.Tool.ui.components.UIComponents
import com.UIN.Tool.ui.screen.dev.DevScreen
import com.UIN.Tool.ui.screen.manage.ManageScreen
import com.UIN.Tool.ui.screen.repo.RepoScreen
import com.UIN.Tool.ui.screen.tools.ToolsScreen
import com.UIN.Tool.ui.theme.ConnectorColor

private data class NavTab(
    val label: String,
    val icon: Int,  // drawable resource id
)

private val tabs = listOf(
    NavTab("开发", R.drawable.ic_developer_mode),
    NavTab("工具", R.drawable.ic_grid_view),
    NavTab("仓库", R.drawable.ic_repo),
    NavTab("管理", R.drawable.ic_settings),
)

@Composable
fun MainScreen(
    initialTab: Int = 1,
    checkUpdate: Boolean = false
) {
    var selectedTab by remember { mutableStateOf(initialTab) }

    // 处理检查更新
    LaunchedEffect(checkUpdate) {
        if (checkUpdate) {
            selectedTab = 3
        }
    }

    Scaffold(
        bottomBar = {
            WorkbenchNavBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                0 -> DevScreen()
                1 -> ToolsScreen()
                2 -> RepoScreen()
                3 -> ManageScreen(checkUpdate = checkUpdate)
            }
        }
    }
}

// ==================== 工台风格导航栏 ====================
// 紧凑型底部导轨，选中态用 ⟩ 连接符标记

@Composable
private fun WorkbenchNavBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(MaterialTheme.colorScheme.surface),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = selectedTab == index
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                                  else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "nav_color"
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onTabSelected(index) }
                        .padding(vertical = Spacing.xs),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // 选中时显示 ⟩ 标记
                    if (isSelected) {
                        Text(
                            text = "⟩",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Light,
                                fontSize = 14.sp,
                                color = contentColor
                            )
                        )
                        Spacer(Modifier.height(2.dp))
                    }

                    Icon(
                        painter = painterResource(id = tab.icon),
                        contentDescription = tab.label,
                        modifier = Modifier.size(22.dp),
                        tint = contentColor
                    )

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                            fontSize = 10.sp
                        ),
                        color = contentColor
                    )
                }
            }
        }
    }
}
