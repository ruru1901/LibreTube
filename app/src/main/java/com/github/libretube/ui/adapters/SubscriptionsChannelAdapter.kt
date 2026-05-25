package com.github.libretube.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.api.obj.Subscription
import com.github.libretube.databinding.ItemSubscriptionChannelBinding
import com.github.libretube.helpers.ImageHelper

class SubscriptionsChannelAdapter(
    private val onChannelClick: (Subscription) -> Unit
) : RecyclerView.Adapter<SubscriptionsChannelAdapter.ViewHolder>() {

    private var channels: List<Subscription> = emptyList()

    fun submitList(list: List<Subscription>) {
        channels = list
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = channels.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSubscriptionChannelBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(channels[position])
    }

    inner class ViewHolder(
        private val binding: ItemSubscriptionChannelBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(subscription: Subscription) {
            binding.channelName.text = subscription.name
            ImageHelper.loadImage(subscription.avatar, binding.channelAvatar)
            binding.verifiedBadge.isVisible = subscription.verified
            binding.root.setOnClickListener {
                onChannelClick(subscription)
            }
        }
    }
}
