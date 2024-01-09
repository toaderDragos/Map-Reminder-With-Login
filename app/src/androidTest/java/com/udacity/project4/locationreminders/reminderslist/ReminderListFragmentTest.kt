package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import android.os.Bundle
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
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
import androidx.test.rule.GrantPermissionRule
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.FakeAndroidDataSource
import com.udacity.project4.util.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.java.KoinJavaComponent.inject
import org.koin.test.KoinTest
import org.koin.test.get
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing   // Don't forget to test live data with getOrAwaitValue()
@MediumTest
class ReminderListFragmentTest : KoinTest {

    private lateinit var appContext: Application
    private lateinit var repository: ReminderDataSource

    // Inject the ViewModel
    private val viewModel: RemindersListViewModel by inject(RemindersListViewModel::class.java)


    private val reminder1 = ReminderDTO("TITLE1", "DESCRIPTION1", "LOCATION1", 23.44, 43.34)
    private val reminder2 = ReminderDTO("TITLE2", "DESCRIPTION2", "LOCATION2", 51.11, 34.11)
    private val reminder3 = ReminderDTO("TITLE3", "DESCRIPTION3", "LOCATION3", 12.12, 45.23)
    private val reminders = mutableListOf(reminder1, reminder2, reminder3)

    @get: Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        android.Manifest.permission.ACCESS_NETWORK_STATE,
    )

    @Before
    fun init() {
        stopKoin() //stop the original app koin
        appContext = getApplicationContext()

        val myModule = module {
            single { reminders }
            single { FakeAndroidDataSource(get()) as ReminderDataSource }
            viewModel { RemindersListViewModel(appContext, get()) }

        }
        // Declare a new koin module
        org.koin.core.context.GlobalContext.startKoin {
            modules(listOf(myModule))
        }

        // Get our real repository
        repository = get()

        // Clear the data to start fresh otherwise earlier saved reminders would mess up the calculations
        runTest {
            repository.deleteAllReminders()
        }

    }

    @After
    fun tearDown() {
        stopKoin()
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

    // Should this test be an instrumented test?
    @Test
    fun loadReminders_Should_Update_remindersList_on_success() = runTest {
        // Given a list of reminder DTOs
        val reminderDTOs = listOf(
            ReminderDTO("Title 1", "Description 1", "Location 1", 0.1, 0.0),
            ReminderDTO("Title 2", "Description 2", "Location 2", 1.3, 4.0)
        )

        repository.saveReminder(reminderDTOs[0])
        repository.saveReminder(reminderDTOs[1])

        // When loading reminders
        viewModel.loadReminders()

        // Then the remindersList should be updated - they are LIveData so we should use the getOrAwaitValue() extension function
        assert(viewModel.remindersList.getOrAwaitValue().size == 2)
        assert(viewModel.remindersList.getOrAwaitValue()[0].title == "Title 1")
        assert(viewModel.remindersList.getOrAwaitValue()[1].title == "Title 2")
    }

    @Test
    fun loadReminders_should_show_error_message_on_failure() = runTest {
        // Given a failure result from the data source
        (repository as FakeAndroidDataSource).setReturnError(true)

        // When loading reminders
        viewModel.loadReminders()

        // Then the showSnackBar event should be triggered
        assert(viewModel.showSnackBar.getOrAwaitValue() != null)
    }


    @Test
    fun noReminderDisplayed() = runTest {
        // GIVEN - On the home screen
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        //GIVEN - Reminders list is empty
        repository.deleteAllReminders()

        // THEN - Verify that no reminders are displayed
        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))
    }

}