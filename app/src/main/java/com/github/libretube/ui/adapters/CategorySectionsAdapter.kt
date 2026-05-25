package com.github.libretube.ui.adapters

import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.helpers.NavigationHelper
import com.github.libretube.parcelable.PlayerData
import com.github.libretube.extensions.dpToPx

class CategorySectionsAdapter(
    private val onSeeAllClick: (query: String) -> Unit,
    private val onContinueWatchingSeeAll: (() -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    data class SectionData(
        val categoryId: String,
        val label: String,
        val query: String,
        val videos: List<StreamItem>
    )

    private var sections: List<SectionData> = emptyList()

    private sealed class Item {
        data class Header(val section: SectionData) : Item()
        data class Row(val section: SectionData) : Item()
    }

    private var flatItems: List<Item> = emptyList()

    fun setContinueWatchingSection(videos: List<StreamItem>, label: String) {
        val cwSection = SectionData(
            categoryId = "continue_watching",
            label = label,
            query = "",
            videos = videos
        )
        val sectionsWithoutCW = sections.filter { it.categoryId != "continue_watching" }
        sections = listOf(cwSection) + sectionsWithoutCW
        flatItems = sections.flatMap { listOf(Item.Header(it), Item.Row(it)) }
        notifyDataSetChanged()
    }

    fun submitSections(
        categoryIdList: List<String>,
        labelList: List<String>,
        queryList: List<String>,
        videosList: List<List<StreamItem>>
    ) {
        sections = categoryIdList.mapIndexed { index, id ->
            SectionData(id, labelList[index], queryList[index], videosList[index])
        }
        flatItems = sections.flatMap { listOf(Item.Header(it), Item.Row(it)) }
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = flatItems.size

    override fun getItemViewType(position: Int): Int {
        return when (flatItems[position]) {
            is Item.Header -> TYPE_HEADER
            is Item.Row -> TYPE_ROW
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> {
                val view = inflater.inflate(R.layout.item_category_section_header, parent, false)
                HeaderViewHolder(view)
            }
            TYPE_ROW -> {
                val view = inflater.inflate(R.layout.item_category_video_row, parent, false)
                RowViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = flatItems[position]) {
            is Item.Header -> (holder as HeaderViewHolder).bind(item.section)
            is Item.Row -> (holder as RowViewHolder).bind(item.section)
        }
    }

    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleView: TextView = itemView.findViewById(R.id.section_title)
        private val seeAllButton: TextView = itemView.findViewById(R.id.see_all_button)

        fun bind(section: SectionData) {
            titleView.text = section.label
            seeAllButton.setOnClickListener {
                if (section.categoryId == "continue_watching") {
                    onContinueWatchingSeeAll?.invoke()
                } else {
                    onSeeAllClick(section.query)
                }
            }
        }
    }

    inner class RowViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val recyclerView: RecyclerView = itemView.findViewById(R.id.category_row_rv)
        private val horizontalAdapter = HorizontalVideoCardsAdapter()

        init {
            val context = recyclerView.context
            recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            recyclerView.isNestedScrollingEnabled = false
            recyclerView.adapter = horizontalAdapter

            val spacingPx = 8f.dpToPx()
            recyclerView.addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                    outRect.left = spacingPx
                    outRect.right = spacingPx
                }
            })
        }

        fun bind(section: SectionData) {
            horizontalAdapter.submitList(section.videos)
        }
    }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ROW = 1
    }
}
