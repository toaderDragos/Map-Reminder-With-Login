package com.udacity.project4.locationreminders.savereminder.selectreminderlocation

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Criteria
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.Locale

private const val LOCATION_PERMISSION_INDEX = 0
private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29

// Removed the original implementation with Base Fragment because it didn't work at all - map wasn't loading
class SelectLocationFragment: Fragment() {

    private lateinit var googleMap: GoogleMap
    private lateinit var mMapView: MapView

    private lateinit var poiMarker: Marker
    private val REQUEST_LOCATION_PERMISSION = 1

    //Use Koin to get the view model of the SaveReminder
    val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private val runningQOrLater = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        // The user already selected a POI and the data is already sent to the viewmodel
        binding.saveLocationButton.setOnClickListener {

            // navigate back to the previous fragment to save the reminder and add the geofence
            findNavController().popBackStack()
        }

        // Implementation
        mMapView = binding.map
        mMapView.onCreate(savedInstanceState)
        mMapView.onResume() // needed to get the map to display immediately

        try {
            MapsInitializer.initialize(requireActivity().applicationContext)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // here is actually the onmap ready implementation.
        mMapView.getMapAsync { mMap ->
            googleMap = mMap

            // For checking everything and showing a move to my location and zooming on my location
            checkForegroundLocationPermissions()
            setHasOptionsMenu(true)
            setDisplayHomeAsUpEnabled(true)
            setMapLongClick(googleMap)
            setPoiClick(googleMap)
            setMapStyle(googleMap)

        }
        return binding.root
    }

