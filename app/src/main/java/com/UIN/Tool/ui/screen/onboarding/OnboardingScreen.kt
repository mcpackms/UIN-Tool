package com.UIN.Tool.ui.screen.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.UIN.Tool.ui.components.Spacing
import com.UIN.Tool.ui.components.UIComponents
import com.UIN.Tool.ui.theme.ConnectorColor
import com.UIN.Tool.ui.theme.WorkbenchGreen
import kotlinx.coroutines.launch

data class OnboardingItem(
    val title: String,
    val description: String,
    val tag: String,  // 简短标签，显示在连接符旁
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onNavigateToMain: () -> Unit,
    isVersionUpdate: Boolean = false,
    versionName: String? = null
) {
    val scope = rememberCoroutineScope()

    val items = if (isVersionUpdate) {
        listOf(
            OnboardingItem(
                "已更新到 v${versionName ?: "4.0.0"}",
                "新功能已就绪，来看看有什么变化",
                "更新"
            ),
            OnboardingItem(
                "插件管理",
                "导入、导出、分类管理你的插件集合",
                "管理"
            ),
            OnboardingItem(
                "插件开发",
                "可视化创建向导，内置代码编辑器，支持原生和 Web",
                "开发"
            ),
            OnboardingItem(
                "现在开始",
                "探索 UIN Tool 的新功能",
                "开始"
            ),
        )
    } else {
        listOf(
            OnboardingItem(
                "欢迎使用 UIN Tool",
                "一个强大的 Android 插件框架\n运行原生 Java 或 Web 插件，轻松扩展功能",
                "介绍"
            ),
            OnboardingItem(
                "插件管理",
                "一键导入导出，分类管理，\n备份恢复所有数据",
                "管理"
            ),
            OnboardingItem(
                "插件开发",
                "可视化创建向导，内置代码编辑器，\n打包为 TPK 文件",
                "开发"
            ),
            OnboardingItem(
                "Web 插件",
                "用 HTML/CSS/JS 开发，\n无需编译，即改即用",
                "Web"
            ),
            OnboardingItem(
                "一切就绪",
                "现在开始探索 UIN Tool 的功能",
                "开始"
            ),
        )
    }

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { items.size }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 顶部跳过按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            horizontalArrangement = Arrangement.End
        ) {
            if (pagerState.currentPage < items.size - 1) {
                Text(
                    text = "跳过",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clickable { onNavigateToMain() }
                        .padding(Spacing.sm)
                )
            }
        }

        // 主内容
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            val item = items[page]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = Spacing.xxl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 连接符标记 + 标签
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⟩",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Light,
                            color = ConnectorColor
                        )
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    Text(
                        text = item.tag,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.xl))

                // 标题
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(Spacing.md))

                // 描述
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
            }
        }

        // 底部：指示器 + 按钮
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 点状指示器（用连接符替代圆点）
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                repeat(items.size) { index ->
                    Text(
                        text = if (index == pagerState.currentPage) "⟩" else "·",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = if (index == pagerState.currentPage) FontWeight.Medium
                                          else FontWeight.Normal,
                            color = if (index == pagerState.currentPage)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            UIComponents.PrimaryButton(
                text = if (pagerState.currentPage == items.size - 1) "开始使用" else "下一步",
                onClick = {
                    if (pagerState.currentPage == items.size - 1) {
                        onNavigateToMain()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(44.dp)
            )
        }
    }
}
