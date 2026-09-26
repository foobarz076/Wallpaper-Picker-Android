package foo.barz.wallpaperpicker.core.worker

import android.content.Context
import android.net.Uri
import android.os.PowerManager
import foo.barz.wallpaperpicker.core.applier.WallpaperApplier
import foo.barz.wallpaperpicker.core.database.WallpaperHistoryDatabase
import foo.barz.wallpaperpicker.core.processor.WallpaperProcessor
import foo.barz.wallpaperpicker.core.source.WallpaperSourceFactory
import foo.barz.wallpaperpicker.core.widget.CurrentWallpaperWidgetProvider
import foo.barz.wallpaperpicker.data.PreferencesManager
import java.io.FileNotFoundException
import java.io.IOException

/**
 * Result of a wallpaper change attempt.
 */
sealed class WallpaperExecutionResult {
    data class Success(val title: String?, val uri: Uri?) : WallpaperExecutionResult()
    data class Skipped(val reason: String) : WallpaperExecutionResult()
    data class Failure(val error: Throwable, val isTransient: Boolean) : WallpaperExecutionResult()
}

/**
 * Unified execution engine for wallpaper rotation.
 * Enforces composite triggers:
 * - Quiet Hours evaluation
 * - Cooldown suppression
 * - Interactive use deferral
 * - Deduplication (same wallpaper skipping)
 * - Per-image crop/scroll overrides
 * - Fair Shuffle history recording
 * - Widget synchronization
 */
object WallpaperChangeExecutor {

    suspend fun execute(
        context: Context,
        isManualTrigger: Boolean = false,
        ruleId: String? = null,
        eventContext: TriggerEventContext = TriggerEventContext.PERIODIC_WORKER
    ): WallpaperExecutionResult {
        val prefs = PreferencesManager(context)
        val now = System.currentTimeMillis()

        // 1. Composite Trigger: Quiet Hours check (bypassed if manual trigger)
        if (!isManualTrigger && prefs.quietHoursEnabled) {
            if (CompositeTriggerHelper.isInQuietHours(
                    now,
                    prefs.quietHoursStartHour,
                    prefs.quietHoursStartMinute,
                    prefs.quietHoursEndHour,
                    prefs.quietHoursEndMinute
                )
            ) {
                val reason = "夜间免打扰时段，已跳过更换"
                prefs.lastExecutionStatus = reason
                prefs.lastExecutionTimestamp = now
                return WallpaperExecutionResult.Skipped(reason)
            }
        }

        // 2. Composite Trigger: Cooldown Suppression check (bypassed if manual trigger)
        if (!isManualTrigger && prefs.cooldownSuppressionEnabled) {
            if (CompositeTriggerHelper.isCooldownSuppressed(
                    now,
                    prefs.lastChangedTimestamp,
                    prefs.cooldownMinutes
                )
            ) {
                val reason = "距离上次更换过近 (冷却抑制中)，已跳过本次更换"
                prefs.lastExecutionStatus = reason
                prefs.lastExecutionTimestamp = now
                return WallpaperExecutionResult.Skipped(reason)
            }
        }

        // 3. Defer during interactive use (bypassed if manual trigger)
        if (!isManualTrigger && prefs.deferDuringInteraction) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (powerManager?.isInteractive == true) {
                val reason = "设备正在使用中，已推迟更换"
                prefs.lastExecutionStatus = reason
                prefs.lastExecutionTimestamp = now
                return WallpaperExecutionResult.Skipped(reason)
            }
        }

        // 4. Resolve active schedule rule (Phase 4.2 Schedule Rule Engine)
        val matchingRule = if (!isManualTrigger && prefs.ruleEngineEnabled) {
            ScheduleRuleEngine.findMatchingRule(context, now, eventContext, ruleId)
        } else null

        // 5. Create active source (bound to rule or global)
        val source = runCatching {
            if (matchingRule != null) {
                ScheduleRuleEngine.resolveSourceForRule(
                    context = context,
                    rule = matchingRule,
                    prefs = prefs,
                    bypassNetworkConstraints = isManualTrigger
                )
            } else {
                WallpaperSourceFactory.createActiveSource(
                    context = context,
                    prefs = prefs,
                    bypassNetworkConstraints = isManualTrigger
                )
            }
        }.getOrElse { error ->
            return recordFailure(prefs, error)
        }

        // 6. Fetch next wallpaper
        val sourceResult = source.getNextWallpaper()
        if (sourceResult.isFailure) {
            return recordFailure(
                prefs,
                sourceResult.exceptionOrNull() ?: Exception("Failed to fetch wallpaper")
            )
        }

        val wallpaperData = sourceResult.getOrThrow()

        // 6. Check if same wallpaper
        val isSameWallpaper = wallpaperData.sourceUri != null &&
                wallpaperData.sourceUri == prefs.lastWallpaperUri &&
                wallpaperData.title == prefs.lastWallpaperTitle

