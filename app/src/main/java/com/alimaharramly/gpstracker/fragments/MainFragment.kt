package com.alimaharramly.gpstracker.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import com.alimaharramly.gpstracker.MainApp
import com.alimaharramly.gpstracker.MainViewModel
import com.alimaharramly.gpstracker.R
import com.alimaharramly.gpstracker.databinding.FragmentMainBinding
import com.alimaharramly.gpstracker.db.TrackItem
import com.alimaharramly.gpstracker.location.LocationModel
import com.alimaharramly.gpstracker.location.LocationService
import com.alimaharramly.gpstracker.utils.DialogManager
import com.alimaharramly.gpstracker.utils.TimeUtils
import com.alimaharramly.gpstracker.utils.checkPermission
import com.alimaharramly.gpstracker.utils.showToast
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.util.Timer
import java.util.TimerTask

class MainFragment : Fragment() {

    private var locationModel: LocationModel? = null
    private var pl: Polyline? = null
    private var isServiceRunning = false
    private var firstStart = true
    private var timer: Timer? = null
    private var startTime = 0L
    private var isMapInitialized = false
    private lateinit var pLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var notificationPermissionLauncher: ActivityResultLauncher<String>
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    private val model: MainViewModel by activityViewModels {
        MainViewModel.ViewModelFactory((requireContext().applicationContext as MainApp).database)
    }
    private var wasGpsEnabled: Boolean? = null
    private lateinit var myLocOverlay: MyLocationNewOverlay

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerPermissions()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        settingsOsm()
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        registerNotificationPermission()
        checkAndRequestPermissions()
        checkAndRequestNotificationPermission()
        checkNotificationsEnabled()
        setOnClicks()
        checkServiceState()
        updateTime()
        registerLocReceiver()
        locationUpdates()
    }

    private fun settingsOsm() {
        Configuration.getInstance().load(
            activity as AppCompatActivity,
            activity?.getSharedPreferences("osm_pref", Context.MODE_PRIVATE)
        )
        Configuration.getInstance().userAgentValue = requireContext().packageName
    }

    private fun initOSM() {
        if (isMapInitialized) return
        isMapInitialized = true

        pl = Polyline().apply {
            outlinePaint.color = PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getString("color_key", "#0096FF")?.toColorInt()!!
        }

        binding.map.apply {
            setMultiTouchControls(true)
            minZoomLevel = 3.0
            maxZoomLevel = 20.0
        }

        val provider = GpsMyLocationProvider(requireActivity())
        myLocOverlay = MyLocationNewOverlay(provider, binding.map).apply {
            enableMyLocation()
            enableFollowLocation()
            runOnFirstFix {
                activity?.runOnUiThread {
                    binding.map.overlays.removeAll { it is Polyline || it is MyLocationNewOverlay }
                    binding.map.overlays.add(this)
                    binding.map.overlays.add(pl)
                    binding.map.controller.setZoom(15.0)
                    binding.map.controller.setCenter(myLocation)
                }
            }
        }
    }

    private fun registerPermissions() {
        pLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
            if (granted) {
                Log.d("PERM", "Permission granted")
                initOSM()
                checkLocationEnabled()
            } else {
                Log.d("PERM", "Permission denied")
                showToast("GPS permission not provided")
            }
        }
    }


    private fun registerNotificationPermission() {
        notificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                showToast("Notification permission denied")
            }
        }
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
            if (!prefs.getBoolean("notification_permission_requested", false)) {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    prefs.edit { putBoolean("notification_permission_requested", true) }
                }
            }
        }
    }

    private fun checkNotificationsEnabled() {
        val areEnabled = NotificationManagerCompat.from(requireContext()).areNotificationsEnabled()
        if (!areEnabled) {
            showToast("Notifications are disabled in settings")
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
            }
            startActivity(intent)
        }
    }

    private fun checkAndRequestPermissions() {
        val fineGranted = checkPermission(Manifest.permission.ACCESS_FINE_LOCATION)

        if (fineGranted) {
            Log.d("PERM", "Already granted")
            initOSM()
            checkLocationEnabled()
        } else {
            Log.d("PERM", "Requesting permission")
            pLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            )
        }
    }


    private fun setOnClicks() {
        val listener = View.OnClickListener {
            when (it.id) {
                R.id.fStartStop -> startStopService()
                R.id.fCenter -> centerLocation()
            }
        }
        binding.fStartStop.setOnClickListener(listener)
        binding.fCenter.setOnClickListener(listener)
    }

    private fun centerLocation() {
        myLocOverlay.myLocation?.let {
            binding.map.controller.animateTo(it)
            myLocOverlay.enableFollowLocation()
        }
    }

    private fun locationUpdates() = with(binding) {
        model.locationUpdates.observe(viewLifecycleOwner) {
            if (locationModel?.distance != it.distance) {
                tvDistance.text = "Distance: %.1f m".format(it.distance)
            }
            tvVelocity.text = "Velocity: %.1f km/h".format(3.6f * it.velocity)
            tvAverageVel.text = "Average Velocity: ${getAverageSpeed(it.distance)} km/h"
            locationModel = it
            updatePolyline(it.geoPointsList)
        }
    }

    private fun updateTime() {
        model.timeData.observe(viewLifecycleOwner) {
            binding.tvTime.text = it
        }
    }

    private fun startTimer() {
        timer?.cancel()
        timer = Timer()
        startTime = LocationService.startTime
        timer?.schedule(object : TimerTask() {
            override fun run() {
                activity?.runOnUiThread {
                    model.timeData.value = getCurrentTime()
                }
            }
        }, 1000, 1000)
    }

    private fun getAverageSpeed(distance: Float): String {
        val elapsedTime = (System.currentTimeMillis() - startTime) / 1000.0f
        return "%.1f".format(3.6f * (distance / elapsedTime))
    }

    private fun getCurrentTime(): String {
        return "Time: ${TimeUtils.getTime(System.currentTimeMillis() - startTime)}"
    }

    @SuppressLint("ImplicitSamInstance")
    private fun startStopService() {
        if (!isServiceRunning) {
            startLocService()
        } else {
            activity?.stopService(Intent(activity, LocationService::class.java))
            binding.fStartStop.setImageResource(R.drawable.ic_play)
            timer?.cancel()
            val track = getTrackItem()
            DialogManager.showSaveDialog(requireContext(), track, object : DialogManager.Listener {
                override fun onClick() {
                    showToast("Track saved!")
                    model.insertTrack(track)
                }
            })
        }
        isServiceRunning = !isServiceRunning
    }

    private fun startLocService() {
        val intent = Intent(activity, LocationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity?.startForegroundService(intent)
        } else {
            activity?.startService(intent)
        }
        binding.fStartStop.setImageResource(R.drawable.ic_stop)
        LocationService.startTime = System.currentTimeMillis()
        startTimer()
    }

    private fun getTrackItem(): TrackItem {
        return TrackItem(
            null,
            getCurrentTime(),
            TimeUtils.getDate(),
            locationModel?.distance?.toDouble() ?: 0.0,
            getAverageSpeed(locationModel?.distance ?: 0.0f),
            geoPointsToString(locationModel?.geoPointsList ?: emptyList())
        )
    }

    private fun geoPointsToString(list: List<GeoPoint>): String {
        return list.joinToString("/") { "${it.latitude},${it.longitude}" }
    }

    private fun updatePolyline(list: List<GeoPoint>) {
        if (list.isEmpty()) return
        if (firstStart && list.size > 1) {
            pl?.setPoints(list)
            firstStart = false
        } else {
            val last = list.last()
            if (pl?.actualPoints?.lastOrNull() != last) {
                pl?.addPoint(last)
            }
        }
    }

    private fun checkServiceState() {
        isServiceRunning = LocationService.isRunning
        binding.fStartStop.setImageResource(
            if (isServiceRunning) R.drawable.ic_stop else R.drawable.ic_play
        )
        if (isServiceRunning) startTimer()
    }

    override fun onResume() {
        super.onResume()
        checkLocationEnabled()
        firstStart = true
    }

    private fun checkLocationEnabled() {
        val lManager = activity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isEnabled = lManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        if (wasGpsEnabled == null || wasGpsEnabled != isEnabled) {
            wasGpsEnabled = isEnabled
            if (!isEnabled) {
                DialogManager.showEnableDialog(activity as AppCompatActivity, object : DialogManager.Listener {
                    override fun onClick() {
                        startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    }
                })
            } else {
                initOSM()
            }
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == LocationService.LOC_MODEL_INTENT) {
                val locModel = intent.getSerializableExtra(LocationService.LOC_MODEL_INTENT) as? LocationModel
                locModel?.let { model.locationUpdates.value = it }
            }
        }
    }

    private fun registerLocReceiver() {
        val locFilter = IntentFilter(LocationService.LOC_MODEL_INTENT)
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(receiver, locFilter)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDetach() {
        super.onDetach()
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(receiver)
    }

    companion object {
        @JvmStatic
        fun newInstance() = MainFragment()
    }
}
