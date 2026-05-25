package com.github.libretube.ui.fragments

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.github.libretube.NavDirections
import com.github.libretube.R
import com.github.libretube.constants.PreferenceKeys.HOME_TAB_CONTENT
import com.github.libretube.databinding.FragmentHomeBinding
import com.github.libretube.helpers.PlayerHelper
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.ui.adapters.CategorySectionsAdapter
import com.github.libretube.ui.adapters.VideoCardsAdapter
import com.github.libretube.ui.models.HomeViewModel
import com.github.libretube.ui.models.SubscriptionsViewModel

class HomeFragment : Fragment(R.layout.fragment_home) {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val homeViewModel: HomeViewModel by activityViewModels()
    private val subscriptionsViewModel: SubscriptionsViewModel by activityViewModels()

    private val feedAdapter = VideoCardsAdapter()
    private val categorySectionsAdapter = CategorySectionsAdapter(
        onSeeAllClick = { query ->
            try {
                findNavController().navigate(NavDirections.showSearchResults(query))
            } catch (_: Exception) {
                // nav controller may not be available
            }
        },
        onContinueWatchingSeeAll = null
    )
    private val defaultAdapter by lazy { ConcatAdapter(feedAdapter, categorySectionsAdapter) }
    private val verticalFeedAdapter = VideoCardsAdapter()
    private var categoryFeedData: HomeViewModel.CategoryFeedData? = null
    private val categoryMruOrder = mutableListOf<String>()
    private var isRebuildingChips = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentHomeBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)

        binding.feedRv.layoutManager = LinearLayoutManager(requireContext())
        binding.feedRv.adapter = defaultAdapter

        binding.refresh.setOnRefreshListener {
            binding.refresh.isRefreshing = true
            fetchHomeFeed()
        }

        binding.refreshButton.setOnClickListener {
            fetchHomeFeed()
        }

        with(homeViewModel) {
            feed.observe(viewLifecycleOwner, ::showFeed)
            categoryFeeds.observe(viewLifecycleOwner) { data ->
                if (data != null) {
                    categoryFeedData = data
                    categorySectionsAdapter.submitSections(
                        data.categoryIds,
                        data.labels,
                        data.queries,
                        data.videos
                    )
                    loadMruOrder()
                    buildCategoryChips(data.labels)
                }
            }
            isLoading.observe(viewLifecycleOwner, ::updateLoading)
        }

        binding.categoryChipGroup.setOnCheckedStateChangeListener { group: com.google.android.material.chip.ChipGroup, checkedIds: MutableList<Int> ->
            val chipId = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener
            val chip = group.findViewById<com.google.android.material.chip.Chip>(chipId)
            updateFeedForCategory(chip.text.toString())
        }

        if (!PlayerHelper.watchHistoryEnabled) {
            binding.continueWatchingChip.isGone = true
        }

        homeViewModel.continueWatching.observe(viewLifecycleOwner) { videos ->
            if (!videos.isNullOrEmpty()) {
                categorySectionsAdapter.setContinueWatchingSection(
                    videos,
                    getString(R.string.continue_watching)
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (homeViewModel.loadedSuccessfully.value == false) {
            fetchHomeFeed()
        }
        binding.chipAll.isChecked = true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun fetchHomeFeed() {
        binding.nothingHere.isGone = true
        val defaultItems = resources.getStringArray(R.array.homeTabItemsValues)
        val visibleItems = PreferenceHelper.getStringSet(HOME_TAB_CONTENT, defaultItems.toSet())

        homeViewModel.loadHomeFeed(
            context = requireContext(),
            subscriptionsViewModel = subscriptionsViewModel,
            visibleItems = visibleItems
        )
    }

    private fun showFeed(streamItems: List<com.github.libretube.api.obj.StreamItem>?) {
        if (streamItems == null) return
        binding.feedRv.isVisible = true
        feedAdapter.submitList(streamItems)
    }

    private fun updateFeedForCategory(label: String) {
        val context = requireContext()
        val allLabel = context.getString(R.string.all)
        val staticLabels = setOf(
            allLabel,
            context.getString(R.string.subscriptions),
            context.getString(R.string.continue_watching)
        )
        if (label !in staticLabels && !isRebuildingChips) {
            categoryMruOrder.remove(label)
            categoryMruOrder.add(0, label)
            saveMruOrder()
            rebuildCategoryChipsOrder()
        }
        if (label == context.getString(R.string.subscriptions)) {
            findNavController().navigate(R.id.action_homeFragment_to_subscriptionsFragment)
            return
        }
        if (label == allLabel || categoryFeedData == null) {
            binding.feedRv.layoutManager = LinearLayoutManager(context)
            binding.feedRv.adapter = defaultAdapter
            return
        }
        val data = categoryFeedData!!
        val idx = data.labels.indexOfFirst { it.equals(label, ignoreCase = true) }
        if (idx < 0) {
            binding.feedRv.layoutManager = LinearLayoutManager(context)
            binding.feedRv.adapter = defaultAdapter
            return
        }
        verticalFeedAdapter.submitList(data.videos[idx])
        binding.feedRv.layoutManager = LinearLayoutManager(context)
        binding.feedRv.adapter = verticalFeedAdapter
    }

    private fun loadMruOrder() {
        val raw = PreferenceHelper.getString("category_mru_order", "")
        if (raw.isBlank()) return
        categoryMruOrder.clear()
        categoryMruOrder.addAll(raw.split(","))
    }

    private fun saveMruOrder() {
        val raw = categoryMruOrder.joinToString(",")
        PreferenceHelper.putString("category_mru_order", raw)
    }

    private fun buildCategoryChips(labels: List<String>) {
        if (isRebuildingChips) return
        isRebuildingChips = true
        val chipGroup = binding.categoryChipGroup
        val context = requireContext()
        val staticLabels = setOf(
            context.getString(R.string.all),
            context.getString(R.string.subscriptions),
            context.getString(R.string.continue_watching)
        )

        val oldDynamic = (0 until chipGroup.childCount)
            .map { chipGroup.getChildAt(it) }
            .filterIsInstance<Chip>()
            .filter { it.text.toString() !in staticLabels }
        oldDynamic.forEach { chipGroup.removeView(it) }

        val sortedLabels = labels.sortedWith(
            compareBy<String> { label ->
                val idx = categoryMruOrder.indexOf(label)
                if (idx >= 0) idx else Int.MAX_VALUE
            }.thenBy { it }
        )

        val subscriptionsIndex = (0 until chipGroup.childCount)
            .firstOrNull {
                val child = chipGroup.getChildAt(it)
                child is Chip && child.text.toString() == context.getString(R.string.subscriptions)
            } ?: chipGroup.childCount

        var insertIndex = subscriptionsIndex
        for (label in sortedLabels) {
            val chip = createCategoryChip(context, label)
            chipGroup.addView(chip, insertIndex)
            insertIndex++
        }
        isRebuildingChips = false
    }

    private fun rebuildCategoryChipsOrder() {
        if (isRebuildingChips) return
        isRebuildingChips = true
        val chipGroup = binding.categoryChipGroup
        val context = requireContext()
        val staticLabels = setOf(
            context.getString(R.string.all),
            context.getString(R.string.subscriptions),
            context.getString(R.string.continue_watching)
        )

        val dynamicChips = (0 until chipGroup.childCount)
            .map { chipGroup.getChildAt(it) }
            .filterIsInstance<Chip>()
            .filter { it.text.toString() !in staticLabels }

        if (dynamicChips.isEmpty()) return

        val sorted = dynamicChips.sortedWith(
            compareBy<Chip> { chip ->
                val idx = categoryMruOrder.indexOf(chip.text.toString())
                if (idx >= 0) idx else Int.MAX_VALUE
            }.thenBy { it.text.toString() }
        )

        if (sorted.map { it.text.toString() } == dynamicChips.map { it.text.toString() }) return

        val selectedText = dynamicChips.firstOrNull { it.isChecked }?.text?.toString()

        dynamicChips.forEach { chipGroup.removeView(it) }

        val subscriptionsIndex = (0 until chipGroup.childCount)
            .firstOrNull {
                val child = chipGroup.getChildAt(it)
                child is Chip && child.text.toString() == context.getString(R.string.subscriptions)
            } ?: chipGroup.childCount

        var insertIndex = subscriptionsIndex
        for (chip in sorted) {
            chipGroup.addView(chip, insertIndex)
            insertIndex++
        }

        if (selectedText != null) {
            for (i in subscriptionsIndex until chipGroup.childCount) {
                val child = chipGroup.getChildAt(i)
                if (child is Chip && child.text.toString() == selectedText) {
                    child.isChecked = true
                    break
                }
            }
        }
        isRebuildingChips = false
    }

    private fun createCategoryChip(context: android.content.Context, label: String): Chip {
        val styled = android.view.ContextThemeWrapper(context, com.google.android.material.R.style.Widget_Material3_Chip_Filter)
        return Chip(styled).apply {
            text = label
            isCheckable = true
            isClickable = true
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(
                ResourcesCompat.getColorStateList(
                    context.resources,
                    R.color.chip_text_selector,
                    context.theme
                )
            )
            setChipBackgroundColorResource(R.color.chip_bg_selector)
            setChipStrokeColorResource(R.color.chip_stroke_selector)
            chipStrokeWidth = 1f
            chipCornerRadius = 14f
            val heightPx = (48f * context.resources.displayMetrics.density).toInt()
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                heightPx
            )
        }
    }

    private fun updateLoading(isLoading: Boolean) {
        if (isLoading) {
            showLoading()
        } else {
            hideLoading()
        }
    }

    private fun showLoading() {
        binding.progress.isVisible = !binding.refresh.isRefreshing
        binding.nothingHere.isVisible = false
    }

    private fun hideLoading() {
        binding.progress.isVisible = false
        binding.refresh.isRefreshing = false

        if (homeViewModel.loadedSuccessfully.value == true) {
            showContent()
        } else {
            showNothingHere()
        }
    }

    private fun showNothingHere() {
        binding.nothingHere.isVisible = true
        binding.feedRv.isVisible = false
    }

    private fun showContent() {
        binding.nothingHere.isVisible = false
        binding.feedRv.isVisible = true
    }
}
