package com.udacity.project4.locationreminders.geofence

import android.content.Context
import android.content.Intent
import android.text.TextUtils
import android.util.Log
import androidx.core.app.JobIntentService
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject
import kotlin.coroutines.CoroutineContext
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import org.koin.android.ext.android.get

abstract class GeofenceTransitionsJobIntentService : JobIntentService(), CoroutineScope, List<Geofence> {
    private var coroutineJob: Job = Job()
    private val TAG = "GeofenceService"
    private lateinit var repository: ReminderDataSource
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + coroutineJob


    companion object {
        private const val JOB_ID = 573

        //        DONE: call this to start the JobIntentService to handle the geofencing transition events
        fun enqueueWork(context: Context, intent: Intent) {
            enqueueWork(
                    context,
                    GeofenceTransitionsJobIntentService::class.java, JOB_ID,
                    intent
            )
        }

    }

    override fun onHandleWork(intent: Intent) {
        //DONE: handle the geofencing transition events and
        // send a notification to the user when he enters the geofence area
        //DONE call @sendNotification

        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        val geofenceList: List<Geofence> =
                geofencingEvent.triggeringGeofences
        sendNotification(
            geofenceList, ReminderDataItem(
                "",
                "",
                "",
                0.0,
                0.0,
                ""
            )
        )
    }



    //DONE: get the request id of the current geofence
    private fun sendNotification(Geofences: List<Geofence>, reminderDataItem: ReminderDataItem) {
        //val remindersRepository: ReminderDataSource by inject()
        val requestId = when {
            Geofences.isNotEmpty() ->
            {
                Log.d(TAG, "sendNotification: " + Geofences[0].requestId)
                Geofences[0].requestId
            }

            else -> {
                Log.e(TAG, "No Geofence Trigger Found !")
                return
            }
        }

        if(TextUtils.isEmpty(requestId)) return

        repository = get()
        //Get the local repository instance
        val remindersRepository: ReminderDataSource by inject()
        //val remindersLocalRepository: RemindersLocalRepository by inject()
//        Interaction to the repository has to be through a coroutine scope
        CoroutineScope(coroutineContext).launch(SupervisorJob()) {
            //get the reminder with the request id
            //val result = remindersLocalRepository.getReminder(requestId)
            val result = remindersRepository.getReminder(requestId)
            if (result is Result.Success<ReminderDTO>) {
                val reminderDTO = result.data
                //send a notification to the user with the reminder details
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