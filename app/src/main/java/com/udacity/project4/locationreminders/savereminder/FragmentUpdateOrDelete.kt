package com.udacity.project4.locationreminders.savereminder

import android.Manifest
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
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.UUID

class FragmentUpdateOrDelete : BaseFragment() {
    private val navigationArgs: FragmentUpdateOrDeleteArgs by navArgs()
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentUpdateOrDeleteReminderBinding
    // lateinit var reminderData: ReminderDataItem

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

        // Let's pre - populate this fragment with the data from the reminder that was selected
//        binding.reminderTitle.hint = reminderData.title.toString()
//        binding.reminderDescription.hint = reminderData.description.toString()

        // takes you to the map fragment to select location
        binding.selectLocation.setOnClickListener {
            // Remove old geofence - I do this here because this is the moment the user wants to change the location.
            removeGeofenceById(reminderData.id)

            _viewModel.navigationCommand.value = NavigationCommand
                .To(
                    FragmentUpdateOrDeleteDirections
                        .actionFragmentUpdateOrDeleteToSelectLocationFragment()
                )
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
            val id = UUID.randomUUID().toString()

            if (title.isNullOrEmpty()) {
                Toast.makeText(
                    context,
                    getString(R.string.please_fill_out_the_title),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                // Get geofence from user input and add geofence
                if (latitude != null && longitude != null) {
                    SaveReminderFragment().checkPermissionsAndAddGeofence(id, latitude, longitude)
                }
                // Create new reminder data item
                val newReminderData =
                    ReminderDataItem(title, description, location, latitude, longitude, id)
                // Add reminder to local db
                _viewModel.updateReminder(newReminderData)

                // Navigate back to the reminders list
                findNavController().navigate(FragmentUpdateOrDeleteDirections.actionFragmentUpdateOrDeleteToReminderListFragment())
            }
        }

        binding.deleteReminder.setOnClickListener {
            _viewModel.deleteReminder(reminderData.id)
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
                // Handle success scenario (e.g., updating UI or showing a toast)
                Toast.makeText(context, getString(R.string.geofence_removed), Toast.LENGTH_SHORT)
                    .show()
            }
            addOnFailureListener {
                // Handle failure scenario
                Toast.makeText(
                    context,
                    getString(R.string.error_in_removing_geofence),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}