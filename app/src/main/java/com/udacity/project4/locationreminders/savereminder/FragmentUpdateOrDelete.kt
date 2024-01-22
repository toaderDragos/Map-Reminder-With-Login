package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.authentication.AuthenticationActivity
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentUpdateOrDeleteReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.geofence.GeofenceTransitionsJobIntentService
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

/**
 * Updating a reminder. I use the same fragment as for saving a reminder, but I populate the fields with the data from the reminder I want to update.
 * I also use the same view model as for saving a reminder. I use the same layout as for saving a reminder.
 * Intentionally the user can save a reminder even though the location service is not turned on.
 * The user has to be able to gracefully use the app even without the location service turned on -
 * he can use it as a normal reminder app if he does not want to give the permissions
 */

class FragmentUpdateOrDelete : BaseFragment() {
    private val navigationArgs: FragmentUpdateOrDeleteArgs by navArgs()
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentUpdateOrDeleteReminderBinding
    private var location: String? = null
    private var latitude: Double? = null
    private var longitude: Double? = null
    lateinit var id: String

    // Permissions are different for Android 10 and above
    private val runningQOrLater = android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.Q

    // constants used in the location permission request to identify my requests
    companion object {
        private const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 103
        private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 104
        private const val LOCATION_PERMISSION_INDEX = 0
        private const val BACKGROUND_LOCATION_PERMISSION_INDEX = 1
        private const val PERMISSION_REQUEST_FINE_LOCATION = 123
        private const val REQUEST_TURN_DEVICE_LOCATION_ON = 55
    }

