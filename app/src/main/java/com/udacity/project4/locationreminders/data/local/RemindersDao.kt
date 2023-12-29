package com.udacity.project4.locationreminders.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.udacity.project4.locationreminders.data.dto.ReminderDTO

/** Complete until here - Dragos memo
 * Data Access Object for the reminders table. What I have modified - I took out the suspend from here (it will still be present)
 */
@Dao
interface RemindersDao {
    /**
     * @return all reminders.
     */
    @Query("SELECT * FROM reminders")
    fun getReminders(): List<ReminderDTO>

    /**
     * @param reminderId the id of the reminder
     * @return the reminder object with the reminderId
     */
    @Query("SELECT * FROM reminders where entry_id = :reminderId")
    fun getReminderById(reminderId: String): ReminderDTO?

    /**
     * Insert a reminder in the database. If the reminder already exists, replace it.
     * @param reminder the reminder to be inserted.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveReminder(reminder: ReminderDTO)

    /**
     * Delete all reminders.
     */
    @Query("DELETE FROM reminders")
    fun deleteAllReminders()


    /**
     * Delete a reminder by id.
     * @return the number of reminders deleted. This should always be 1.
     */
    @Query("DELETE FROM reminders WHERE entry_id = :reminderId")
    fun deleteReminder(reminderId: String): Int

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(reminder: ReminderDTO)

}