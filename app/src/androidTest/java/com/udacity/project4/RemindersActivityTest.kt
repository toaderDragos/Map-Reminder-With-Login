package com.udacity.project4

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.google.android.material.internal.ContextUtils.getActivity
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
import org.hamcrest.CoreMatchers.endsWith
import org.hamcrest.CoreMatchers.not
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
import org.koin.test.inject

@RunWith(AndroidJUnit4::class)
@LargeTest
//END TO END test to black box test the app
class RemindersActivityTest :
    KoinTest {// Extended Koin Test - embed autoclose @after method to close Koin after every test

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application
    private val dataBindingIdlingResource = DataBindingIdlingResource()
    private val reminder1 = ReminderDTO("TITLE1", "DESCRIPTION1", "LOCATION123", 23.44, 43.34)
    private val reminders = mutableListOf(reminder1)
    private lateinit var saveViewModel: SaveReminderViewModel

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
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun create_Reminder_Save_Reminder_Display_Reminder_Update_Reminder_Delete_Reminder() = runTest {

        // Setting a fake location for the reminder including the latitude and longitude ( in SaveReminder Fragment they are needed for the geofence )
        saveViewModel = inject<SaveReminderViewModel>().value
        saveViewModel.reminderSelectedLocationStr.postValue(reminder1.location)
        saveViewModel.latitude.postValue(reminder1.latitude)
        saveViewModel.longitude.postValue(reminder1.longitude)

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
        // Give a title and description to the reminder // Update the reminder as we go along
        onView(withId(R.id.reminderTitle)).perform(replaceText("New Title"))
        reminder1.title = "New Title"
        onView(withId(R.id.reminderDescription)).perform(replaceText("New Description"))
        reminder1.description = "New Description"

        /*** Select location and map testing ***/
        // Click on the select location button
        onView(withId(R.id.selectLocation)).perform(click())

        // Wait for the map to zoom in to user's position
        Thread.sleep(1500)
        // Check if the Select Location screen is displayed - map, saveLocation button, cancel
        onView(withId(R.id.map)).check(matches(isDisplayed()))
        // Check if the Save Location button is displayed
        onView(withId(R.id.saveLocationButton)).check(matches(isDisplayed()))
        // Click on the map  // May not select any location so this might go wrong
        onView(withId(R.id.map)).perform(click())

        /*** Test menu choices for map styling. Thread.sleep is just for a nicer user experience ***/
        // Click on the menu
        openActionBarOverflowOrOptionsMenu(getApplicationContext())
        // Click on the Hybrid Map
        onView(withText("Hybrid Map")).perform(click())
        Thread.sleep(500)
        // Click on the menu
        openActionBarOverflowOrOptionsMenu(getApplicationContext())
        // Click on the Satellite Map
        onView(withText("Satellite Map")).perform(click())
        Thread.sleep(500)
        // Click on the menu
        openActionBarOverflowOrOptionsMenu(getApplicationContext())
        // Click on the Terrain Map
        onView(withText("Terrain Map")).perform(click())
        Thread.sleep(500)
        // Click on the menu
        openActionBarOverflowOrOptionsMenu(getApplicationContext())
        // Click on the Normal Map
        onView(withText("Normal Map")).perform(click())
        Thread.sleep(500)

        // Just go back without saving because testing the selection of a POI is almost impossible
        Espresso.pressBack()

        // In the save reminder screen check if the reminder title and description is still displayed
        onView(withId(R.id.reminderTitle)).check(matches(isDisplayed()))
        onView(withId(R.id.reminderDescription)).check(matches(isDisplayed()))
        Thread.sleep(1000)

        // Check if the selected location is displayed
        onView(withId(R.id.selectedLocation)).check(matches(withText(reminder1.location)))

        // Click on the save reminder button   // Location should be already loaded because it was saved in the repository earlier
        onView(withId(R.id.saveReminder)).perform(click())
        Thread.sleep(500)

        // Check if the reminder is displayed in the reminders list - they are hidden in a recyclerview so this is not the way
//        onView(withText("New Title")).check(matches(isDisplayed()))
//        onView(withText("New Description")).check(matches(isDisplayed()))

        // monitor activityScenario for "idling" (used to flow control the espresso tests)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        /*** Update the reminder testing ***/
        // Click on the reminder - the reminder is in a recyclerview so it's not directly accessible
        // onView(withText(endsWith("New Title"))).perform(click()) - wrong!

        // verify that the ReminderListFragment is in view
        onView(withId(R.id.reminderssRecyclerView)).check((matches(isDisplayed())))
        // Works
        onView(withId(R.id.reminderssRecyclerView))
            .perform(
                RecyclerViewActions.actionOnItem<ViewHolder>(
                    hasDescendant(withText("New Title")),
                    click()
                )
            )

        // Check if the clicked reminder details are displayed
        onView(withId(R.id.reminderTitle)).check(matches(isDisplayed()))
        onView(withId(R.id.reminderDescription)).check(matches(isDisplayed()))
        // Update title and description
        onView(withId(R.id.reminderTitle)).perform(replaceText("Updated Title"))
        onView(withId(R.id.reminderDescription)).perform(replaceText("Updated Description"))

        // use idling resource to wait for data binding to finish
        dataBindingIdlingResource.monitorActivity(activityScenario)
        Thread.sleep(1000)

        // Hide keyboard
        onView(withId(R.id.reminderDescription)).perform(closeSoftKeyboard())
        // Check if the Update Location button is displayed   - this is the id of the button in the UPDATE REMINDER screen
        onView(withId(R.id.updateReminder)).check(matches(isDisplayed()))
        // Click on the update reminder button
        onView(withId(R.id.updateReminder)).perform(click())

        // Check if the UPDATED reminder is displayed in the reminders list
        onView(withText("Updated Title")).check(matches(isDisplayed()))
        onView(withText("Updated Description")).check(matches(isDisplayed()))

        /*** Delete the reminder testing ***/
        // Click on the reminder
        onView(withText(endsWith("Updated Title"))).perform(click())

        // Hide keyboard
        onView(withId(R.id.reminderDescription)).perform(closeSoftKeyboard())
        // Wait for the snackbar to disappear
        Thread.sleep(3000)
        // Click on the delete reminder button
        onView(withId(R.id.deleteReminder)).perform(click())
        // Check if there are no reminders displayed
        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))
    }


    /*** Setting a fake location for the reminder including the latitude and longitude.
     * This is because otherwise the location notification appears first and the test fails.
     * I don't set any title intentionally. Test passes
     * ***/
    @Test
    fun show_Message_on_Title_Missing_Error() {

        // Some fake data stored in the viewmodel
        saveViewModel = inject<SaveReminderViewModel>().value
        saveViewModel.reminderSelectedLocationStr.postValue(reminder1.location)
        saveViewModel.latitude.postValue(reminder1.latitude)
        saveViewModel.longitude.postValue(reminder1.longitude)

        // Launch RemindersActivity
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        // Use idling resources to wait for data binding to finish
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // Click on the add reminder button
        onView(withId(R.id.addReminderFAB)).perform(click())

        // Click on the Save Reminder button
        onView(withId(R.id.saveReminder)).perform(click())
        Thread.sleep(500)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // Check if the snack-bar is displayed
        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(withText(R.string.err_enter_title)))

    }

    /*** Trying to save a reminder without a location should display an error message. PASSES */
    @Test
    fun show_Message_when_location_is_empty() {
        // Some fake data that pre-populates title and description in the save fragment
        saveViewModel = inject<SaveReminderViewModel>().value
        saveViewModel.reminderTitle.postValue(reminder1.title)
        saveViewModel.reminderDescription.postValue(reminder1.description)

        // Launch RemindersActivity
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        // Use idling resources to wait for data binding to finish
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // Click on the add new reminder button
        onView(withId(R.id.addReminderFAB)).perform(click())

        // Click on the Save Reminder button
        onView(withId(R.id.saveReminder)).perform(click())
        Thread.sleep(500)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // Check if the snack-bar is displayed
        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(withText(R.string.err_select_location)))

    }

    /*** Saving a complete reminder should display a successful message. PASSES :)))*/
    @Test
    fun show_toast_message_when_reminder_is_saved() {
        // Some fake data that pre-populates title and description in the save fragment
        saveViewModel = inject<SaveReminderViewModel>().value
        saveViewModel.reminderTitle.postValue(reminder1.title)
        saveViewModel.reminderDescription.postValue(reminder1.description)
        saveViewModel.reminderSelectedLocationStr.postValue(reminder1.location)
        saveViewModel.latitude.postValue(reminder1.latitude)
        saveViewModel.longitude.postValue(reminder1.longitude)

        // Launch RemindersActivity
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        // Use idling resources to wait for data binding to finish
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // Click on the add new reminder button
        onView(withId(R.id.addReminderFAB)).perform(click())

        // Click on the Save Reminder button
        onView(withId(R.id.saveReminder)).perform(click())
        Thread.sleep(500)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // Check if the toast is displayed
        val activity = getActivity(appContext)
        if (activity != null) {
            onView(withText(R.string.reminder_saved))
                .inRoot(withDecorView(not(activity.window.decorView)))
                .check(matches(isDisplayed()))
        }
    }


}