    // I add a geofencing client so I can update the geofence
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        intent.action = GeofenceTransitionsJobIntentService.ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_MUTABLE)
    }

    private lateinit var geofencingClient: GeofencingClient

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_update_or_delete_reminder,
            container,
            false
        )

        // I add a geofencing client so I can put all the geofences inside
        geofencingClient = activity?.let { LocationServices.getGeofencingClient(it) }!!

        setDisplayHomeAsUpEnabled(true)
        binding.viewModel = _viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Populate UI with existing reminder data
        val reminderData = navigationArgs.reminderItem
        binding.lifecycleOwner = this

        // Set the actual text of the EditText fields and location
        binding.reminderTitle.setText(reminderData.title)
        binding.reminderDescription.setText(reminderData.description)

        // I set the location from the viewModel- this binding is already done in the XML
        // Saving the location in a variable and then assigning it to the viewModel onStart
        location = reminderData.location.toString()

        /**
         * Takes you to the map fragment so you can save the location
         * It removes the old geofence and saves the information through the viewModel.
         * The live datas are updated in the viewModel and then they are assigned here. They are destined for this purpose only.
         */
        binding.selectLocation.setOnClickListener {
            // Remove old geofence - I do this here because this is the moment the user wants to CHANGE the location.
            removeGeofenceById(reminderData.id)
            _viewModel.saveLocationButtonClickedFromUpdateOrDelete.value = true
            _viewModel.navigationCommand.value = NavigationCommand
                .To(FragmentUpdateOrDeleteDirections.actionFragmentUpdateOrDeleteToSelectLocationFragment())
        }

        // Let's take the info for location directly from viewmodel - it's already there
        binding.updateReminder.setOnClickListener {

            val title = binding.reminderTitle.text.toString()
            val description = binding.reminderDescription.text.toString()
            location = _viewModel.updatedReminderSelectedLocationStr.value
            latitude = _viewModel.updatedLatitude.value
            longitude = _viewModel.updatedLongitude.value

            // I need the id to update the geofence on updateGeofence
            id = reminderData.id

            // Create new reminder data item
            val newReminderData =
                ReminderDataItem(title, description, location, latitude, longitude, id)

            // Check if all elements are filled out and Add reminder to local db
            if (_viewModel.validateEnteredData(newReminderData)) {
                _viewModel.updateReminder(newReminderData)

                // Check if permissions are granted, if not, request them
                if (foregroundAndBackgroundLocationPermissionApproved()) {
                    checkDeviceLocationSettingsAndStartGeofence()
                } else {
                    requestForegroundAndBackgroundLocationPermissions()
                }
                // Navigate back to the reminders list
                findNavController().navigate(FragmentUpdateOrDeleteDirections.actionFragmentUpdateOrDeleteToReminderListFragment())
            }
        }

        binding.deleteReminder.setOnClickListener {
            // Remove the saved reminder
            _viewModel.deleteReminder(reminderData.id)

            // Remove the geofence
            removeGeofenceById(reminderData.id)
            findNavController().navigate(FragmentUpdateOrDeleteDirections.actionFragmentUpdateOrDeleteToReminderListFragment())
        }
    }

    override fun onResume() {
        super.onResume()
        // I set the location from the viewmodel- this binding is already done in the XML
        binding.selectedLocation.text = _viewModel.updatedReminderSelectedLocationStr.value
        val log1 = _viewModel.updatedReminderSelectedLocationStr.value
        println("dra Updated location in returning from Save Location is: $log1")
    }

    private fun removeGeofenceById(geofenceId: String) {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Handle the case where you don't have permission
            Toast.makeText(
                requireContext(),
                "Unable to add geofence. Please add location permission in settings",
                Toast.LENGTH_SHORT
            )
                .show()
            return
        }

        geofencingClient.removeGeofences(listOf(geofenceId)).run {
            addOnSuccessListener {
                Toast.makeText(context, getString(R.string.geofence_removed), Toast.LENGTH_SHORT)
                    .show()
            }
            addOnFailureListener {
                Toast.makeText(
                    context,
                    getString(R.string.error_in_removing_geofence),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /** PERMISSIONS
     * Step 1: Added permissions to AndroidManifest
     * Step 2: Have method that checks for permissions (below)
     * */

    @TargetApi(29)
    private fun foregroundAndBackgroundLocationPermissionApproved(): Boolean {
        val foregroundLocationApproved = (
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ))
        val backgroundPermissionApproved =
            if (runningQOrLater) {
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        )
            } else {
                true
            }
        return foregroundLocationApproved && backgroundPermissionApproved
    }

    /** PERMISSIONS
     * Step 3: Request permissions
     * If they are approved, then just return, otherwise request them
     * */

    @TargetApi(29)
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
        Log.d(AuthenticationActivity.TAG, "Request foreground only location permission")
        ActivityCompat.requestPermissions(
            requireActivity(),
            permissionsArray,
            resultCode
        )
    }

    /** PERMISSIONS
     * Step 4: Handle permissions. After the user responds to the permission request at step 3, we check the result
     * If they are approved, then just return, otherwise request them
     * */
    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        Log.d(AuthenticationActivity.TAG, "onRequestPermissionResult")
        // See below what this does 1*
        if (
            grantResults.isEmpty() ||
            grantResults[LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED ||
            (requestCode == REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE &&
                    grantResults[BACKGROUND_LOCATION_PERMISSION_INDEX] ==
                    PackageManager.PERMISSION_DENIED)
        ) {
            // Explain to the user that the app has little functionality without the permissions
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
            checkDeviceLocationSettingsAndStartGeofence()
        }
    }

    /** PERMISSIONS
     * Step 5: Turn device location ON. Having the user grant permissions is only one part of the permissions needed,
     * another thing to check is if the device’s location is on. Check device location settings and
     * START GEOFENCE
     * */
    @SuppressLint("VisibleForTests")
    private fun checkDeviceLocationSettingsAndStartGeofence(resolve: Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask = settingsClient.checkLocationSettings(builder.build())

        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                try {
                    exception.startResolutionForResult(
                        requireActivity(),
                        REQUEST_TURN_DEVICE_LOCATION_ON
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(
                        AuthenticationActivity.TAG,
                        "Error getting location settings resolution: " + sendEx.message
                    )
                }
            } else {
                Snackbar.make(
                    this.requireView(),
                    R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettingsAndStartGeofence()
                }.show()
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                // Remove old geofence and put a new one
                removeGeofenceById(id)
                updateGeofence()
            }
        }
    }

    /**
     * START GEOFENCE - extracted as a separate class so it can be called from the code above:
     * if ( it.isSuccessful ) {setupGeofence()}
     * */

    private fun updateGeofence() {
        val latitude = _viewModel.updatedLatitude.value
        val longitude = _viewModel.updatedLongitude.value

        if ((longitude != null) && (latitude != null)) {
            val geofencingRequest = _viewModel.geofencingRequestBuilder(id, latitude, longitude)
            // This is a mandatory re-check for permissions requested by geofencingClient
            if (context?.let {
                    ActivityCompat.checkSelfPermission(it, Manifest.permission.ACCESS_FINE_LOCATION)
                } != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
                addOnCompleteListener {
                    addOnSuccessListener {
                        Toast.makeText(
                            context,
                            getString(R.string.geofence_added),
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    }
                    addOnFailureListener {
                        Toast.makeText(
                            context,
                            getString(R.string.error_in_adding_geofence),
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    }
                }
            }
        }
    }

    /** PERMISSIONS
     * Step 6: After the user chooses whether to accept or deny device location permissions,
     * this checks if the user has chosen to accept the permissions. If not, it will ask again.
     * */
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
            checkDeviceLocationSettingsAndStartGeofence(false)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }
}


