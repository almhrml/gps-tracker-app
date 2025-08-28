package com.alimaharramly.gpstracker.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.preference.PreferenceManager
import com.alimaharramly.gpstracker.MainApp
import com.alimaharramly.gpstracker.MainViewModel
import com.alimaharramly.gpstracker.R
import com.alimaharramly.gpstracker.databinding.ViewTrackBinding
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

class ViewTrackFragment : Fragment() {
    private var startPoint: GeoPoint? = null
    private lateinit var binding: ViewTrackBinding
    private val model: MainViewModel by activityViewModels {
        MainViewModel.ViewModelFactory((requireContext().applicationContext as MainApp).database)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        settingsOsm()
        binding = ViewTrackBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getTrack()
        binding.fCenter.setOnClickListener {
            if (startPoint != null) binding.map.controller.animateTo(startPoint)
        }
    }

    private fun getTrack() = with(binding) {
        model.currentTrack.observe(viewLifecycleOwner) {
            val date = "Date: ${it.date}"
            val averageVel = "Average velocity: ${it.velocity} km/h"

            val distanceText = if (it.distance < 1000) {
                "Distance: ${it.distance.toInt()} m" // в метрах
            } else {
                val distanceInKm = it.distance / 1000.0
                "Distance: %.2f km".format(distanceInKm) // в км
            }

            tvAverageVel.text = averageVel
            tvDistance.text = distanceText
            tvDate.text = date
            tvTime.text = it.time
            val polyline = getPolyline(it.geoPoints)
            map.overlays.add(polyline)
            setMarkers(polyline.actualPoints)
            goToStartPosition(polyline.actualPoints[0])
            startPoint = polyline.actualPoints[0]
        }
    }

    private fun goToStartPosition(startPosition: GeoPoint) {
        binding.map.controller.zoomTo(14.0)
        binding.map.controller.animateTo(startPosition)
        binding.map.setMultiTouchControls(true)
    }

    private fun setMarkers(list: List<GeoPoint>) = with(binding) {
        val startMarker = Marker(map)
        val finishMarker = Marker(map)
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        finishMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        startMarker.icon = getDrawable(requireContext(), R.drawable.ic_start_position)
        finishMarker.icon = getDrawable(requireContext(), R.drawable.ic_finish_position)
        startMarker.position = list[0]
        finishMarker.position = list[list.size - 1]
        map.overlays.add(startMarker)
        map.overlays.add(finishMarker)
    }

    private fun getPolyline(geoPoints: String): Polyline {
        val polyline = Polyline()
        polyline.outlinePaint.color =
            PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getString("color_key", "#0096FF")?.toColorInt()!!
        val list = geoPoints.split("/")
        list.forEach {
            if (it.isEmpty()) return@forEach
            val points = it.split(",")
            polyline.addPoint(GeoPoint(
                    points[0].toDouble(),
                    points[1].toDouble()
                )
            )
        }
        return polyline
    }

    private fun settingsOsm() {
        Configuration.getInstance().load(
            activity as AppCompatActivity,
            activity?.getSharedPreferences("osm_pref", Context.MODE_PRIVATE)
        )
        Configuration.getInstance().userAgentValue = requireContext().packageName
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            ViewTrackFragment()
    }
}