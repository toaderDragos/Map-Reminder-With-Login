package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource(var reminders: MutableList<ReminderDTO> = mutableListOf()) : ReminderDataSource {

//   Create a fake data source to act as a double to the real data source
    private var returnError = false

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        // "Return the reminders"
        return if (returnError) {
            Result.Error("Error in testing FakeDataSource in Test Folder: Can't load reminders!")
        } else {
            Result.Success(reminders.toList())
        }
    }

    fun setReturnError(value: Boolean) {
        returnError = value
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        //"return the reminder with the id"
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

    override suspend fun deleteReminder(id: String) {
        reminders.removeIf { reminder -> reminder.id == id }
    }

    override suspend fun updateReminder(reminder: ReminderDTO) {
        // Find the index of the reminder to be updated
        val index = reminders.indexOfFirst { it.id == reminder.id }
        // Update the reminder
        reminders[index] = reminder
    }


}