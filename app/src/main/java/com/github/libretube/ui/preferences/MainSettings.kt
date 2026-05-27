package com.github.libretube.ui.preferences

import android.os.Bundle
import android.widget.LinearLayout
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import com.github.libretube.BuildConfig
import com.github.libretube.R
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.ui.base.BasePreferenceFragment
import com.github.libretube.ui.dialogs.ErrorDialog
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainSettings : BasePreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)

        val crashlog = findPreference<Preference>("crashlog")
        crashlog?.isVisible = PreferenceHelper.getErrorLog().isNotEmpty() && BuildConfig.DEBUG
        crashlog?.setOnPreferenceClickListener {
            ErrorDialog().show(childFragmentManager, null)
            crashlog.isVisible = false
            true
        }

        findPreference<Preference>("content_preferences")?.setOnPreferenceClickListener {
            showContentPreferencesDialog()
            true
        }

        listOf(
            "general" to R.id.action_global_generalSettings,
            "appearance" to R.id.action_global_appearanceSettings,
            "sponsorblock" to R.id.action_global_sponsorBlockSettings,
            "player" to R.id.action_global_playerSettings,
            "audio_video" to R.id.action_global_audioVideoSettings,
            "history" to R.id.action_global_historySettings,
            "notifications" to R.id.action_global_notificationSettings,
            "backup_restore" to R.id.action_global_backupRestoreSettings
        ).forEach { (preferenceKey, actionId) ->
            findPreference<Preference>(preferenceKey)?.setOnPreferenceClickListener { _ ->
                findNavController().navigate(actionId)
                true
            }
        }
    }

    private fun showContentPreferencesDialog() {
        val context = requireContext()
        val rootLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 32)
        }

        val savedLanguages = PreferenceHelper.getString(PreferenceKeys.PREFERRED_LANGUAGES, "")
            .split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
        val savedCategories = PreferenceHelper.getString(PreferenceKeys.PREFERRED_CATEGORIES, "")
            .split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()

        val languageLabel = android.widget.TextView(context).apply {
            text = context.getString(R.string.onboarding_title_languages)
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium)
            setPadding(0, 0, 0, 16)
        }
        rootLayout.addView(languageLabel)

        val languageEntries = listOf(
            R.string.lang_english to "en", R.string.lang_hindi to "hi",
            R.string.lang_tamil to "ta", R.string.lang_telugu to "te",
            R.string.lang_malayalam to "ml", R.string.lang_kannada to "kn",
            R.string.lang_bengali to "bn", R.string.lang_marathi to "mr",
            R.string.lang_punjabi to "pa", R.string.lang_other to "other"
        )
        val languageChipGroup = ChipGroup(context).apply {
            isSelectionRequired = false
            isSingleSelection = false
        }
        languageEntries.forEach { (resId, code) ->
            val chip = Chip(context).apply {
                text = context.getString(resId)
                isCheckable = true
                isChecked = code in savedLanguages || context.getString(resId) in savedLanguages
            }
            languageChipGroup.addView(chip)
        }
        rootLayout.addView(languageChipGroup)

        val categoryLabel = android.widget.TextView(context).apply {
            text = context.getString(R.string.onboarding_title_categories)
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium)
            setPadding(0, 24, 0, 16)
        }
        rootLayout.addView(categoryLabel)

        val categoryEntries = listOf(
            R.string.cat_music to "music", R.string.cat_gaming to "gaming",
            R.string.cat_news to "news", R.string.cat_education to "education",
            R.string.cat_comedy to "comedy", R.string.cat_tech to "tech",
            R.string.cat_sports to "sports", R.string.cat_vlogs to "vlogs",
            R.string.cat_cooking to "cooking", R.string.cat_finance to "finance",
            R.string.cat_fitness to "fitness", R.string.cat_movies to "movies",
            R.string.cat_anime to "anime", R.string.cat_podcasts to "podcasts"
        )
        val categoryChipGroup = ChipGroup(context).apply {
            isSelectionRequired = false
            isSingleSelection = false
        }
        categoryEntries.forEach { (resId, id) ->
            val chip = Chip(context).apply {
                text = context.getString(resId)
                isCheckable = true
                isChecked = id in savedCategories || context.getString(resId) in savedCategories
            }
            categoryChipGroup.addView(chip)
        }
        rootLayout.addView(categoryChipGroup)

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.content_preferences_dialog_title)
            .setView(rootLayout)
            .setPositiveButton(R.string.okay) { _, _ ->
                val selectedLanguages = (0 until languageChipGroup.childCount).mapNotNull { i ->
                    val chip = languageChipGroup.getChildAt(i) as? Chip ?: return@mapNotNull null
                    if (chip.isChecked) {
                        val label = chip.text.toString()
                        languageEntries.firstOrNull { context.getString(it.first) == label }?.second ?: label
                    } else null
                }
                PreferenceHelper.putString(
                    PreferenceKeys.PREFERRED_LANGUAGES,
                    selectedLanguages.joinToString(",")
                )

                val selectedCategories = (0 until categoryChipGroup.childCount).mapNotNull { i ->
                    val chip = categoryChipGroup.getChildAt(i) as? Chip ?: return@mapNotNull null
                    if (chip.isChecked) {
                        val label = chip.text.toString()
                        categoryEntries.firstOrNull { context.getString(it.first) == label }?.second ?: label
                    } else null
                }
                PreferenceHelper.putString(
                    PreferenceKeys.PREFERRED_CATEGORIES,
                    selectedCategories.joinToString(",")
                )
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
