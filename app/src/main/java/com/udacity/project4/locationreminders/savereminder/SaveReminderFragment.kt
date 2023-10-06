package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.geofence.GeofenceTransitionsJobIntentService.Companion.ACTION_GEOFENCE_EVENT
import com.udacity.project4.locationreminders.geofence.GeofencingConstants
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.UUID

class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding
    lateinit var reminderData: ReminderDataItem

    private lateinit var geofencingClient: GeofencingClient
//    val geofenceList = mutableListOf<Geofence>()

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private val PERMISSION_REQUEST_FINE_LOCATION = 123

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
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            _viewModel.navigationCommand.value = NavigationCommand
                .To(SaveReminderFragmentDirections
                    .actionSaveReminderFragmentToSelectLocationFragment())
        }

        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value
            val id = UUID.randomUUID().toString()

            if (latitude != null && longitude != null) {

                if (ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // Request the ACCESS_FINE_LOCATION permission
                    ActivityCompat.requestPermissions(
                        requireActivity(),
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        PERMISSION_REQUEST_FINE_LOCATION
                    )
                    return@setOnClickListener
                }

                val geofence = Geofence.Builder()
                    .setRequestId(id)
                    .setCircularRegion(
                        latitude,
                        longitude,
                        GeofencingConstants.GEOFENCE_RADIUS_IN_METERS
                    )
                    .setExpirationDuration(GeofencingConstants.GEOFENCE_EXPIRATION_IN_MILLISECONDS)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                    .build()

                val geofencingRequest = GeofencingRequest.Builder()
                    .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                    .addGeofence(geofence)
                    .build()

                // we add a single geofence
                geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
                    addOnCompleteListener {
                        addOnSuccessListener {
                            Toast.makeText(context, "Area added!", Toast.LENGTH_SHORT).show()
                        }
                        addOnFailureListener {
                            Toast.makeText(context, "Error in adding geofence", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }

                reminderData =
                    ReminderDataItem(title, description, location, latitude, longitude, id)
                _viewModel.validateAndSaveReminder(reminderData)
                // Going back to the list to see the reminder added
                findNavController().navigate(SaveReminderFragmentDirections.actionSaveReminderFragmentToReminderListFragment())
            }
        }
    }

//    If user doesn't have the permissions then the geofence can't be added
    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_FINE_LOCATION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, add the geofence
                // You can call the code to add the geofence here
            } else {
                // Permission denied, show a message or take appropriate action
                Toast.makeText(
                    requireContext(),
                    "Permission denied. Unable to add geofence.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }
}
