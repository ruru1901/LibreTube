package com.github.libretube.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.github.libretube.R
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.databinding.FragmentOnboardingBinding
import com.github.libretube.helpers.PreferenceHelper

class OnboardingFragment : Fragment(R.layout.fragment_onboarding) {
    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val languageMapping = mapOf(
            getString(R.string.lang_english) to "en",
            getString(R.string.lang_hindi) to "hi",
            getString(R.string.lang_tamil) to "ta",
            getString(R.string.lang_telugu) to "te",
            getString(R.string.lang_malayalam) to "ml",
            getString(R.string.lang_kannada) to "kn",
            getString(R.string.lang_bengali) to "bn",
            getString(R.string.lang_marathi) to "mr",
            getString(R.string.lang_punjabi) to "pa",
            getString(R.string.lang_other) to "other"
        )

        val categoryMapping = mapOf(
            getString(R.string.cat_music) to "music",
            getString(R.string.cat_gaming) to "gaming",
            getString(R.string.cat_news) to "news",
            getString(R.string.cat_education) to "education",
            getString(R.string.cat_comedy) to "comedy",
            getString(R.string.cat_tech) to "tech",
            getString(R.string.cat_sports) to "sports",
            getString(R.string.cat_vlogs) to "vlogs",
            getString(R.string.cat_cooking) to "cooking",
            getString(R.string.cat_finance) to "finance",
            getString(R.string.cat_fitness) to "fitness",
            getString(R.string.cat_movies) to "movies",
            getString(R.string.cat_anime) to "anime",
            getString(R.string.cat_podcasts) to "podcasts"
        )

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    requireActivity().finish()
                }
            }
        )

        binding.getStartedButton.setOnClickListener {
            val selectedLanguages = binding.languageChipGroup.checkedChipIds.map { chipId ->
                val chip = binding.languageChipGroup.findViewById<com.google.android.material.chip.Chip>(chipId)
                languageMapping[chip.text.toString()] ?: chip.text.toString()
            }
            PreferenceHelper.putString(
                PreferenceKeys.PREFERRED_LANGUAGES,
                selectedLanguages.joinToString(",")
            )

            val selectedCategories = binding.categoryChipGroup.checkedChipIds.mapNotNull { chipId ->
                val chip = binding.categoryChipGroup.findViewById<com.google.android.material.chip.Chip>(chipId)
                categoryMapping[chip.text.toString()]
            }
            PreferenceHelper.putString(
                PreferenceKeys.PREFERRED_CATEGORIES,
                selectedCategories.joinToString(",")
            )

            PreferenceHelper.putBoolean(PreferenceKeys.ONBOARDING_COMPLETE, true)

            findNavController().navigate(
                R.id.homeFragment,
                null,
                NavOptions.Builder()
                    .setPopUpTo(R.id.onboardingFragment, true)
                    .build()
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
