package com.github.libretube.ui.base

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.github.libretube.R
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.helpers.LocaleHelper
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.helpers.ThemeHelper
import com.github.libretube.helpers.ThemeHelper.getThemeMode
import com.github.libretube.helpers.WindowHelper
import java.io.File
import java.util.Locale

/**
 * Activity that applies the LibreTube theme and the in-app language
 */
open class BaseActivity : AppCompatActivity() {
    open val isDialogActivity: Boolean = false

    val screenOrientationPref by lazy {
        val orientationPref = PreferenceHelper.getString(
            PreferenceKeys.ORIENTATION,
            resources.getString(R.string.config_default_orientation_pref)
        )
        when (orientationPref) {
            "portrait" -> ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
            "landscape" -> ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
            "auto" -> ActivityInfo.SCREEN_ORIENTATION_USER
            else -> throw IllegalArgumentException()
        }
    }

    /**
     * Whether the phone of the user has a cutout like a notch or not
     */
    var hasCutout: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // set the app theme (e.g. Material You)
        ThemeHelper.updateTheme(this)
        if (isDialogActivity) ThemeHelper.applyDialogActivityTheme(this)

        // enable auto-rotation if enabled
        requestOrientationChange()

        // wait for the window decor view to be drawn before detecting display cutouts
        window.decorView.setOnApplyWindowInsetsListener { view, insets ->
            hasCutout = WindowHelper.hasCutout(view)
            window.decorView.onApplyWindowInsets(insets)
        }
        enableEdgeToEdge()

        super.onCreate(savedInstanceState)

        // apply custom background if set
        applyCustomBackground()
    }

    private fun applyCustomBackground() {
        if (isDialogActivity) return
        val enabled = PreferenceHelper.getBoolean(PreferenceKeys.CUSTOM_BACKGROUND_ENABLED, false)
        if (!enabled) return
        val bgPath = PreferenceHelper.getString(PreferenceKeys.CUSTOM_BACKGROUND_PATH, "")
        if (bgPath.isBlank()) return
        val bgFile = File(bgPath)
        if (!bgFile.exists()) return
        try {
            val bitmap = BitmapFactory.decodeFile(bgFile.absolutePath)
            if (bitmap != null) {
                val bgDrawable = BitmapDrawable(resources, bitmap)
                val scrim = android.graphics.drawable.ColorDrawable(0x80000000.toInt())
                val layers = LayerDrawable(arrayOf(bgDrawable, scrim))
                window.decorView.background = layers
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase)

        val configuration = Configuration().apply {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                // TODO: remove this case in the future
                @Suppress("DEPRECATION")
                val locale = LocaleHelper.getAppLocale()
                Locale.setDefault(locale)
                setLocale(locale)
            }

            val uiPref = PreferenceHelper.getString(PreferenceKeys.THEME_MODE, "A")
            AppCompatDelegate.setDefaultNightMode(getThemeMode(uiPref))
        }

        applyOverrideConfiguration(configuration)
    }

    /**
     * Rotate the screen according to the app orientation preference
     */
    open fun requestOrientationChange() {
        requestedOrientation = screenOrientationPref
    }
}
