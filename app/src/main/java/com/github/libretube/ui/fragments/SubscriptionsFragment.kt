package com.github.libretube.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.libretube.R
import com.github.libretube.api.obj.Subscription
import com.github.libretube.databinding.FragmentSubscriptionsBinding
import com.github.libretube.helpers.NavigationHelper
import com.github.libretube.ui.adapters.SubscriptionsChannelAdapter
import com.github.libretube.ui.base.DynamicLayoutManagerFragment
import com.github.libretube.ui.models.SubscriptionsViewModel
import com.github.libretube.ui.sheets.SubscriptionsBottomSheet

class SubscriptionsFragment : DynamicLayoutManagerFragment(R.layout.fragment_subscriptions) {
    private var _binding: FragmentSubscriptionsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SubscriptionsViewModel by activityViewModels()

    private val channelAdapter = SubscriptionsChannelAdapter { subscription ->
        navigateToChannel(subscription)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentSubscriptionsBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)

        binding.subFeed.layoutManager = LinearLayoutManager(requireContext())
        binding.subFeed.adapter = channelAdapter

        binding.subProgress.isVisible = true

        if (viewModel.subscriptions.value == null) {
            viewModel.fetchFeed(requireContext(), forceRefresh = false)
        }

        viewModel.subscriptions.observe(viewLifecycleOwner) { subscriptions ->
            if (subscriptions != null) {
                showChannels(subscriptions)
            }
        }

        binding.subRefresh.setOnRefreshListener {
            viewModel.fetchFeed(requireContext(), forceRefresh = true)
        }

        binding.toggleSubs.isVisible = true
        binding.toggleSubs.setOnClickListener {
            SubscriptionsBottomSheet()
                .show(childFragmentManager)
        }
    }

    private fun showChannels(subscriptions: List<Subscription>) {
        binding.subProgress.isGone = true

        val notLoaded = subscriptions.isEmpty()
        binding.subFeed.isGone = notLoaded
        binding.emptyFeed.isVisible = notLoaded

        binding.toggleSubs.text = getString(R.string.subscriptions)

        binding.subRefresh.isRefreshing = false

        channelAdapter.submitList(subscriptions)
    }

    private fun navigateToChannel(subscription: Subscription) {
        NavigationHelper.navigateChannel(requireContext(), subscription.url)
    }

    override fun setLayoutManagers(gridItems: Int) {
        // not using grid layout
    }

    fun removeItem(videoId: String) {
        // channel list doesn't have items to remove
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