        if (isSameWallpaper) {
            val reason = "壁纸无变化，已跳过重复应用"
            prefs.lastExecutionStatus = reason
            prefs.lastErrorMessage = null
            prefs.lastExecutionTimestamp = now
            return WallpaperExecutionResult.Skipped(reason)
        }

        // 7. Query per-image preference override (scroll mode, crop focus, horizontal flip)
        val historyDb = WallpaperHistoryDatabase(context)
        val customPref = wallpaperData.sourceUri?.let { uri ->
            runCatching { historyDb.getItemByUri(uri.toString()) }.getOrNull()
        }
        val effectiveScrollMode = customPref?.customScrollMode ?: prefs.scrollMode
        val effectiveCropFocusX = customPref?.cropFocusX ?: 0.5f
        val effectiveCropFocusY = customPref?.cropFocusY ?: 0.5f
        val effectiveFlipHorizontal = customPref?.flipHorizontal ?: false

        // 8. Process bitmap with sub-sampling and cropping
        val processor = WallpaperProcessor(context)
        val processResult = processor.process(
            openStream = wallpaperData.openStream,
            scrollMode = effectiveScrollMode,
            cropMode = prefs.cropMode,
            cropFocusX = effectiveCropFocusX,
            cropFocusY = effectiveCropFocusY,
            flipHorizontal = effectiveFlipHorizontal
        )
        if (processResult.isFailure) {
            return recordFailure(
                prefs,
                processResult.exceptionOrNull() ?: Exception("Failed to process wallpaper")
            )
        }

        val bitmap = processResult.getOrThrow()

        // 9. Re-check interactive deferral before applying to prevent screen stutter
        if (!isManualTrigger && prefs.deferDuringInteraction) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (powerManager?.isInteractive == true) {
                val reason = "设备正在使用中，已推迟更换"
                prefs.lastExecutionStatus = reason
                prefs.lastExecutionTimestamp = System.currentTimeMillis()
                return WallpaperExecutionResult.Skipped(reason)
            }
        }

        // 10. Apply wallpaper to system (rule-specific target if defined, otherwise global preference)
        val applier = WallpaperApplier(context)
        val effectiveTarget = matchingRule?.targetScreen ?: prefs.target
        val applyResult = applier.apply(bitmap, effectiveTarget)
        if (applyResult.isFailure) {
            return recordFailure(
                prefs,
                applyResult.exceptionOrNull() ?: Exception("Failed to apply wallpaper")
            )
        }

        val concreteSourceType = wallpaperData.sourceType ?: prefs.sourceType
        val baseSourceTitle = wallpaperData.sourceTitle
        val concreteSourceTitle = if (matchingRule != null) {
            if (baseSourceTitle.isNullOrBlank()) matchingRule.name else "${matchingRule.name} · $baseSourceTitle"
        } else {
            baseSourceTitle
        }
        val appliedTime = System.currentTimeMillis()

        prefs.lastChangedTimestamp = appliedTime
        prefs.lastExecutionTimestamp = appliedTime
        prefs.lastWallpaperTitle = wallpaperData.title
        prefs.lastWallpaperUri = wallpaperData.sourceUri
        prefs.lastWallpaperSourceType = concreteSourceType
        prefs.lastWallpaperSourceTitle = concreteSourceTitle
        prefs.lastExecutionStatus = if (matchingRule != null) "成功 (规则: ${matchingRule.name})" else "成功"
        prefs.lastErrorMessage = null

        // 11. Record wallpaper key into Fair Shuffle history
        val wallpaperKey = wallpaperData.sourceUri?.toString() ?: wallpaperData.title
        if (wallpaperKey != null) {
            prefs.recordRecentWallpaperKey(wallpaperKey)
        }

        // 12. Record into persistent history database
        wallpaperData.sourceUri?.let { uri ->
            runCatching {
                historyDb.recordAppliedWallpaper(
                    sourceUri = uri,
                    title = wallpaperData.title,
                    sourceType = concreteSourceType,
                    appliedTimestamp = appliedTime,
                    sourceTitle = concreteSourceTitle,
                    remoteUrl = wallpaperData.remoteUrl
                )
            }
        }

        // 13. Refresh home screen widgets and Quick Settings tiles
        CurrentWallpaperWidgetProvider.updateAllWidgets(context)
        foo.barz.wallpaperpicker.core.tile.NextWallpaperTileService.requestUpdate(context)

        return WallpaperExecutionResult.Success(wallpaperData.title, wallpaperData.sourceUri)
    }

    private fun recordFailure(prefs: PreferencesManager, error: Throwable): WallpaperExecutionResult.Failure {
        val errorMsg = error.localizedMessage ?: error.javaClass.simpleName
        prefs.lastErrorMessage = errorMsg
        prefs.lastExecutionStatus = "失败: $errorMsg"
        prefs.lastExecutionTimestamp = System.currentTimeMillis()

        val isTransient = error is IOException && error !is FileNotFoundException
        return WallpaperExecutionResult.Failure(error, isTransient)
    }
}
