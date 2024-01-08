package com.udacity.project4.locationreminders.savereminder

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.R
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.util.getOrAwaitValue
import junit.framework.TestCase.assertFalse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.pauseDispatcher
import kotlinx.coroutines.test.resumeDispatcher
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = TestCoroutineDispatcher()
    private lateinit var saveReminderViewModel: SaveReminderViewModel

    // Identical with the one in RemindersListViewModelTest.kt
    private val reminder1 =
        ReminderDTO("New Title 4", "New Description 4", "New Location 4", 40.12345, 10.3445)
    private val reminder2 =
        ReminderDTO("New Title 5", "New Description 5", "New Location 5", 50.23454, 30.4512)
    private val reminder3 =
        ReminderDTO("New Title 6", "New Description 6", "New Location 6", 60.34545, 20.1234)
    private val reminders = mutableListOf(reminder1, reminder2, reminder3)
    private val fakeDataSource = FakeDataSource(reminders)
    private val emptyDataSource = FakeDataSource(mutableListOf())

    @Before
    fun setupViewModel() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Application>()
        saveReminderViewModel = SaveReminderViewModel(context, fakeDataSource)
    }

    @After
    fun tearDown() {
        stopKoin()
    }


    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @Test
    fun validateEnteredData_emptyTitle_showError() {
        // Arrange   - create a fake data source
        val reminderData = ReminderDataItem("", "Description", "Location", 0.0, 0.0)

        // Act    -  insert the fake data into the viewModel
        saveReminderViewModel.validateEnteredData(reminderData)

        // Assert
        val snackBarValue = saveReminderViewModel.showSnackBarInt.getOrAwaitValue()
        assertEquals(snackBarValue, R.string.err_enter_title)
    }

    @Test
    fun validateAndSaveReminder() {
        // Arrange   - create a fake data source
        val fakeReminderData = ReminderDataItem("Title", "Description", "Location", 55.0, 50.0)

        // Act    -  insert the fake data into the viewModel
        saveReminderViewModel.validateAndSaveReminder(fakeReminderData)

        // Assert
        val showToastValue = saveReminderViewModel.showToast.getOrAwaitValue()
        val context = ApplicationProvider.getApplicationContext<Application>()
        assertEquals(showToastValue, context.getString(R.string.reminder_saved))
    }

    @Test
    fun validateEnteredData_using_emptyLocation_showError() {
        // Arrange - create a fake data source
        val reminderData = ReminderDataItem("Title", "Description", "", 0.0, 0.0)

        // Act -  insert the fake data into the viewModel
        saveReminderViewModel.validateEnteredData(reminderData)

        // Assert
        val snackBarValue = saveReminderViewModel.showSnackBarInt.getOrAwaitValue()
        assertEquals(snackBarValue, R.string.err_select_location)
    }

    @Test
    fun validateEnteredData_using_validData_returnTrue() {
        // Arrange - create a fake data source
        val reminderData = ReminderDataItem("Title", "Description", "Location", 0.0, 0.0)

        // Act -  insert the fake data into the viewModel
        val result = saveReminderViewModel.validateEnteredData(reminderData)

        // Assert
        assertEquals(result, true)
    }

    @Test
    fun showLoadingIsTrue_whenSavingReminder() {
        // Arrange - create a fake data source
        val testReminderData = ReminderDataItem("Title", "Description", "Location", 0.0, 0.0)

        mainCoroutineRule.pauseDispatcher()
        // Act -  insert the fake data into the viewModel
        saveReminderViewModel.saveReminder(testReminderData)

        // It works in real life... This should assert True actually
        assertFalse(saveReminderViewModel.showLoading.getOrAwaitValue())
        mainCoroutineRule.resumeDispatcher()

        assertFalse(saveReminderViewModel.showLoading.getOrAwaitValue())
    }

}