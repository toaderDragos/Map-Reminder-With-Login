package com.udacity.project4.locationreminders

import android.app.Application
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get

@RunWith(AndroidJUnit4::class)
@LargeTest
//END TO END test to black box test the app
class RemindersActivityTest :
    AutoCloseKoinTest() {// Extended Koin Test - embed autoclose @after method to close Koin after every test

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application

    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */
    @Before
    fun init() {
        stopKoin()//stop the original app koin
        appContext = getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single { RemindersLocalRepository(get()) }
            single { LocalDB.createRemindersDao(appContext) }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        //Get our real repository
        repository = get()

        //clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
    }


    //    TODO: add End to End testing to the app
// Extended Koin Test - embed autoclose @after method to close Koin after every test
// This is required to use Koin's 'runBlockingTest' inject
    @After
    fun tearDown() {
        stopKoin()
    }

//    // END TO END TESTING TO THE APP   - not my code
//    @Test
//    fun createReminder_saveAndDisplayReminder() {
//
//        //1. Launch RemindersActivity
//        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
//        dataBindingIdlingResource.monitorActivity(activityScenario)
//        //2. Click add reminder button
//        onView(withId(R.id.addReminderFAB)).perform(click())
//
//        //3. Input details and navigate to location selection
//        onView(withId(R.id.saveReminder)).perform(click())
//        onView(withId(com.google.android.material.R.id.snackbar_text))
//            .check(matches(withText(R.string.err_enter_title)))
//        onView(withId(R.id.reminderTitle)).perform(replaceText("Title"))
//        onView(withId(R.id.saveReminder)).perform(click())
//        onView(withId(com.google.android.material.R.id.snackbar_text))
//            .check(matches(withText(R.string.err_select_location)))
//        onView(withId(R.id.reminderDescription)).perform(replaceText("Description"))
//        onView(withId(R.id.selectLocation)).perform(click())
//
//        //4. Test menu items
//        openActionBarOverflowOrOptionsMenu(getApplicationContext())
//        onView(withText("Hybrid Map")).perform(click())
//        openActionBarOverflowOrOptionsMenu(getApplicationContext())
//        onView(withText("Satellite Map")).perform(click())
//        openActionBarOverflowOrOptionsMenu(getApplicationContext())
//        onView(withText("Terrain Map")).perform(click())
//        openActionBarOverflowOrOptionsMenu(getApplicationContext())
//        onView(withText("Normal Map")).perform(click())
//
//        //5. Long Click on the map and save location
//        onView(withId(R.id.map)).check(matches(isDisplayed()))
//        onView(withId(R.id.map)).perform(click())
//        Thread.sleep(500L)
//        onView(withId(R.id.save_button)).check(matches(isDisplayed()))
//        onView(withId(R.id.save_button)).perform(click())
//
//        //6. saveReminder details
//        onView(withId(R.id.reminderTitle)).check(matches(isDisplayed()))
//        onView(withId(R.id.saveReminder)).perform(click())
//
//        //7. Assert reminder details are displayed
//        onView(withText("Title")).check(matches(isDisplayed()))
//        onView(withText("Description")).check(matches(isDisplayed()))
//
//        //8. Close activity
//        activityScenario.close()
//    }
//}


}
