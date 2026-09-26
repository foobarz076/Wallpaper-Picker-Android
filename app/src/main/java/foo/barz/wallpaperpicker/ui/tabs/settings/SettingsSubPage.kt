package foo.barz.wallpaperpicker.ui.tabs.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.graphics.vector.ImageVector

import androidx.annotation.StringRes
import foo.barz.wallpaperpicker.R

/**
 * Normalized sub-pages for application settings categorization.
 */
enum class SettingsSubPage(
    val title: String,
    @get:StringRes val titleRes: Int,
    val description: String,
    @get:StringRes val descriptionRes: Int,
    val icon: ImageVector
) {
    DISPLAY(
        title = "壁纸呈现与构图",
        titleRes = R.string.subpage_display_title,
        description = "目标屏幕、裁切构图、视差随动及即时生效",
        descriptionRes = R.string.subpage_display_desc,
        icon = Icons.Default.Crop
    ),
    SCHEDULING(
        title = "自动更换调度",
        titleRes = R.string.subpage_scheduling_title,
        description = "定时轮换、规则日程表、定点时刻与精细定时器",
        descriptionRes = R.string.subpage_scheduling_desc,
        icon = Icons.Default.Schedule
    ),
    SAFEGUARDS(
        title = "高级策略与防护",
        titleRes = R.string.subpage_safeguards_title,
        description = "夜间免打扰、防碰撞冷却、使用中避让与智能洗牌",
        descriptionRes = R.string.subpage_safeguards_desc,
        icon = Icons.Default.Tune
    ),
    WIDGETS(
        title = "桌面微件与快捷方式",
        titleRes = R.string.subpage_widgets_title,
        description = "桌面快捷方式、微件缩略图裁剪与控制中心磁贴",
        descriptionRes = R.string.subpage_widgets_desc,
        icon = Icons.Default.TouchApp
    ),
    STORAGE(
        title = "存储与缓存管理",
        titleRes = R.string.subpage_storage_title,
        description = "临时缓存上限、已收藏壁纸、导出备份与深度清理",
        descriptionRes = R.string.subpage_storage_desc,
        icon = Icons.Default.Storage
    ),
    ABOUT(
        title = "关于、保活与诊断",
        titleRes = R.string.subpage_about_title,
        description = "电池优化排查、系统保活指南、诊断日志与开源许可",
        descriptionRes = R.string.subpage_about_desc,
        icon = Icons.Default.Info
    )
}
