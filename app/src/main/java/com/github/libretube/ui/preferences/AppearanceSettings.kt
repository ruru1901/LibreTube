package com.github.libretube.ui.preferences

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.github.libretube.R
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.ui.adapters.IconsSheetAdapter
import com.github.libretube.ui.base.BasePreferenceFragment
import com.github.libretube.ui.dialogs.NavBarOptionsDialog
import com.github.libretube.ui.dialogs.RequireRestartDialog
import com.github.libretube.ui.sheets.IconsBottomSheet
import java.io.File
import java.io.FileOutputStream

class AppearanceSettings : BasePreferenceFragment() {

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@registerForActivityResult
        try {
            val bgFile = File(requireContext().filesDir, "custom_background.png")
            requireContext().contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(bgFile).use { output ->
                    input.copyTo(output)
                }
            }
            PreferenceHelper.putString(PreferenceKeys.CUSTOM_BACKGROUND_PATH, bgFile.absolutePath)
            Toast.makeText(requireContext(), R.string.background_set, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), e.localizedMessage, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.appearance_settings, rootKey)

        val themeToggle = findPreference<ListPreference>(PreferenceKeys.THEME_MODE)
        themeToggle?.setOnPreferenceChangeListener { _, _ ->
            RequireRestartDialog().show(childFragmentManager, RequireRestartDialog::class.java.name)
            true
        }

        val pureTheme = findPreference<SwitchPreferenceCompat>(PreferenceKeys.PURE_THEME)
        pureTheme?.setOnPreferenceChangeListener { _, _ ->
            RequireRestartDialog().show(childFragmentManager, RequireRestartDialog::class.java.name)
            true
        }


        val changeIcon = findPreference<Preference>(PreferenceKeys.APP_ICON)
        val iconPref = PreferenceHelper.getString(
            PreferenceKeys.APP_ICON,
            IconsSheetAdapter.Companion.AppIcon.Default.activityAlias
        )
        IconsSheetAdapter.availableIcons.firstOrNull { it.activityAlias == iconPref }?.let {
            changeIcon?.summary = getString(it.nameResource)
        }
        changeIcon?.setOnPreferenceClickListener {
            IconsBottomSheet().show(childFragmentManager)
            true
        }

        val labelVisibilityMode = findPreference<ListPreference>(PreferenceKeys.LABEL_VISIBILITY)
        labelVisibilityMode?.setOnPreferenceChangeListener { _, _ ->
            RequireRestartDialog().show(childFragmentManager, RequireRestartDialog::class.java.name)
            true
        }

        val navBarOptions = findPreference<Preference>(PreferenceKeys.NAVBAR_ITEMS)
        navBarOptions?.setOnPreferenceClickListener {
            NavBarOptionsDialog().show(childFragmentManager, null)
            true
        }

        val chooseBg = findPreference<Preference>("choose_background")
        chooseBg?.setOnPreferenceClickListener {
            pickImage.launch("image/*")
            true
        }

        val removeBg = findPreference<Preference>("remove_background")
        removeBg?.setOnPreferenceClickListener {
            PreferenceHelper.putString(PreferenceKeys.CUSTOM_BACKGROUND_PATH, "")
            PreferenceHelper.putBoolean(PreferenceKeys.CUSTOM_BACKGROUND_ENABLED, false)
            Toast.makeText(requireContext(), R.string.background_removed, Toast.LENGTH_SHORT).show()
            true
        }
    }

}
