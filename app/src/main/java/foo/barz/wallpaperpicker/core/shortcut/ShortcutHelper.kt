package foo.barz.wallpaperpicker.core.shortcut

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import foo.barz.wallpaperpicker.R
import foo.barz.wallpaperpicker.ui.ShortcutActionActivity

/**
 * Utility helper for managing static and dynamic app shortcuts, as well as pinning shortcuts to the desktop.
 */
object ShortcutHelper {
    const val ACTION_NEXT_WALLPAPER = "foo.barz.wallpaperpicker.ACTION_NEXT_WALLPAPER"
    const val ACTION_VIEW_CURRENT_WALLPAPER = "foo.barz.wallpaperpicker.ACTION_VIEW_CURRENT_WALLPAPER"

    const val ID_NEXT_WALLPAPER = "next_wallpaper"
    const val ID_VIEW_CURRENT = "view_current"

    /**
     * Publishes dynamic shortcuts to ensure immediate availability and launcher compatibility.
     */
    fun updateDynamicShortcuts(context: Context) {
        runCatching {
            val nextIntent = Intent(context, ShortcutActionActivity::class.java).apply {
                action = ACTION_NEXT_WALLPAPER
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val nextShortcut = ShortcutInfoCompat.Builder(context, ID_NEXT_WALLPAPER)
                .setShortLabel(context.getString(R.string.shortcut_next_short))
                .setLongLabel(context.getString(R.string.shortcut_next_long))
                .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shortcut_next))
                .setIntent(nextIntent)
                .build()

            val viewIntent = Intent(context, ShortcutActionActivity::class.java).apply {
                action = ACTION_VIEW_CURRENT_WALLPAPER
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val viewShortcut = ShortcutInfoCompat.Builder(context, ID_VIEW_CURRENT)
                .setShortLabel(context.getString(R.string.shortcut_view_short))
                .setLongLabel(context.getString(R.string.shortcut_view_long))
                .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shortcut_view))
                .setIntent(viewIntent)
                .build()

            ShortcutManagerCompat.addDynamicShortcuts(context, listOf(nextShortcut, viewShortcut))
        }
    }

    /**
     * Checks if the user's default launcher supports pinning shortcuts to the home screen.
     */
    fun isPinShortcutSupported(context: Context): Boolean {
        return runCatching { ShortcutManagerCompat.isRequestPinShortcutSupported(context) }.getOrDefault(false)
    }

    /**
     * Requests the system launcher to pin the "Next Wallpaper" shortcut to the home screen.
     */
    fun requestPinNextWallpaperShortcut(context: Context): Boolean {
        if (!isPinShortcutSupported(context)) return false
        val nextIntent = Intent(context, ShortcutActionActivity::class.java).apply {
            action = ACTION_NEXT_WALLPAPER
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val shortcut = ShortcutInfoCompat.Builder(context, ID_NEXT_WALLPAPER)
            .setShortLabel(context.getString(R.string.shortcut_next_short))
            .setLongLabel(context.getString(R.string.shortcut_next_long))
            .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shortcut_next))
            .setIntent(nextIntent)
            .build()
        return ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
    }

    /**
     * Requests the system launcher to pin the "View Current Wallpaper" shortcut to the home screen.
     */
    fun requestPinViewCurrentShortcut(context: Context): Boolean {
        if (!isPinShortcutSupported(context)) return false
        val viewIntent = Intent(context, ShortcutActionActivity::class.java).apply {
            action = ACTION_VIEW_CURRENT_WALLPAPER
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val shortcut = ShortcutInfoCompat.Builder(context, ID_VIEW_CURRENT)
            .setShortLabel(context.getString(R.string.shortcut_view_short))
            .setLongLabel(context.getString(R.string.shortcut_view_long))
            .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shortcut_view))
            .setIntent(viewIntent)
            .build()
        return ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
    }
}
