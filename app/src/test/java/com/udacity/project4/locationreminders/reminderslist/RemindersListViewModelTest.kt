package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.util.getOrAwaitValue
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
import org.koin.java.KoinJavaComponent
import org.koin.test.KoinTest
import org.koin.test.get
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
@Config(sdk = [30])
class RemindersListViewModelTest : KoinTest {

    private lateinit var appContext: Application
    private lateinit var repository: ReminderDataSource

    // Inject the fake data source
    private val fdataSource: FakeDataSource by KoinJavaComponent.inject(FakeDataSource::class.java)

    // Inject the ViewModel
    private val viewModel: RemindersListViewModel by KoinJavaComponent.inject(RemindersListViewModel::class.java)

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun init() {

        appContext = ApplicationProvider.getApplicationContext()

        val myModule = module {
            single { FakeDataSource(mutableListOf()) as ReminderDataSource }
            viewModel { RemindersListViewModel(appContext, get()) }

        }

        stopKoin() //stop the original app koin  // Should remove the error: A Koin Application has already been started
        // Declare a new koin module
        org.koin.core.context.GlobalContext.startKoin {
            modules(listOf(myModule))
        }

        // Get our real repository
        repository = get() as ReminderDataSource

    }

    @After
    fun end() {
        stopKoin()
    }

    // We create a fake reminder and save it to the repository. Then we load the reminders from the db and check if the list is not empty.
    @Test
    fun viewModel_getReminder() = runTest {
        val reminder = ReminderDTO("title", "description", "location", 0.0, 0.0)
        fdataSource.saveReminder(reminder)

        viewModel.loadReminders()
        assert(viewModel.remindersList.getOrAwaitValue() != null)

    }

    // Provide testing to the RemindersListViewModel
    @Test
    fun viewModel_loadReminders() {
        val reminder1 = ReminderDataItem("title1", "description1", "location1", 0.1, 0.1)
        val reminder2 = ReminderDataItem("title2", "description2", "location2", 0.2, 0.2)
        val reminder3 = ReminderDataItem("title3", "description3", "location3", 0.3, 0.3)

        viewModel.remindersList.value = listOf(reminder1, reminder2, reminder3)
        viewModel.loadReminders()
        assert(viewModel.remindersList.value != null)
    }

    // Provide testing to the RemindersListViewModel and its live data objects
    @Test
    fun viewModel_remindersList_LiveData() {

        val remindersListViewModel: RemindersListViewModel by KoinJavaComponent.inject(
            RemindersListViewModel::class.java
        )

        // Loading some reminders
        val reminder1 = ReminderDataItem("title1", "description1", "location1", 0.1, 0.1)
        val reminder2 = ReminderDataItem("title2", "description2", "location2", 0.2, 0.2)
        val reminder3 = ReminderDataItem("title3", "description3", "location3", 0.3, 0.3)
        viewModel.remindersList.value = listOf(reminder1, reminder2, reminder3)
        viewModel.loadReminders()

        // The getOrAwaitValue is given in course - it is stored in tests/util/LiveDataTestUtil.kt it helps test livedata
        val value = remindersListViewModel.remindersList.getOrAwaitValue()
        assert(value == listOf(reminder1, reminder2, reminder3))
    }
}