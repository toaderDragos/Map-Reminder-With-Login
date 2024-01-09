package com.udacity.project4

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.FakeAndroidDataSource
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorActivity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get

@RunWith(AndroidJUnit4::class)
@LargeTest
//END TO END test to black box test the app
class RemindersActivityTest :
    KoinTest {// Extended Koin Test - embed autoclose @after method to close Koin after every test

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application
    private val dataBindingIdlingResource = DataBindingIdlingResource()
    private val reminder1 = ReminderDTO("TITLE1", "DESCRIPTION1", "LOCATION1", 23.44, 43.34)
    private val reminders = mutableListOf(reminder1)

    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     * Don't forget to disable animations and transitions for the app to be tested properly
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun init() {
        stopKoin() //stop the original app koin
        appContext = getApplicationContext()

        val myModule = module {
            single { reminders }
            single { FakeAndroidDataSource(get()) as ReminderDataSource }
            viewModel { RemindersListViewModel(appContext, get()) }
            single { SaveReminderViewModel(appContext, get() as ReminderDataSource) }
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

    //      It ensures that Espresso waits for data binding to finish updating the UI before it tries to interact with it.
    //      This is particularly useful when your UI changes in response to data changes, and these changes are not immediate.
    //      It helps avoid flakiness in UI tests caused by asynchronous data binding operations.  */
    @Before
    fun registerIdlingResource() {
        ActivityScenario.launch(RemindersActivity::class.java).use { scenario ->
            dataBindingIdlingResource.monitorActivity(scenario)
            IdlingRegistry.getInstance().register(dataBindingIdlingResource)
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }

    @get: Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        android.Manifest.permission.ACCESS_NETWORK_STATE,
    )

    // test authenticationActivity
    @Test
    fun create_Reminder_Save_Reminder_Display_Reminder_Update_Reminder_Delete_Reminder() {

        // Launch RemindersActivity
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        // Use idling resources to wait for data binding to finish
        dataBindingIdlingResource.monitorActivity(activityScenario)

        /*** Reminders list testing ***/
        // Check if the reminders list is displayed - there should be no list here because the repository starts with deleting all data
        // Instead there should be an icon
        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))
        // Click on the add reminder button
        onView(withId(R.id.addReminderFAB)).perform(click())

        // Check if the save reminder screen is displayed - title, description, location, selectLocation
        // If the next lines are correct, then the save reminder screen is displayed implicitly
        onView(withId(R.id.selectLocation)).check(matches(isDisplayed()))
        // Give a title and description to the reminder
        onView(withId(R.id.reminderTitle)).perform(replaceText("New Title"))
        onView(withId(R.id.reminderDescription)).perform(replaceText("New Description"))

        /*** Select location and map testing ***/
        // Click on the select location button
        onView(withId(R.id.selectLocation)).perform(click())

        // Wait for the map to zoom in to user's position
        Thread.sleep(2000)
        // Check if the Select Location screen is displayed - map, saveLocation button, cancel
        onView(withId(R.id.map)).check(matches(isDisplayed()))
        // Check if the Save Location button is displayed
        onView(withId(R.id.saveLocationButton)).check(matches(isDisplayed()))
        // Click on the map  // May not select any location so this might go wrong
        onView(withId(R.id.map)).perform(click())
        // Long click on the map
        onView(withId(R.id.map)).perform(longClick())

        /*** Test menu choices for map styling ***/
        // Click on the menu
        openActionBarOverflowOrOptionsMenu(getApplicationContext())
        // Click on the Hybrid Map
        onView(withText("Hybrid Map")).perform(click())
        // Click on the menu
        openActionBarOverflowOrOptionsMenu(getApplicationContext())
        // Click on the Satellite Map
        onView(withText("Satellite Map")).perform(click())
        // Click on the menu
        openActionBarOverflowOrOptionsMenu(getApplicationContext())
        // Click on the Terrain Map
        onView(withText("Terrain Map")).perform(click())
        // Click on the menu
        openActionBarOverflowOrOptionsMenu(getApplicationContext())
        // Click on the Normal Map
        onView(withText("Normal Map")).perform(click())

        // Click on the save location button
        onView(withId(R.id.saveLocationButton)).perform(click())

        // In the save reminder screen check if the reminder title and description is still displayed
        onView(withId(R.id.reminderTitle)).check(matches(isDisplayed()))
        onView(withId(R.id.reminderDescription)).check(matches(isDisplayed()))
        // Click on the save reminder button
        onView(withId(R.id.saveReminder)).perform(click())

        // Check if the reminder is displayed in the reminders list
        onView(withText("New Title")).check(matches(isDisplayed()))
        onView(withText("New Description")).check(matches(isDisplayed()))

        /*** Update the reminder testing ***/
        // Click on the reminder
        onView(withText("New Title")).perform(click())

        // Check if the clicked reminder details are displayed
        onView(withId(R.id.reminderTitle)).check(matches(isDisplayed()))
        onView(withId(R.id.reminderDescription)).check(matches(isDisplayed()))
        // Update title and description
        onView(withId(R.id.reminderTitle)).perform(replaceText("Updated Title"))
        onView(withId(R.id.reminderDescription)).perform(replaceText("Updated Description"))

        // use idling resource to wait for data binding to finish
        dataBindingIdlingResource.monitorActivity(activityScenario)
        Thread.sleep(2000)

        // Check if the Save Location button is displayed
        onView(withId(R.id.updateReminder)).check(matches(isDisplayed()))
        // Click on the update reminder button
        onView(withId(R.id.updateReminder)).perform(click())

        // Check if the UPDATED reminder is displayed in the reminders list
        onView(withText("Updated Title")).check(matches(isDisplayed()))
        onView(withText("Updated Description")).check(matches(isDisplayed()))

        /*** Delete the reminder testing ***/
        // Click on the reminder
        onView(withText("Updated Title")).perform(click())
        // Click on the delete reminder button
        onView(withId(R.id.deleteReminder)).perform(click())
        // Check if there are no reminders displayed
        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))
    }


}
