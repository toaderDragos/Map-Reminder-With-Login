package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {

    private lateinit var database: RemindersDatabase

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun initDb() {
        database = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                RemindersDatabase::class.java).build()
    }

    @After
    fun closeDb() = database.close()

    @Test
    fun saveReminderAndGetById() = runTest {
        val reminder = ReminderDTO("Title", "Description", "Location", 0.0, 0.0)
        database.reminderDao().saveReminder(reminder)

        val loaded = database.reminderDao().getReminderById(reminder.id)

        assertThat(loaded as ReminderDTO, notNullValue())
        assertThat(loaded.id, `is`(reminder.id))
        assertThat(loaded.title, `is`(reminder.title))
        assertThat(loaded.description, `is`(reminder.description))
        assertThat(loaded.location, `is`(reminder.location))
        assertThat(loaded.latitude, `is`(reminder.latitude))
        assertThat(loaded.longitude, `is`(reminder.longitude))
    }

    @Test
    fun deleteAllReminders() = runTest {
        // Arrange - create a fake data source
        val reminder = ReminderDTO("Title", "Description", "Location", 0.0, 0.0)

        // Act -  save the fake data into the DAO
        database.reminderDao().saveReminder(reminder)

        // Act -  delete the fake data from the DAO
        database.reminderDao().deleteAllReminders()

        // Assert - check that the data was deleted
        val loaded = database.reminderDao().getReminderById(reminder.id)
        assertThat(loaded, `is`(nullValue()))
    }

    @Test
    fun getReminders() = runTest {
        // Arrange - create a fake data source
        val reminder1 = ReminderDTO("Title1", "Description1", "Location1", 1.0, 0.0)
        val reminder2 = ReminderDTO("Title2", "Description2", "Location2", 2.0, 0.0)
        val reminder3 = ReminderDTO("Title3", "Description3", "Location3", 3.0, 0.0)

        // Act -  insert the fake data into the DAO
        database.reminderDao().saveReminder(reminder1)
        database.reminderDao().saveReminder(reminder2)
        database.reminderDao().saveReminder(reminder3)

        // Assert - check that the data was inserted
        val loaded = database.reminderDao().getReminders()
        assertThat(loaded.size, `is`(3))
    }

    @Test
    fun deleteOneReminder() = runTest {
        // Arrange - create a fake data source
        val reminder = ReminderDTO("Title", "Description", "Location", 1.0, 0.0)

        // Act -  insert the fake data into the DAO
        database.reminderDao().saveReminder(reminder)
        // Act -  delete the fake data from the DAO
        database.reminderDao().deleteReminder(reminder.id)

        // Assert - check that the data was deleted
        val loaded = database.reminderDao().getReminderById(reminder.id)
        assertThat(loaded, `is`(nullValue()))
    }

    @Test
    fun updateReminder() = runTest {
        // Arrange - create 2 fake data sources WITH THE SAME ID
        val reminder = ReminderDTO("Title", "Description", "Location", 1.0, 0.0, "test00")
        val updatedReminder =
            ReminderDTO("New Title", "New Description", "New Location", 2.0, 0.0, "test00")

        // Act -  insert the fake data into the DAO
        database.reminderDao().saveReminder(reminder)
        // Act -  update the fake data from the DAO
        database.reminderDao().update(updatedReminder)

        // Assert - check that the data was updated
        val loaded = database.reminderDao().getReminderById(reminder.id)
        assertThat(loaded, `is`(updatedReminder))
    }

}
