package com.alimaharramly.gpstracker.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.alimaharramly.gpstracker.MainApp
import com.alimaharramly.gpstracker.MainViewModel
import com.alimaharramly.gpstracker.databinding.TracksBinding
import com.alimaharramly.gpstracker.db.TrackAdapter
import com.alimaharramly.gpstracker.db.TrackItem
import com.alimaharramly.gpstracker.utils.openFragment
import kotlin.getValue

class TracksFragment : Fragment(), TrackAdapter.Listener {
    private lateinit var binding: TracksBinding
    private lateinit var adapter: TrackAdapter
    private val model: MainViewModel by activityViewModels {
        MainViewModel.ViewModelFactory((requireContext().applicationContext as MainApp).database)
    }  // Инициализация класса MainViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            TracksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRcView()
        getTracks()
    }

    private fun getTracks() {
        model.tracks.observe(viewLifecycleOwner) {
            adapter.submitList(it)
            binding.tvEmpty.visibility = if (it.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun initRcView() = with(binding) {
        adapter = TrackAdapter(this@TracksFragment)
        rcView.layoutManager = LinearLayoutManager(requireContext())
        rcView.adapter = adapter
    }

    override fun onClick(track: TrackItem, type: TrackAdapter.ClickType) {
        when(type){
            TrackAdapter.ClickType.DELETE -> model.deleteTrack(track)
            TrackAdapter.ClickType.OPEN -> {
                model.currentTrack.value = track
                openFragment(ViewTrackFragment.newInstance())
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            TracksFragment()
    }
}