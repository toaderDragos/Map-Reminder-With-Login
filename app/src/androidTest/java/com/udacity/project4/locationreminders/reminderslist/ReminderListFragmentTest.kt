package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import android.os.Bundle
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.FakeAndroidDataSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.java.KoinJavaComponent.inject
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest {

    private lateinit var appContext: Application
    private lateinit var repository: ReminderDataSource

    // Inject the fake data source
    private val fdataSource: FakeAndroidDataSource by inject(FakeAndroidDataSource::class.java)

    // Inject the ViewModel
    private val viewModel: RemindersListViewModel by inject(RemindersListViewModel::class.java)

    @Before
    fun init() {
        stopKoin()//stop the original app koin
        appContext = getApplicationContext()

        val myModule = module {
            viewModel { RemindersListViewModel(appContext, get() as ReminderDataSource) }
            single<ReminderDataSource> { FakeAndroidDataSource(get()) }
        }

        // declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }

        //Get our real repository
        repository = koinApplication().koin.get()

    }

    // test the navigation of the fragments.
    @Test
    fun clickAddReminderButton_navigatesToAddReminderFragment() {
        // GIVEN - On the home screen
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        // Create a mock NavController
        val navController = mock(NavController::class.java)
        scenario.onFragment {
            // Set the graph on the TestNavHostController
            Navigation.setViewNavController(it.requireView(), navController)
        }

        // WHEN - Click on the add reminder button
        onView(withId(R.id.addReminderFAB)).perform(click())

        // THEN - Verify that we navigate to the add screen
        verify(navController).navigate(
            ReminderListFragmentDirections.toSaveReminder()
        )
    }

    @Test
    fun loadReminders_Should_Update_remindersList_on_success() = runBlockingTest {
        // Given a list of reminder DTOs
        val reminderDTOs = listOf(
            ReminderDTO("Title 1", "Description 1", "Location 1", 0.0, 0.0),
            ReminderDTO("Title 2", "Description 2", "Location 2", 0.0, 0.0)
        )
        fdataSource.saveReminder(reminderDTOs[0])
        fdataSource.saveReminder(reminderDTOs[1])

        // When loading reminders
        viewModel.loadReminders()

        // Then the remindersList should be updated
        assert(viewModel.remindersList.value?.size == 2)
        assert(viewModel.remindersList.value?.get(0)?.title == "Title 1")
        assert(viewModel.remindersList.value?.get(1)?.title == "Title 2")
    }

    @Test
    fun loadReminders_should_show_error_message_on_failure() {
        // Given a failure result from the data source
        fdataSource.setReturnError(true)

        // When loading reminders
        viewModel.loadReminders()

        // Then the showSnackBar event should be triggered
        assert(viewModel.showSnackBar.value != null)
    }


    //    test the displayed data on the UI page of Save Reminder Fragment
    @Test
    fun noReminderDisplayed() = runBlockingTest {
        // GIVEN - On the home screen
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        //GIVEN - Reminders list is empty
        repository.deleteAllReminders()
        // WHEN - No reminders are added
        // THEN - Verify that no reminders are displayed
        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))
    }

// add testing for the error messages.//
}