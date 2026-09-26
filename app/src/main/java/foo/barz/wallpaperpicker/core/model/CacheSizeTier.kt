package foo.barz.wallpaperpicker.core.model

import androidx.annotation.StringRes
import foo.barz.wallpaperpicker.R

/**
 * Discrete storage and file count tiers for transient wallpaper cache pruning.
 */
enum class CacheSizeTier(
    val displayName: String,
    @get:StringRes val displayNameRes: Int,
    val maxCount: Int,
    val maxSizeBytes: Long,
    val description: String,
    @get:StringRes val descriptionRes: Int
) {
    SMALL(
        displayName = "小容量 (20MB / 10张)",
        displayNameRes = R.string.cache_tier_small_label,
        maxCount = 10,
        maxSizeBytes = 20L * 1024L * 1024L,
        description = "适合存储紧张或低配设备，最小化磁盘占用",
        descriptionRes = R.string.cache_tier_small_desc
    ),
    STANDARD(
        displayName = "标准 (50MB / 25张)",
        displayNameRes = R.string.cache_tier_standard_label,
        maxCount = 25,
        maxSizeBytes = 50L * 1024L * 1024L,
        description = "推荐设置，兼顾离线轮播容灾与空间开销",
        descriptionRes = R.string.cache_tier_standard_desc
    ),
    LARGE(
        displayName = "大容量 (100MB / 50张)",
        displayNameRes = R.string.cache_tier_large_label,
        maxCount = 50,
        maxSizeBytes = 100L * 1024L * 1024L,
        description = "适合长期离线或多源混合囤图",
        descriptionRes = R.string.cache_tier_large_desc
    ),
    DISABLED(
        displayName = "禁用自动清理 (不限制)",
        displayNameRes = R.string.cache_tier_disabled_label,
        maxCount = Int.MAX_VALUE,
        maxSizeBytes = Long.MAX_VALUE,
        description = "不执行自动 LRU 淘汰，仅保留手动清理与系统回收",
        descriptionRes = R.string.cache_tier_disabled_desc
    );

    val isAutoPruneEnabled: Boolean
        get() = this != DISABLED
}
