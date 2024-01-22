package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

    // Testing implementation to the RemindersLocalRepository.kt
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: RemindersDatabase
    private lateinit var remindersLocalRepository: RemindersLocalRepository

    @Before
    fun initDb() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).allowMainThreadQueries()
            .build()     // For testing purposes Important - if you don't do this an error will pop up because of using main dispatcher
        remindersLocalRepository = RemindersLocalRepository(
            database.reminderDao(),
            Dispatchers.Main
        )
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun saveReminder_getReminderById() = runTest {
        // GIVEN - save a reminder
        val reminder = ReminderDTO(
            "title",
            "description",
            "location",
            1.0,
            2.0
        )
        remindersLocalRepository.saveReminder(reminder)

        // WHEN - get a reminder by id
        val result = remindersLocalRepository.getReminder(reminder.id)

        // THEN - the result is the same as the saved reminder
        assertThat(result is Result.Success, `is`(true))
        result as Result.Success
        assertThat(result.data.title, `is`(reminder.title))
        assertThat(result.data.description, `is`(reminder.description))
        assertThat(result.data.location, `is`(reminder.location))
        assertThat(result.data.latitude, `is`(reminder.latitude))
        assertThat(result.data.longitude, `is`(reminder.longitude))
    }

    @Test
    fun dataNotAvailable_getReminderById() = runBlocking {
        // GIVEN - save a reminder
        val reminder = ReminderDTO(
            "title",
            "description",
            "location",
            1.0,
            2.0
        )
        remindersLocalRepository.saveReminder(reminder)

        // WHEN - get a reminder by id
        val result = remindersLocalRepository.getReminder("")

        // THEN - the result is an error
        assertTrue(result is Result.Error)
    }

    @Test
    fun deleteAllReminders_getReminders() = runBlocking {
        // GIVEN - save a reminder
        val reminder = ReminderDTO(
            "title",
            "description",
            "location",
            1.0,
            2.0
        )
        remindersLocalRepository.saveReminder(reminder)

        // WHEN - delete all reminders
        remindersLocalRepository.deleteAllReminders()

        // THEN - the result of getReminders is an empty list
        val result = remindersLocalRepository.getReminders()
        assertTrue(result == Result.Success(emptyList<ReminderDTO>()))
    }

    @Test
    fun delete_one_reminder() = runBlocking {
        // GIVEN - save a reminder
        val reminder = ReminderDTO(
            "title",
            "description",
            "location",
            1.0,
            2.0
        )
        remindersLocalRepository.saveReminder(reminder)

        // WHEN - delete a reminder
        remindersLocalRepository.deleteReminder(reminder.id)

        // THEN - the result of getReminders is an empty list
        val result = remindersLocalRepository.getReminders()
        assertTrue(result == Result.Success(emptyList<ReminderDTO>()))
    }
}
