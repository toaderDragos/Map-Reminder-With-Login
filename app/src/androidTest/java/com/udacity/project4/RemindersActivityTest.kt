package com.udacity.project4
//
//import android.app.Application
//import androidx.fragment.app.testing.launchFragmentInContainer
//import androidx.navigation.NavController
//import androidx.navigation.Navigation
//import androidx.test.core.app.ApplicationProvider.getApplicationContext
//import androidx.test.espresso.Espresso.onView
//import androidx.test.ext.junit.runners.AndroidJUnit4
//import androidx.test.filters.LargeTest
//import com.udacity.project4.locationreminders.data.ReminderDataSource
//import com.udacity.project4.locationreminders.data.local.LocalDB
//import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
//import com.udacity.project4.locationreminders.reminderslist.ReminderListFragment
//import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
//import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
//import kotlinx.coroutines.runBlocking
//import org.junit.After
//import org.junit.Before
//import org.junit.Test
//import org.junit.runner.RunWith
//import org.koin.androidx.viewmodel.dsl.viewModel
//import org.koin.core.context.startKoin
//import org.koin.core.context.stopKoin
//import org.koin.dsl.module
//import org.mockito.Mockito.mock
//import com.udacity.project4.locationreminders.reminderslist.ReminderListFragmentDirections
//import org.mockito.Mockito.verify
//import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
//import androidx.test.espresso.IdlingRegistry
//import androidx.test.espresso.action.ViewActions.*
//import androidx.test.espresso.assertion.ViewAssertions.matches
//import androidx.test.espresso.matcher.ViewMatchers.*
//import androidx.test.espresso.matcher.ViewMatchers.withId
//import androidx.test.espresso.action.ViewActions.click
//import org.koin.core.context.GlobalContext.get
//import org.koin.androidx.fragment.android.setupKoinFragmentFactory
//@RunWith(AndroidJUnit4::class)
//@LargeTest
////END TO END test to black box test the app
// class RemindersActivityTest : KoinTest {// Extended Koin Test - embed autoclose @after method to close Koin after every test
//
//    private lateinit var repository: ReminderDataSource
//    private lateinit var appContext: Application
//
//    /**
//     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
//     * at this step we will initialize Koin related code to be able to use it in out testing.
//     */
//    @Before
//    fun init() {
//        stopKoin()//stop the original app koin
//        appContext = getApplicationContext()
//        val myModule = module {
//            viewModel {
//                RemindersListViewModel(
//                    appContext,
//                    get() as ReminderDataSource
//                )
//            }
//            single {
//                SaveReminderViewModel(
//                    appContext,
//                    get() as ReminderDataSource
//                )
//            }
//            single { RemindersLocalRepository(get()) }
//            single { LocalDB.createRemindersDao(appContext) }
//        }
//        //declare a new koin module
//        startKoin {
//            modules(listOf(myModule))
//        }
//        //Get our real repository
//        repository = get()
//
//        //clear the data to start fresh
//        runBlocking {
//            repository.deleteAllReminders()
//        }
//    }
//
//    //    TODO: add End to End testing to the app
//// Extended Koin Test - embed autoclose @after method to close Koin after every test
//// This is required to use Koin's 'runBlockingTest' inject
//
//    @Test
//    fun clickAddReminderFAB_navigatesToAddReminder() {
//        // Create a mock NavController
//        val navController = mock(NavController::class.java)
//
//        // Launch the ReminderListFragment
//        val scenario = launchFragmentInContainer<ReminderListFragment>(themeResId = R.style.AppTheme)
//
//        scenario.onFragment { fragment ->
//            // Set the NavController property on the fragment
//            Navigation.setViewNavController(fragment.requireView(), navController)
//        }
//
//        // Perform a click action on the addReminderFAB
//        onView(withId(R.id.addReminderFAB)).perform(click())
//
//        // Verify that the NavController navigated to the add reminder screen
//        verify(navController).navigate(ReminderListFragmentDirections.toSaveReminder())
//
//    }
//
//
//
//
//    @After
//    fun tearDown() {
//        stopKoin()
//    }
//
//    // Test AuthenticationActivity.kt
//
//
//    // Test the navigation to the SaveReminderFragment
//
//
//}
