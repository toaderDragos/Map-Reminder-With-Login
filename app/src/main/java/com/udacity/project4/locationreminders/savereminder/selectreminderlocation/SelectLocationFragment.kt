package com.udacity.project4.locationreminders.savereminder.selectreminderlocation

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Criteria
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
import com.udacity.project4.R
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

private const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
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
            // findNavController().navigate(SelectLocationFragmentDirections.actionSelectLocationFragmentToSaveReminderFragment())
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
            checkAll3LocationPermissions()
            enableMyLocation()
            setHasOptionsMenu(true)
            setDisplayHomeAsUpEnabled(true)
            setPoiClick(googleMap)
            setMapStyle(googleMap)

        }
        return binding.root
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

    private fun passPoiInfoToViewModel(latAndLng: LatLng, currentPOI: PointOfInterest, name: String) {
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

    // Checks that users have given permission
    private fun isPermissionGranted() : Boolean {
        return context?.let {
            ContextCompat.checkSelfPermission(it, Manifest.permission.ACCESS_FINE_LOCATION)
        } === PackageManager.PERMISSION_GRANTED
    }

    // Checks if users have given their location and sets location enabled if so.
    private fun enableMyLocation() {
        if (isPermissionGranted()) {
            if (context?.let {
                    ActivityCompat.checkSelfPermission(it, Manifest.permission.ACCESS_FINE_LOCATION)
                } != PackageManager.PERMISSION_GRANTED && context?.let {
                    ActivityCompat.checkSelfPermission(it, Manifest.permission.ACCESS_COARSE_LOCATION)
                } != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            googleMap.isMyLocationEnabled = true
        }
        else {
            Toast.makeText(context, "App needs permissions to run!", Toast.LENGTH_LONG).show()
            ActivityCompat.requestPermissions(
                context as Activity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        }
    }

    private fun zoomOnMyLocation() {
        val locationManager = getSystemService(requireContext(), LocationManager::class.java)
        val criteria = Criteria()

        if (context?.let {
                ActivityCompat.checkSelfPermission(it, Manifest.permission.ACCESS_FINE_LOCATION)
            } != PackageManager.PERMISSION_GRANTED && context?.let {
                ActivityCompat.checkSelfPermission(it, Manifest.permission.ACCESS_COARSE_LOCATION)
            } != PackageManager.PERMISSION_GRANTED
        ) {
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
                .target(LatLng(location.latitude, location.longitude)) // Sets the center of the map to location user
                .zoom(17f)    // Sets the zoom
                .bearing(90f) // Sets the orientation of the camera to east
                .tilt(40f)    // Sets the tilt of the camera to 40 degrees
                .build()      // Creates a CameraPosition from the builder
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        }
    }

    /**
     *  Checking Coarse Location, Fine Location, Background Location AND turned on location services
     */
    private fun checkAll3LocationPermissions() {
        if (foregroundAndBackgroundLocationPermissionApproved()) {
            checkDeviceLocationSettingsAndStartMaps()
        } else {
            requestForegroundAndBackgroundLocationPermissions()
        }
    }

    /**
     *  Uses the Location Client to check the current state of location settings, and gives the user
     *  the opportunity to turn on location services within our app.
     */
    private fun checkDeviceLocationSettingsAndStartMaps(resolve:Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = activity?.let { LocationServices.getSettingsClient(it) }
        // Checks if the location is enabled on the phone
        val locationSettingsResponseTask = settingsClient?.checkLocationSettings(builder.build())
        locationSettingsResponseTask?.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve){
                try {
                    startIntentSenderForResult(exception.resolution.intentSender,
                        REQUEST_TURN_DEVICE_LOCATION_ON,
                        null, 0, 0, 0, null)
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d("dra", "Error getting location settings resolution: " + sendEx.message)
                }
            } else {
                // here we display a dialog that says that location should be enabled
                this.view?.let {
                    Snackbar.make(it, R.string.location_required_error, Snackbar.LENGTH_INDEFINITE)
                        .setAction(android.R.string.ok) {
                            checkDeviceLocationSettingsAndStartMaps()
                        }.show()
                }
            }
        }
        locationSettingsResponseTask?.addOnCompleteListener {
            if ( it.isSuccessful ) {
                Toast.makeText(context, "Location permission granted!", Toast.LENGTH_SHORT).show()
                // If my location does not activate that means it's not really granted!!
                enableMyLocation()
                zoomOnMyLocation()
            }
        }
    }

    /**
     *  Determines whether the app has the appropriate permissions across Android 10+ and all other
     *  Android versions.
     */
    @TargetApi(29)
    private fun foregroundAndBackgroundLocationPermissionApproved(): Boolean {
        val foregroundLocationApproved = (
                PackageManager.PERMISSION_GRANTED == context?.let {
                            ActivityCompat.checkSelfPermission(it, Manifest.permission.ACCESS_FINE_LOCATION)
                        })
        val backgroundPermissionApproved =
            if (runningQOrLater) {
                PackageManager.PERMISSION_GRANTED ==
                        context?.let {
                            ActivityCompat.checkSelfPermission(it, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                        }
            } else {
                true    // return true if the device is running less then Q( where you don't need this permission)
            }
        return foregroundLocationApproved && backgroundPermissionApproved
    }

    /**
     *  Requests ACCESS_FINE_LOCATION and (on Android 10+ (Q) ACCESS_BACKGROUND_LOCATION.
     */
    @TargetApi(29 )
    private fun requestForegroundAndBackgroundLocationPermissions() {
        if (foregroundAndBackgroundLocationPermissionApproved())
            return
        var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        val resultCode = when {
            runningQOrLater -> {
                permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
            }
            else -> REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        }
        Log.d("dra", "Request foreground only location permission")
        activity?.let {
            ActivityCompat.requestPermissions(it, permissionsArray, resultCode)
        }
    }

}




// For dropping a marker at a point on the Map
//            val sydney = LatLng(-34.00, 151.05)
//            googleMap.addMarker(
//                MarkerOptions().position(sydney).title("Marker Title").snippet("Marker Description")
//            )

// For zooming automatically to the location of the marker
//            val cameraPosition = CameraPosition.Builder().target(sydney).zoom(12f).build()
//            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))

//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        binding.lifecycleOwner = this
//
//        context?.let { MapsInitializer.initialize(it) }
//        setHasOptionsMenu(true)
//        setDisplayHomeAsUpEnabled(true)
//
//        mapView = binding.map
//        mapView.onCreate(savedInstanceState)
//        mapView.getMapAsync(this)
//    }
//
//    override fun onMapReady(googleMap: GoogleMap) {
//
//        googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
//        map = googleMap
//        setPoiClick(map)
//        setMapStyle(map)
//
//        /**
//         * Inside this functions we check for permissions
//         */
//        enableMyLocation()
//        zoomOnMyLocation()
//
//        //These coordinates represent the lattitude and longitude of the Googleplex.
//        val latitude = 37.422160
//        val longitude = -122.084270
//        val zoomLevel = 15f
//
//        val homeLatLng = LatLng(latitude, longitude)
//        map.moveCamera(CameraUpdateFactory.newLatLngZoom(homeLatLng, zoomLevel))
//        map.addMarker(MarkerOptions().position(homeLatLng))
//    }

//    private fun setMapLongClick(map: GoogleMap) {
//        map.setOnMapLongClickListener { latLng ->
//            // A Snippet is Additional text that's displayed below the title.
//            val snippet = String.format(
//                Locale.getDefault(),
//                "Lat: %1$.5f, Long: %2$.5f",
//                latLng.latitude,
//                latLng.longitude
//            )
//            val poi = map.addMarker(
//                MarkerOptions()
//                    .position(latLng)
//                    .title(getString(R.string.dropped_pin))
//                    .snippet(snippet)
//                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
//            )
//            passPoiInfoToViewModel(latLng, "Dropped Pin", poi)
//        }
//    }