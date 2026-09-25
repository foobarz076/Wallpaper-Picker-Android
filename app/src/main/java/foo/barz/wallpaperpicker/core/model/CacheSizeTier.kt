package foo.barz.wallpaperpicker.core.model

/**
 * Discrete storage and file count tiers for transient wallpaper cache pruning.
 */
enum class CacheSizeTier(
    val displayName: String,
    val maxCount: Int,
    val maxSizeBytes: Long,
    val description: String
) {
    SMALL(
        displayName = "小容量 (20MB / 10张)",
        maxCount = 10,
        maxSizeBytes = 20L * 1024L * 1024L,
        description = "适合存储紧张或低配设备，最小化磁盘占用"
    ),
    STANDARD(
        displayName = "标准 (50MB / 25张)",
        maxCount = 25,
        maxSizeBytes = 50L * 1024L * 1024L,
        description = "推荐设置，兼顾离线轮播容灾与空间开销"
    ),
    LARGE(
        displayName = "大容量 (100MB / 50张)",
        maxCount = 50,
        maxSizeBytes = 100L * 1024L * 1024L,
        description = "适合长期离线或多源混合囤图"
    ),
    DISABLED(
        displayName = "禁用自动清理 (不限制)",
        maxCount = Int.MAX_VALUE,
        maxSizeBytes = Long.MAX_VALUE,
        description = "不执行自动 LRU 淘汰，仅保留手动清理与系统回收"
    );

    val isAutoPruneEnabled: Boolean
        get() = this != DISABLED
}