    private fun setMapLongClick(map: GoogleMap) {
        map.setOnMapLongClickListener { latLng ->
            // A Snippet is Additional text that's displayed below the title.
            val snippet = String.format(
                Locale.getDefault(),
                "Lat: %1$.5f, Long: %2$.5f",
                latLng.latitude,
                latLng.longitude
            )
            map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(getString(R.string.dropped_pin))
                    .snippet(snippet)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
            )
            // create a Poi object
            val poi = PointOfInterest(latLng, "Dropped Pin", snippet)
            passPoiInfoToViewModel(latLng, poi, "Dropped Pin")
        }
    }

    // Places a marker on the map and displays an info window that contains POI name.
    private fun setPoiClick(map: GoogleMap) {
        map.setOnPoiClickListener { poi ->
            poiMarker = map.addMarker(
                MarkerOptions()
                    .position(poi.latLng)
                    .title(poi.name)
            )!!
            passPoiInfoToViewModel(poi.latLng, poi, poi.name)
            poiMarker.showInfoWindow()
        }
    }

    /**
     * There are 2 sets of LiveData - one for saving a reminder and another for updating a reminder.
     * It solves multiple confusions and conflicts splitting them into 2 sets.
     */
    private fun passPoiInfoToViewModel(
        latAndLng: LatLng,
        currentPOI: PointOfInterest,
        name: String
    ) {
        if (_viewModel.saveLocationButtonClickedFromUpdateOrDelete.value == true) {
            _viewModel.updatedReminderSelectedLocationStr.value = name
            _viewModel.updatedLatitude.value = latAndLng.latitude
            _viewModel.updatedLongitude.value = latAndLng.longitude
            // reset the value to false so that the user can select a new location
            _viewModel.saveLocationButtonClickedFromUpdateOrDelete.value = false

            val log2 = _viewModel.updatedReminderSelectedLocationStr.value
            println("dra Updated location in Select Location fragment: $log2 ")
            return
        }
        _viewModel.reminderSelectedLocationStr.value = name
        _viewModel.selectedPOI.value = currentPOI
        _viewModel.longitude.value = latAndLng.longitude
        _viewModel.latitude.value = latAndLng.latitude
    }

    private fun setMapStyle(map: GoogleMap) {
        try {
            // Customize the styling of the base map using a JSON raw object
            val success = map.setMapStyle(
                context?.let { MapStyleOptions.loadRawResourceStyle(it, R.raw.map_style) }
            )
            if (!success) { Log.e("dra", "Style parsing failed.") }
        } catch (e: Resources.NotFoundException) {
            Log.e("dra", "Can't find style. Error: ", e)
        }
    }

    // The permission is given above this method. Request permissions is called below from a Fragment context
    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        googleMap.isMyLocationEnabled = true
        zoomOnMyLocation()
    }

    private fun zoomOnMyLocation() {
        val locationManager = getSystemService(requireContext(), LocationManager::class.java)
        val criteria = Criteria()

        // If the user has not enabled the location services, then we don't zoom on his location
        if (context?.let {
                ActivityCompat.checkSelfPermission(it, Manifest.permission.ACCESS_FINE_LOCATION)
            } != PackageManager.PERMISSION_GRANTED && context?.let {
                ActivityCompat.checkSelfPermission(it, Manifest.permission.ACCESS_COARSE_LOCATION)
            } != PackageManager.PERMISSION_GRANTED
        ) {
            // display a message to the user that he has to enable the location services
            Toast.makeText(
                context,
                "In order for the phone to find your locations you must enable location services!",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val location =  locationManager!!.getLastKnownLocation(
            locationManager.getBestProvider(criteria, false)!!
        )
        if (location != null) {
            googleMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(location.latitude, location.longitude), 13f
                )
            )
            val cameraPosition = CameraPosition.Builder()
                .target(
                    LatLng(
                        location.latitude,
                        location.longitude
                    )
                ) // Sets the center of the map to location user
                .zoom(17f)    // Sets the zoom
                .bearing(90f) // Sets the orientation of the camera to east
                .tilt(40f)    // Sets the tilt of the camera to 40 degrees
                .build()      // Creates a CameraPosition from the builder
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    // Called whenever an item in your options menu is selected.
    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        // Change the map type based on the user's selection.
        R.id.normal_map -> {
            googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }

        R.id.hybrid_map -> {
            googleMap.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }

        R.id.satellite_map -> {
            googleMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }

        R.id.terrain_map -> {
            googleMap.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }

        else -> super.onOptionsItemSelected(item)
    }

    /***  STEP 1: Determines whether the app has the appropriate permissions */
    private fun foregroundLocationPermissionApproved(): Boolean {
        // is permission granted - the val below has the answer
        val foregroundLocationApproved = (
                PackageManager.PERMISSION_GRANTED == context?.let {
                    ActivityCompat.checkSelfPermission(it, Manifest.permission.ACCESS_FINE_LOCATION)
                })
        if (!foregroundLocationApproved) {
            // Show a toast message with a rationale to why the app needs this permission
            Toast.makeText(
                context,
                "Location permission is needed for core functionality",
                Toast.LENGTH_SHORT
            ).show()
        }
        return foregroundLocationApproved
    }

    /***  STEP 2: If the Location is approved, then continue, else Requests ACCESS_FINE_LOCATION and (on Android 10+ (Q)).*/
    private fun requestForegroundLocationPermissions() {
        if (foregroundLocationPermissionApproved())
            return
        val permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        Log.d("dra", "Request user's location")
        this.requestPermissions(permissionsArray, REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE)
    }

    /***  STEP 3: Checking  Fine Location, (Background Location IS NOT NECESSARY) and turned on location services*/
    private fun checkForegroundLocationPermissions() {
        if (foregroundLocationPermissionApproved()) {
            enableMyLocation()
            // set my location click listener
            googleMap.setOnMyLocationButtonClickListener {
                checkDeviceLocationSettings()
                true
            }
        } else {
            requestForegroundLocationPermissions()
        }
    }

    /***  STEP 4: Handle the result of the permission request */
    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (grantResults.isEmpty() ||
            grantResults[LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED
        ) {
            Snackbar.make(
                this.requireView(),
                R.string.permission_denied_explanation,
                Snackbar.LENGTH_INDEFINITE
            )
                .setAction(R.string.settings) {
                    startActivity(Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }.show()
        } else {
            // Gets called immediately after the user grants the location permission
            enableMyLocation()
        }
    }

    /**
     *  STEP 5: LOCATION SERVICES. Uses the Location Client to check the current state of location settings, and gives the user
     *  the opportunity to turn ON location services within our app. It is activated on click on the my location button
     */
    @SuppressLint("VisibleForTests")
    private fun checkDeviceLocationSettings(resolve: Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = activity?.let { LocationServices.getSettingsClient(it) }
        // Checks if the location is enabled on the phone
        val locationSettingsResponseTask = settingsClient?.checkLocationSettings(builder.build())
        locationSettingsResponseTask?.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                try {
                    startIntentSenderForResult(
                        exception.resolution.intentSender,
                        REQUEST_TURN_DEVICE_LOCATION_ON,
                        null, 0, 0, 0, null
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d("dra", "Error getting location settings resolution: " + sendEx.message)
                }
            }
            // If the user doesn't turn on the location services, then we display a message
            this.view?.let {
                Snackbar.make(it, R.string.location_required_error, Snackbar.LENGTH_LONG)
                    .setAction(android.R.string.ok) {
                        checkDeviceLocationSettings()
                    }.show()
            }
        }
        locationSettingsResponseTask?.addOnCompleteListener {
            if (it.isSuccessful) {
                Toast.makeText(context, "Device location turned on", Toast.LENGTH_SHORT).show()
                // If my location does not activate that means it's not really granted!!
                // Zoom on my location works when entering the map with location device setting ON
//                enableMyLocation()
                zoomOnMyLocation()
            }
        }
    }

}



