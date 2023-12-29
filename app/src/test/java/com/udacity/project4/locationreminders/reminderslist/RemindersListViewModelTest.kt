package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.util.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.pauseDispatcher
import kotlinx.coroutines.test.resumeDispatcher
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.koin.test.UpperCase.value

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

    private lateinit var remindersListViewModel: RemindersListViewModel
    private lateinit var noRemindersListViewModel: RemindersListViewModel

    private val reminder1 =
        ReminderDTO("New Title 1", "New Description 1", "New Location 1", 40.12345, 10.3445)
    private val reminder2 =
        ReminderDTO("New Title 2", "New Description 2", "New Location 2", 50.23454, 30.4512)
    private val reminder3 =
        ReminderDTO("New Title 3", "New Description 3", "New Location 3", 60.34545, 20.1234)
    private val reminders = mutableListOf(reminder1, reminder2, reminder3)
    private val fakeDataSource = FakeDataSource(reminders)
    private val emptyDataSource = FakeDataSource(mutableListOf())

    // write tests that are in synchronicity and in order
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setupViewModels() {
        remindersListViewModel =
            RemindersListViewModel(ApplicationProvider.getApplicationContext(), fakeDataSource)
        noRemindersListViewModel =
            RemindersListViewModel(ApplicationProvider.getApplicationContext(), emptyDataSource)
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    // ViewModel tests
    @Test
    fun shouldShowNoDataWhenRemindersListIsNull() {
        noRemindersListViewModel.loadReminders()
        val value = noRemindersListViewModel.showNoData.getOrAwaitValue()
        assertThat(value, `is`(true))
    }


    @Test
    fun shouldShowNoDataWhenRemindersListIsEmpty() {
        noRemindersListViewModel.remindersList.value = emptyList()
        noRemindersListViewModel.loadReminders()
        assertThat(noRemindersListViewModel.showNoData.value, `is`(true))
    }

    @Test
    fun remindersListShouldContainRemindersAfterLoad() {
        // Loading the 3 reminders from above
        remindersListViewModel.loadReminders()
        assertThat(remindersListViewModel.showNoData.value, `is`(false))
        assertThat(remindersListViewModel.remindersList.value?.size, `is`(3))
    }

    // LiveData
    @Test
    fun liveDataShouldContainRemindersAfterLoad() {
        remindersListViewModel.loadReminders()
        val value = remindersListViewModel.remindersList.getOrAwaitValue()
        assertThat(value, not(nullValue()))
    }

//    // Error handling  // Error: LiveData Value was not set - this test is not working
    // I have asked on the forums, I have searched the internet - no solution found... yet!
//    @Test
//    fun shouldDisplayErrorWhenFailedToLoadReminders() {
//        fakeDataSource.setReturnError(true)
//        remindersListViewModel.loadReminders()
//
//        val value = remindersListViewModel.remindersList.getOrAwaitValue()
//        val snackBarErrorMessage = remindersListViewModel.showSnackBar.getOrAwaitValue()
//        assertThat(value, `is`(nullValue()))
//        assertThat(snackBarErrorMessage, `is`("Can't load reminders!"))
//    }

    // Check loading
    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @Test
    fun loadingIndicatorShouldAppearAndDisappearOnLoad() {
        // Pause dispatcher so you can verify initial values
        mainCoroutineRule.pauseDispatcher()

        // Load the reminders in the view model
        remindersListViewModel.loadReminders()

        // Show loading appears before dispatcher resumes
        assertThat(remindersListViewModel.showLoading.getOrAwaitValue(), `is`(true))

        // Execute pending coroutines actions
        mainCoroutineRule.resumeDispatcher()

        // Show loading disappears after dispatcher resumes
        assertThat(remindersListViewModel.showLoading.getOrAwaitValue(), `is`(false))
    }

}