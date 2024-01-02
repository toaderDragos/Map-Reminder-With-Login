package com.udacity.project4.locationreminders.savereminder

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.maps.model.PointOfInterest
import com.udacity.project4.R
import com.udacity.project4.base.BaseViewModel
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.geofence.GeofencingConstants
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import kotlinx.coroutines.launch

class SaveReminderViewModel(val app: Application, val dataSource: ReminderDataSource) :
    BaseViewModel(app) {

    val reminderTitle = MutableLiveData<String?>()
    val reminderDescription = MutableLiveData<String?>()
    val reminderSelectedLocationStr = MutableLiveData<String?>()
    val selectedPOI = MutableLiveData<PointOfInterest?>()
    val latitude = MutableLiveData<Double?>()
    val longitude = MutableLiveData<Double?>()

    val saveLocationButtonClickedFromUpdateOrDelete = MutableLiveData<Boolean>()

    val updatedReminderSelectedLocationStr = MutableLiveData<String?>()
    val updatedLatitude = MutableLiveData<Double?>()
    val updatedLongitude = MutableLiveData<Double?>()


    /**
     * Clear the live data objects to start fresh next time the view model gets called
     */
    fun onClear() {
        reminderTitle.value = null
        reminderDescription.value = null
        reminderSelectedLocationStr.value = null
        selectedPOI.value = null
        latitude.value = null
        longitude.value = null
    }

    /**
     * Validate the entered data then saves the reminder data to the DataSource
     */
    fun validateAndSaveReminder(reminderData: ReminderDataItem) {
        if (validateEnteredData(reminderData)) {
            saveReminder(reminderData)
        }
    }

    /**
     * Save the reminder to the data source
     */
    fun saveReminder(reminderData: ReminderDataItem) {
        showLoading.value = true
        viewModelScope.launch {
            dataSource.saveReminder(
                ReminderDTO(
                    reminderData.title,
                    reminderData.description,
                    reminderData.location,
                    reminderData.latitude,
                    reminderData.longitude,
                    reminderData.id
                )
            )
            showLoading.value = false
            showToast.value = app.getString(R.string.reminder_saved)
            // Deleting the livedata objects
            onClear()
        }
    }

    /**
     * Validate the entered data and show error to the user if there's any invalid data
     */
    fun validateEnteredData(reminderData: ReminderDataItem): Boolean {
        if (reminderData.title.isNullOrEmpty()) {
            showSnackBarInt.value = R.string.err_enter_title
            return false
        }

        if (reminderData.location.isNullOrEmpty()) {
            showSnackBarInt.value = R.string.err_select_location
            return false
        }
        return true
    }

    fun geofencingRequestBuilder(
        id: String,
        latitude: Double,
        longitude: Double
    ): GeofencingRequest? {
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
        return geofencingRequest
    }

    /**
     * Validate the entered data then updates the reminder data to the DataSource
     */
    fun validateAndUpdateReminder(reminderData: ReminderDataItem) {
        if (validateEnteredData(reminderData)) {
            updateReminder(reminderData)
        }
    }

    /**
     * The New Geofence gets updated in the fragment. The previous Geofence is deleted in the same place.
     * Resolve issue with the showToast.value() not working.
     */
    private fun updateReminder(reminderData: ReminderDataItem) {
        viewModelScope.launch {
            val result = dataSource.getReminder(reminderData.id)
            if (result is Result.Success<ReminderDTO>) {
                val reminderToUpdate = result.data
                reminderToUpdate.let {
                    it.title = reminderData.title
                    it.description = reminderData.description
                    it.location = reminderData.location
                    it.latitude = reminderData.latitude
                    it.longitude = reminderData.longitude

                    // update the reminder in the DB
                    dataSource.updateReminder(it)

                    // Handle UI updates or navigation
                    showLoading.value = false
                    showToast.value = app.getString(R.string.reminder_updated)
//                    navigationCommand.value = NavigationCommand.Back
                }
            }
        }
    }

    /**
     * This delete reminder function deletes the entry in the room database.
     * The associated Geofence was deleted in the Fragment.
     */

    fun deleteReminder(id: String) {
        viewModelScope.launch {
            dataSource.deleteReminder(id)
            // Handle UI updates or navigation
            showLoading.value = false
            showToast.value = app.getString(R.string.reminder_deleted)
            // navigationCommand.value = NavigationCommand.Back
        }
    }



}