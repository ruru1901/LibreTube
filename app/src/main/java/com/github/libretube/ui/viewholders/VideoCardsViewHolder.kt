package com.github.libretube.ui.viewholders

import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.databinding.AllCaughtUpRowBinding
import com.github.libretube.databinding.ItemVideoBinding

class VideoCardsViewHolder : RecyclerView.ViewHolder {
    var itemVideoBinding: ItemVideoBinding? = null
    var allCaughtUpBinding: AllCaughtUpRowBinding? = null

    constructor(binding: ItemVideoBinding) : super(binding.root) {
        itemVideoBinding = binding
    }

    constructor(binding: AllCaughtUpRowBinding) : super(binding.root) {
        allCaughtUpBinding = binding
    }
}
