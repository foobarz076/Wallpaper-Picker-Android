package foo.barz.wallpaperpicker.core.model

import androidx.annotation.StringRes
import foo.barz.wallpaperpicker.R

/**
 * Strategy for lock screen wallpaper rendering when both home and lock screens are targeted.
 */
enum class LockScreenStrategy(
    val label: String,
    @get:StringRes val labelRes: Int,
    val description: String,
    @get:StringRes val descriptionRes: Int
) {
    INDEPENDENT_CENTERED(
        "独立单屏居中",
        R.string.lock_strategy_centered_label,
        "锁屏始终保持单屏居中，不随桌面生成超宽视差图，防止切断主体或解锁横移（推荐）",
        R.string.lock_strategy_centered_desc
    ),
    FOLLOW_DESKTOP(
        "与桌面完全联动",
        R.string.lock_strategy_follow_label,
        "锁屏与桌面共享视差宽图（Pixel 原生风格，主屏在最左侧时解锁无缝衔接）",
        R.string.lock_strategy_follow_desc
    )
}
