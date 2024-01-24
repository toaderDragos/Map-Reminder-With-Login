package com.udacity.project4.locationreminders.geofence

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.JobIntentService
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.sendNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import kotlin.coroutines.CoroutineContext

/**
 * Unmodified from original yet
 */

class GeofenceTransitionsJobIntentService : JobIntentService(), CoroutineScope {

    private var coroutineJob: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + coroutineJob

    companion object {
        private const val JOB_ID = 573
        internal const val ACTION_GEOFENCE_EVENT =
            "RemindersActivity.treasureHunt.action.ACTION_GEOFENCE_EVENT"

        //  Call this to start the JobIntentService to handle the geofencing transition events
        fun enqueueWork(context: Context, intent: Intent) {
            enqueueWork(context, GeofenceTransitionsJobIntentService::class.java, JOB_ID, intent)
        }
    }

    // This was at the onReceive() method in GeoFences Starter file - at the Broadcast Receiver
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onHandleWork(intent: Intent) {
        if (intent.action == ACTION_GEOFENCE_EVENT) {
            val geofencingEvent = GeofencingEvent.fromIntent(intent)

            if (geofencingEvent?.hasError() == true) {
                val errorMessage = errorMessage(applicationContext, geofencingEvent.errorCode)
                Log.e("dra", "Error at geofencing event/ on handle work" + errorMessage)
                return
            }

            // Check if the geofenceTransition type is ENTER - we can have EXIT transition type as well.
            if (geofencingEvent != null) {
                if (geofencingEvent.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                    Log.v("dra", "Geofence has been entered")

                    if (geofencingEvent.triggeringGeofences?.isNotEmpty() == true) {
                        geofencingEvent.triggeringGeofences?.get(0)?.requestId
                        // Pass in the list so that we can find which location was triggered
                        geofencingEvent.triggeringGeofences?.let { sendNotification(it) }
                    } else {
                        Log.e("dra", "No Geofence Trigger Found!")
                        return
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun sendNotification(triggeringGeofences: List<Geofence>) {

        // Get the local repository instance
        val remindersLocalRepository: RemindersLocalRepository by inject()

        // We search through the list for the activated geofence
        for (gfence in triggeringGeofences) {

            // Interaction to the repository has to be through a coroutine scope
            CoroutineScope(coroutineContext).launch(SupervisorJob()) {

                val requestId = gfence.requestId
                // Get the reminder with the request id
                val result = remindersLocalRepository.getReminder(requestId)
                if (result is Result.Success<ReminderDTO>) {
                    val reminderDTO = result.data
                    // send a notification to the user with the reminder details
                    println("dra The notification has started, that means that the geofence was found!")
                    sendNotification(
                        this@GeofenceTransitionsJobIntentService, ReminderDataItem(
                            reminderDTO.title,
                            reminderDTO.description,
                            reminderDTO.location,
                            reminderDTO.latitude,
                            reminderDTO.longitude,
                            reminderDTO.id
                        )
                    )
                }
            }
        }
    }

    /**
     * Returns the error string for a geofencing error code.
     */
    fun errorMessage(context: Context, errorCode: Int): String {
        val resources = context.resources
        return when (errorCode) {
            GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE -> resources.getString(
                R.string.geofence_not_available
            )
            GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES -> resources.getString(
                R.string.geofence_too_many_geofences
            )
            GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS -> resources.getString(
                R.string.geofence_too_many_pending_intents
            )
            else -> resources.getString(R.string.error_happened)
        }
    }

}
