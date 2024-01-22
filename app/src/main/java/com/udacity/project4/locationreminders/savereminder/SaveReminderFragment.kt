package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.authentication.AuthenticationActivity.Companion.TAG
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.geofence.GeofenceTransitionsJobIntentService.Companion.ACTION_GEOFENCE_EVENT
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.UUID

class SaveReminderFragment : BaseFragment() {
    // Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding
    lateinit var reminderData: ReminderDataItem
    var id: String = UUID.randomUUID().toString()

    // Permissions are different for Android 10 and above
    private val runningQOrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    private lateinit var geofencingClient: GeofencingClient

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_MUTABLE
        )    // for the apps that target version 31 and above
    }

    // constants used in the location permission request to identify my requests
    companion object {
        private const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
        private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
        private const val LOCATION_PERMISSION_INDEX = 0
        private const val BACKGROUND_LOCATION_PERMISSION_INDEX = 1
        private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        // I add a geofencing client so I can put all the geofences inside
        geofencingClient = activity?.let { LocationServices.getGeofencingClient(it) }!!

        setDisplayHomeAsUpEnabled(true)
        binding.viewModel = _viewModel
        return binding.root
    }

    // All the permissions for the location services should already be on at this point,
    // so I'm suppressing the necessary permissions for addGeofences()
    @RequiresApi(Build.VERSION_CODES.N)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this

        binding.selectLocation.setOnClickListener {
            _viewModel.saveLocationButtonClickedFromUpdateOrDelete.value = false
            _viewModel.navigationCommand.value = NavigationCommand.To(
                SaveReminderFragmentDirections
                    .actionSaveReminderFragmentToSelectLocationFragment()
            )
        }

        binding.saveReminder.setOnClickListener {

            // Check if first 2 permissions are granted: foreground and background, if not, request them
            if (foregroundAndBackgroundLocationPermissionApproved()) {
                // If first 2 are granted, check if the device's location is on (3rd permission), if not, request it
                // At the end it checks for the 4th permission: notification permission
                checkDeviceLocationSettingsAndStartSavingReminder()
            } else {
                requestForegroundAndBackgroundLocationPermissions()
            }
        }
    }

    // After every permission is granted: 1 foreground, 2 background,3 the device's location is on, and 4. notifications are enabled,
    // Call this at on success at step 5
    private fun validateReminderStartGeofenceAndSaveReminder() {
        val reminderDataItem = ReminderDataItem(
            _viewModel.reminderTitle.value,
            _viewModel.reminderDescription.value,
            _viewModel.reminderSelectedLocationStr.value,
            _viewModel.latitude.value,
            _viewModel.longitude.value,
            id
        )
        if (_viewModel.validateEnteredData(reminderDataItem)) {
            _viewModel.saveReminder(reminderDataItem)
            setupGeofence()
            // Navigate back to the reminders list
            findNavController().navigate(SaveReminderFragmentDirections.actionSaveReminderFragmentToReminderListFragment())
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
        Log.d(TAG, "Request foreground only location permission")
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
    @RequiresApi(Build.VERSION_CODES.N)
    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        Log.d(TAG, "onRequestPermissionResult")
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
            checkDeviceLocationSettingsAndStartSavingReminder()
        }
    }

    /** PERMISSIONS
     * Step 5: Turn device location ON. Having the user grant permissions is only one part of the permissions needed,
     * another thing to check is if the device’s location is on. Check device location settings and
     * START GEOFENCE
     * */
    @RequiresApi(Build.VERSION_CODES.N)
    @SuppressLint("VisibleForTests")
    private fun checkDeviceLocationSettingsAndStartSavingReminder(resolve: Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(this.requireActivity())
        val locationSettingsResponseTask = settingsClient.checkLocationSettings(builder.build())

        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                try {
                    exception.startResolutionForResult(
                        requireActivity(),
                        REQUEST_TURN_DEVICE_LOCATION_ON
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(TAG, "Error getting location settings resolution: " + sendEx.message)
                }
            } else {
                Snackbar.make(
                    this.requireView(),
                    R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettingsAndStartSavingReminder()
                }.show()
            }
            // If the user doesn't turn on the location services, then we display a message
            this.view?.let {
                Snackbar.make(it, R.string.location_required_error, Snackbar.LENGTH_LONG).show()
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                val notificationManager =
                    context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                // Check if notifications NOT are enabled, if not, show a dialogue to the user
                if (!notificationManager.areNotificationsEnabled()) {
                    showNotificationPermissionDialog()
                    println("dra Notifications are NOT enabled and all of the permissions are granted")
                } else {
                    println("dra Notifications are enabled and all of the permissions are granted")
                    validateReminderStartGeofenceAndSaveReminder()
                }
            }
        }
    }

    /**
     * START GEOFENCE - extracted as a separate class so it can be called from the code above:
     * if ( it.isSuccessful ) {setupGeofence()}
     * */
    private fun setupGeofence() {
        val latitude = _viewModel.latitude.value
        val longitude = _viewModel.longitude.value

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
    @RequiresApi(Build.VERSION_CODES.N)
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
            // If the user has turned on the device's location, then we can start the geofence
            checkDeviceLocationSettingsAndStartSavingReminder(false)
            // make toast
            Toast.makeText(
                context, "An answer was given! - dragos", Toast.LENGTH_LONG
            ).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun showNotificationPermissionDialog() {
        // Inflate the custom layout
        val dialogView =
            LayoutInflater.from(requireContext()).inflate(R.layout.notification_dialogue, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        // Set up the buttons
        dialogView.findViewById<Button>(R.id.enableButton).setOnClickListener {
            // Enable notifications
            val intent = Intent()
            intent.action = "android.settings.APP_NOTIFICATION_SETTINGS"
            intent.putExtra("android.provider.extra.APP_PACKAGE", requireContext().packageName)
            startActivity(intent)
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.notNowButton).setOnClickListener {
            // User refuses to enable notifications
            dialog.dismiss()
            // Toast message explaining that notifications are essential to the app's functionality
            Toast.makeText(
                context,
                getString(R.string.notification_permission_toast),
                Toast.LENGTH_LONG
            ).show()
        }

        dialog.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }
}


//_____________________________________________________________________________________________
//Permissions can be denied in a few ways:
//If the grantResults array is empty, then the interaction was interrupted and the permission request was cancelled.
//If the grantResults array’s value at the LOCATION_PERMISSION_INDEX has a PERMISSION_DENIED it means that the user denied foreground permissions.
//If the request code equals REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE and the BACKGROUND_LOCATION_PERMISSION_INDEX
//is denied it means that the device is running API 29 or above and that background permissions were denied.