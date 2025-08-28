package com.alimaharramly.gpstracker.db

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.alimaharramly.gpstracker.R
import com.alimaharramly.gpstracker.databinding.TrackItemBinding
import com.alimaharramly.gpstracker.db.TrackAdapter.Holder

class TrackAdapter(private val listener: Listener) : ListAdapter<TrackItem, Holder>(Comparator()) {

    class Holder(view: View, private val listener: Listener) : RecyclerView.ViewHolder(view),
        View.OnClickListener {
        private val binding = TrackItemBinding.bind(view)
        private var trackTemp: TrackItem? = null

        init {
            binding.ibDelete.setOnClickListener(this)
            binding.item.setOnClickListener(this)
        }

        fun bind(track: TrackItem) = with(binding) {
            trackTemp = track
            val velocity = "${track.velocity} km/h"
            val distanceInKm = track.distance / 1000.0
            val formattedDistance = "%.2f km".format(distanceInKm)
            tvDistance.text = formattedDistance
            tvVelocity.text = velocity
            tvDate.text = track.date
            tvTime.text = track.time
        }

        override fun onClick(view: View?) {
           val type = when(view?.id) {
                R.id.ibDelete -> ClickType.DELETE
                R.id.item -> ClickType.OPEN
               else -> ClickType.OPEN
            }
            trackTemp?.let { listener.onClick(it, type) }

        }
    }

    class Comparator : DiffUtil.ItemCallback<TrackItem>() {
        override fun areItemsTheSame(
            oldItem: TrackItem,
            newItem: TrackItem
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: TrackItem,
            newItem: TrackItem
        ): Boolean {
            return oldItem == newItem
        }

    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.track_item, parent, false)
        return Holder(view, listener)
    }

    override fun onBindViewHolder(
        holder: Holder,
        position: Int
    ) {
        holder.bind(getItem(position))
    }

    interface Listener {
        fun onClick(track: TrackItem, type: ClickType)
    }

    enum class ClickType {
        DELETE,
        OPEN
    }
}
