package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.app.Activity
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
import androidx.navigation.fragment.navArgs
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationServices
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentUpdateOrDeleteReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.geofence.GeofenceTransitionsJobIntentService
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class FragmentUpdateOrDelete : BaseFragment() {
    private val navigationArgs: FragmentUpdateOrDeleteArgs by navArgs()
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentUpdateOrDeleteReminderBinding

    // I add a geofencing client so I can update the geofence
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        intent.action = GeofenceTransitionsJobIntentService.ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
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
        // TO DO - check if this works for real!!
        val reminderData = navigationArgs.reminderItem
        binding.lifecycleOwner = this

        // Set the actual text of the EditText fields
        binding.reminderTitle.setText(reminderData.title)
        binding.reminderDescription.setText(reminderData.description)

        // takes you to the map fragment to select location
        binding.selectLocation.setOnClickListener {
            // Remove old geofence - I do this here because this is the moment the user wants to CHANGE the location.
            removeGeofenceById(reminderData.id)

            _viewModel.navigationCommand.value = NavigationCommand
                .To(FragmentUpdateOrDeleteDirections.actionFragmentUpdateOrDeleteToSelectLocationFragment())
        }

        // these are the fields that were received from the reminders List - I should take them from the newly created entries
        binding.updateReminder.setOnClickListener {
            // Code to handle update
            val title = binding.reminderTitle.text.toString()
            val description = binding.reminderDescription.text.toString()
            val location =
                _viewModel.reminderSelectedLocationStr.value        // These values are saved here by the SelectLocationFragment
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value
            val id = reminderData.id

            if (title.isNullOrEmpty()) {
                Toast.makeText(
                    context,
                    getString(R.string.please_fill_out_the_title),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                // The geofence was removed when user clicks on new location button - intentional behaviour
                // Otherwise it should remain the same!

                // Get geofence from user input and add geofence
                if (latitude != null && longitude != null) {
                    // Add new geofence
                    checkPermissionsAndAddGeofence(id, latitude, longitude)
                }
                // Create new reminder data item
                val newReminderData =
                    ReminderDataItem(title, description, location, latitude, longitude, id)

                // Check and update the reminder
                _viewModel.validateAndUpdateReminder(newReminderData)

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

    // This class is identical with the one in Save Reminder Fragment
    private val PERMISSION_REQUEST_FINE_LOCATION = 123
    private fun checkPermissionsAndAddGeofence(id: String, latitude: Double, longitude: Double) {
        if (context?.let {
                ActivityCompat.checkSelfPermission(it, Manifest.permission.ACCESS_FINE_LOCATION)
            } != PackageManager.PERMISSION_GRANTED
        ) {
            // Request the ACCESS_FINE_LOCATION permission
            ActivityCompat.requestPermissions(
                context as Activity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSION_REQUEST_FINE_LOCATION
            )
            return
        }

        // Build the geofence in the viewModel- there is no reason for it to be in the Fragment
        val geofencingRequest = _viewModel.geofencingRequestBuilder(id, latitude, longitude)

        // We add a single geofence to the geofencing client
        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
            addOnCompleteListener {
                addOnSuccessListener {
                    Toast.makeText(context, getString(R.string.geofence_added), Toast.LENGTH_SHORT)
                        .show()
                }
                addOnFailureListener {
                    Toast.makeText(
                        context,
                        getString(R.string.error_in_adding_geofence),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}