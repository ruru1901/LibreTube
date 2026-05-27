package com.github.libretube.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.ListAdapter
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.databinding.ItemVideoHorizontalBinding
import com.github.libretube.extensions.toID
import com.github.libretube.helpers.ImageHelper
import com.github.libretube.helpers.NavigationHelper
import com.github.libretube.parcelable.PlayerData
import com.github.libretube.ui.adapters.callbacks.DiffUtilItemCallback
import com.github.libretube.ui.extensions.setFormattedDuration

class HorizontalVideoCardsAdapter :
    ListAdapter<StreamItem, HorizontalVideoCardsAdapter.HorizontalViewHolder>(DiffUtilItemCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HorizontalViewHolder {
        val binding = ItemVideoHorizontalBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return HorizontalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HorizontalViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class HorizontalViewHolder(
        private val binding: ItemVideoHorizontalBinding
    ) : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {

        fun bind(video: StreamItem) {
            val context = binding.root.context
            val videoId = video.url.orEmpty().toID()

            val screenWidth = context.resources.displayMetrics.widthPixels
            val cardWidth = (screenWidth * 0.75).toInt()
            binding.root.updateLayoutParams {
                width = cardWidth
            }

            ImageHelper.loadImage(video.thumbnail, binding.thumbnail)
            binding.videoTitle.text = video.title
            binding.channelName.text = video.uploaderName
            if (video.uploaderAvatar != null) {
                ImageHelper.loadImage(video.uploaderAvatar, binding.channelAvatar, true)
            }

            video.duration?.let {
                binding.thumbnailDuration.setFormattedDuration(it, video.isShort, video.uploaded)
            }

            binding.root.setOnClickListener {
                NavigationHelper.navigateVideo(context, PlayerData(videoId))
            }
        }
    }
}
