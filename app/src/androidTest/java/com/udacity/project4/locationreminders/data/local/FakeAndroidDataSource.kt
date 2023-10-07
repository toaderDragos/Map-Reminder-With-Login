package com.udacity.project4.locationreminders.data.local

import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

class FakeAndroidDataSource (var reminders: MutableList<ReminderDTO> = mutableListOf()) :
    ReminderDataSource {

    //   Create a fake data source to act as a double to the real data source
    private var returnError = false

    // This method is used to set a boolean value to return an error.
    // Also, it is used to test the error message.
    fun setReturnError(value: Boolean) {
        returnError = value
    }

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        return if (returnError) {
            Result.Error("Error in testing FakeDataSource in Test Folder: Can't load reminders!")
        } else {
            Result.Success(reminders.toList())
        }
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        //  Return the reminder with the id
        return if (returnError) {
            Result.Error("Error in testing FakeDataSource in Test Folder: Reminder not found!")
        } else {
            val reminder = reminders.find { reminder -> reminder.id == id }
            return if (reminder == null)
                Result.Error("Error in testing FakeDataSource in Test Folder: Reminder not found because is null!")
            else
                Result.Success(reminder)
        }
    }

    override suspend fun deleteAllReminders() {
        reminders.clear()
    }
}