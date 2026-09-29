package foo.barz.wallpaperpicker.core.worker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import foo.barz.wallpaperpicker.data.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Foreground service that listens for screen-off events and triggers a delayed
 * wallpaper rotation when the screen is locked/turned off.
 */
class ScreenOffWatcherService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pendingChangeJob: Job? = null
    private var receiverRegistered = false

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    handleScreenOff()
                }
                Intent.ACTION_SCREEN_ON -> {
                    // Cancel pending rotation if user woke the device within debounce window
                    pendingChangeJob?.cancel()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        startAsForeground()
        registerScreenReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prefs = PreferencesManager(this)
        if (!shouldRunService(prefs)) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        unregisterScreenReceiver()
        pendingChangeJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun shouldRunService(prefs: PreferencesManager): Boolean {
        if (!prefs.isScheduled) return false
        return if (prefs.ruleEngineEnabled) {
            hasEnabledScreenOffRule()
        } else {
            prefs.screenOffTriggerEnabled
        }
    }

    private fun handleScreenOff() {
        val prefs = PreferencesManager(this)
        if (!shouldRunService(prefs)) return

        pendingChangeJob?.cancel()
        pendingChangeJob = serviceScope.launch {
            val matchingRule = if (prefs.ruleEngineEnabled) {
                val rule = foo.barz.wallpaperpicker.core.database.ScheduleRulesDatabase(applicationContext)
                    .getEnabledRules()
                    .firstOrNull { it.triggerType == foo.barz.wallpaperpicker.core.model.ScheduleRuleTriggerType.SCREEN_OFF }
                if (rule == null) {
                    // In rule engine mode, do not execute if no screen-off rule is active
                    return@launch
                }
                rule
            } else null

            val delaySeconds = (matchingRule?.screenOffDelaySeconds ?: prefs.screenOffDelaySeconds).coerceIn(1, 60)
            delay(delaySeconds * 1000L)

            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            // Verify screen is still turned off before applying wallpaper
            if (powerManager?.isInteractive == false) {
                WallpaperChangeExecutor.execute(
                    context = applicationContext,
                    isManualTrigger = false,
                    ruleId = matchingRule?.id,
                    eventContext = TriggerEventContext.SCREEN_OFF
                )
            }
        }
    }

    private fun hasEnabledScreenOffRule(): Boolean {
        return foo.barz.wallpaperpicker.core.database.ScheduleRulesDatabase(this)
            .getEnabledRules()
            .any { it.triggerType == foo.barz.wallpaperpicker.core.model.ScheduleRuleTriggerType.SCREEN_OFF }
    }

    private fun registerScreenReceiver() {
        if (!receiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
            }
            registerReceiver(screenReceiver, filter)
            receiverRegistered = true
        }
    }

    private fun unregisterScreenReceiver() {
        if (receiverRegistered) {
            runCatching { unregisterReceiver(screenReceiver) }
            receiverRegistered = false
        }
    }

    private fun startAsForeground() {
        val channelId = "screen_off_wallpaper_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "锁屏更换壁纸服务",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "监听锁屏状态并在熄屏后自动切换壁纸"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("锁屏更换壁纸守护中")
            .setContentText("熄屏后将自动延迟更换新壁纸")
            .setSmallIcon(android.R.drawable.sym_def_app_icon)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 3001

        fun start(context: Context) {
            val intent = Intent(context, ScreenOffWatcherService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, ScreenOffWatcherService::class.java)
            context.stopService(intent)
        }

        fun syncWithPreferences(context: Context) {
            val prefs = PreferencesManager(context)
            val shouldRun = if (!prefs.isScheduled) {
                false
            } else if (prefs.ruleEngineEnabled) {
                foo.barz.wallpaperpicker.core.database.ScheduleRulesDatabase(context)
                    .getEnabledRules()
                    .any { it.triggerType == foo.barz.wallpaperpicker.core.model.ScheduleRuleTriggerType.SCREEN_OFF }
            } else {
                prefs.screenOffTriggerEnabled
            }

            if (shouldRun) {
                start(context)
            } else {
                stop(context)
            }
        }
    }
}
