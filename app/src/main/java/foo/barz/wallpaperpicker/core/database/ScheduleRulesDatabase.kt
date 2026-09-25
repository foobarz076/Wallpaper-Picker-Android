package foo.barz.wallpaperpicker.core.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import foo.barz.wallpaperpicker.core.model.ScheduleRule
import foo.barz.wallpaperpicker.core.model.ScheduleRuleSourceBinding
import foo.barz.wallpaperpicker.core.model.ScheduleRuleTriggerType
import foo.barz.wallpaperpicker.core.model.WallpaperTarget

/**
 * SQLite database managing persistent schedule rules for the Schedule Rule Engine (Phase 4.2).
 */
class ScheduleRulesDatabase(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID TEXT PRIMARY KEY,
                $COLUMN_NAME TEXT NOT NULL,
                $COLUMN_IS_ENABLED INTEGER NOT NULL DEFAULT 1,
                $COLUMN_TRIGGER_TYPE TEXT NOT NULL,
                $COLUMN_TARGET_TIME TEXT,
                $COLUMN_WINDOW_START_TIME TEXT,
                $COLUMN_WINDOW_END_TIME TEXT,
                $COLUMN_INTERVAL_MINUTES INTEGER DEFAULT 60,
                $COLUMN_SCREEN_OFF_DELAY_SECONDS INTEGER DEFAULT 3,
                $COLUMN_SOURCE_BINDING TEXT NOT NULL,
                $COLUMN_SPECIFIC_SOURCE_ID TEXT,
                $COLUMN_SPECIFIC_SOURCE_TITLE TEXT,
                $COLUMN_TARGET_SCREEN TEXT NOT NULL,
                $COLUMN_CREATED_TIMESTAMP INTEGER NOT NULL,
                $COLUMN_UPDATED_TIMESTAMP INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_rules_enabled ON $TABLE_NAME ($COLUMN_IS_ENABLED)")
        db.execSQL("CREATE INDEX idx_rules_created ON $TABLE_NAME ($COLUMN_CREATED_TIMESTAMP ASC)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    /**
     * Retrieves all schedule rules sorted by creation time.
     */
    fun getAllRules(): List<ScheduleRule> {
        val list = mutableListOf<ScheduleRule>()
        val db = readableDatabase
        val cursor: Cursor = db.query(
            TABLE_NAME,
            null,
            null,
            null,
            null,
            null,
            "$COLUMN_CREATED_TIMESTAMP ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(parseRule(it))
            }
        }
        return list
    }

    /**
     * Retrieves all currently enabled schedule rules.
     */
    fun getEnabledRules(): List<ScheduleRule> {
        val list = mutableListOf<ScheduleRule>()
        val db = readableDatabase
        val cursor: Cursor = db.query(
            TABLE_NAME,
            null,
            "$COLUMN_IS_ENABLED = 1",
            null,
            null,
            null,
            "$COLUMN_CREATED_TIMESTAMP ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(parseRule(it))
            }
        }
        return list
    }

    /**
     * Retrieves a schedule rule by its ID.
     */
    fun getRuleById(id: String): ScheduleRule? {
        val db = readableDatabase
        val cursor: Cursor = db.query(
            TABLE_NAME,
            null,
            "$COLUMN_ID = ?",
            arrayOf(id),
            null,
            null,
            null
        )
        cursor.use {
            if (it.moveToFirst()) {
                return parseRule(it)
            }
        }
        return null
    }

    /**
     * Inserts a new schedule rule into the database.
     */
    fun insertRule(rule: ScheduleRule) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ID, rule.id)
            put(COLUMN_NAME, rule.name)
            put(COLUMN_IS_ENABLED, if (rule.isEnabled) 1 else 0)
            put(COLUMN_TRIGGER_TYPE, rule.triggerType.name)
            put(COLUMN_TARGET_TIME, rule.targetTime)
            put(COLUMN_WINDOW_START_TIME, rule.windowStartTime)
            put(COLUMN_WINDOW_END_TIME, rule.windowEndTime)
            put(COLUMN_INTERVAL_MINUTES, rule.intervalMinutes)
            put(COLUMN_SCREEN_OFF_DELAY_SECONDS, rule.screenOffDelaySeconds)
            put(COLUMN_SOURCE_BINDING, rule.sourceBinding.name)
            put(COLUMN_SPECIFIC_SOURCE_ID, rule.specificSourceId)
            put(COLUMN_SPECIFIC_SOURCE_TITLE, rule.specificSourceTitle)
            put(COLUMN_TARGET_SCREEN, rule.targetScreen.name)
            put(COLUMN_CREATED_TIMESTAMP, rule.createdTimestamp)
            put(COLUMN_UPDATED_TIMESTAMP, rule.updatedTimestamp)
        }
        db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    /**
     * Updates an existing schedule rule.
     */
    fun updateRule(rule: ScheduleRule) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAME, rule.name)
            put(COLUMN_IS_ENABLED, if (rule.isEnabled) 1 else 0)
            put(COLUMN_TRIGGER_TYPE, rule.triggerType.name)
            put(COLUMN_TARGET_TIME, rule.targetTime)
            put(COLUMN_WINDOW_START_TIME, rule.windowStartTime)
            put(COLUMN_WINDOW_END_TIME, rule.windowEndTime)
            put(COLUMN_INTERVAL_MINUTES, rule.intervalMinutes)
            put(COLUMN_SCREEN_OFF_DELAY_SECONDS, rule.screenOffDelaySeconds)
            put(COLUMN_SOURCE_BINDING, rule.sourceBinding.name)
            put(COLUMN_SPECIFIC_SOURCE_ID, rule.specificSourceId)
            put(COLUMN_SPECIFIC_SOURCE_TITLE, rule.specificSourceTitle)
            put(COLUMN_TARGET_SCREEN, rule.targetScreen.name)
            put(COLUMN_UPDATED_TIMESTAMP, System.currentTimeMillis())
        }
        db.update(TABLE_NAME, values, "$COLUMN_ID = ?", arrayOf(rule.id))
    }

    /**
     * Updates the enabled status of a specific rule.
     */
    fun setRuleEnabled(id: String, enabled: Boolean) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_IS_ENABLED, if (enabled) 1 else 0)
            put(COLUMN_UPDATED_TIMESTAMP, System.currentTimeMillis())
        }
        db.update(TABLE_NAME, values, "$COLUMN_ID = ?", arrayOf(id))
    }

    /**
     * Deletes a schedule rule by its ID.
     */
    fun deleteRule(id: String) {
        val db = writableDatabase
        db.delete(TABLE_NAME, "$COLUMN_ID = ?", arrayOf(id))
    }

    /**
     * Populates default preset rules if database is empty.
     */
    fun populateDefaultPresetRulesIfNeeded() {
        if (getAllRules().isNotEmpty()) return

        val defaultRules = listOf(
            ScheduleRule(
                name = "每日晨间打卡",
                isEnabled = true,
                triggerType = ScheduleRuleTriggerType.DAILY_TIME,
                targetTime = "08:00",
                sourceBinding = ScheduleRuleSourceBinding.ACTIVE_DEFAULT
            ),
            ScheduleRule(
                name = "白天工作时段轮换",
                isEnabled = true,
                triggerType = ScheduleRuleTriggerType.TIME_WINDOW,
                windowStartTime = "09:00",
                windowEndTime = "18:00",
                intervalMinutes = 120L,
                sourceBinding = ScheduleRuleSourceBinding.ACTIVE_DEFAULT
            ),
            ScheduleRule(
                name = "夜间睡眠定点打卡",
                isEnabled = true,
                triggerType = ScheduleRuleTriggerType.DAILY_TIME,
                targetTime = "20:00",
                sourceBinding = ScheduleRuleSourceBinding.ACTIVE_DEFAULT
            )
        )

        for (rule in defaultRules) {
            insertRule(rule)
        }
    }

    private fun parseRule(cursor: Cursor): ScheduleRule {
        val id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ID))
        val name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME))
        val isEnabled = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_ENABLED)) == 1
        val triggerTypeStr = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TRIGGER_TYPE))
        val triggerType = runCatching { ScheduleRuleTriggerType.valueOf(triggerTypeStr) }
            .getOrDefault(ScheduleRuleTriggerType.DAILY_TIME)
        val targetTime = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TARGET_TIME)) ?: "08:00"
        val windowStartTime = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_WINDOW_START_TIME)) ?: "09:00"
        val windowEndTime = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_WINDOW_END_TIME)) ?: "18:00"
        val intervalMinutes = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_INTERVAL_MINUTES))
        val screenOffDelaySeconds = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SCREEN_OFF_DELAY_SECONDS))
        val sourceBindingStr = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SOURCE_BINDING))
        val sourceBinding = runCatching { ScheduleRuleSourceBinding.valueOf(sourceBindingStr) }
            .getOrDefault(ScheduleRuleSourceBinding.ACTIVE_DEFAULT)
        val specificSourceId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SPECIFIC_SOURCE_ID))
        val specificSourceTitle = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SPECIFIC_SOURCE_TITLE))
        val targetScreenStr = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TARGET_SCREEN))
        val targetScreen = runCatching { WallpaperTarget.valueOf(targetScreenStr) }
            .getOrDefault(WallpaperTarget.BOTH)
        val created = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_TIMESTAMP))
        val updated = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_UPDATED_TIMESTAMP))

        return ScheduleRule(
            id = id,
            name = name,
            isEnabled = isEnabled,
            triggerType = triggerType,
            targetTime = targetTime,
            windowStartTime = windowStartTime,
            windowEndTime = windowEndTime,
            intervalMinutes = intervalMinutes,
            screenOffDelaySeconds = screenOffDelaySeconds,
            sourceBinding = sourceBinding,
            specificSourceId = specificSourceId,
            specificSourceTitle = specificSourceTitle,
            targetScreen = targetScreen,
            createdTimestamp = created,
            updatedTimestamp = updated
        )
    }

    companion object {
        const val DATABASE_NAME = "schedule_rules.db"
        const val DATABASE_VERSION = 1
        const val TABLE_NAME = "schedule_rules"

        const val COLUMN_ID = "id"
        const val COLUMN_NAME = "name"
        const val COLUMN_IS_ENABLED = "is_enabled"
        const val COLUMN_TRIGGER_TYPE = "trigger_type"
        const val COLUMN_TARGET_TIME = "target_time"
        const val COLUMN_WINDOW_START_TIME = "window_start_time"
        const val COLUMN_WINDOW_END_TIME = "window_end_time"
        const val COLUMN_INTERVAL_MINUTES = "interval_minutes"
        const val COLUMN_SCREEN_OFF_DELAY_SECONDS = "screen_off_delay_seconds"
        const val COLUMN_SOURCE_BINDING = "source_binding"
        const val COLUMN_SPECIFIC_SOURCE_ID = "specific_source_id"
        const val COLUMN_SPECIFIC_SOURCE_TITLE = "specific_source_title"
        const val COLUMN_TARGET_SCREEN = "target_screen"
        const val COLUMN_CREATED_TIMESTAMP = "created_timestamp"
        const val COLUMN_UPDATED_TIMESTAMP = "updated_timestamp"
    }
}
