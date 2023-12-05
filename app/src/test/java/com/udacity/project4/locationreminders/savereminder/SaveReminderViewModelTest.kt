package com.udacity.project4.locationreminders.savereminder

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.util.getOrAwaitValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.setMain
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.jupiter.api.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.mockito.Mockito.mock

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = TestCoroutineDispatcher()

    private lateinit var saveReminderViewModel: SaveReminderViewModel

    private lateinit var fakeDataSource: ReminderDataSource

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        val app = mock(Application::class.java)

        saveReminderViewModel = SaveReminderViewModel(app, fakeDataSource)

        val myModule = module {
            viewModel { SaveReminderViewModel(app, fakeDataSource) }
            single<ReminderDataSource> { FakeDataSource(get()) }
        }

        // Declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
    }

    @Test
    fun validateEnteredData_emptyTitle_showError() {
        // Arrange   - create a fake data source
        val reminderData = ReminderDataItem("", "Description", "Location", 0.0, 0.0)

        // Act    -  insert the fake data into the viewModel
        saveReminderViewModel.validateEnteredData(reminderData)

        // Assert
        val snackBarValue = saveReminderViewModel.showSnackBar.getOrAwaitValue()
        assertThat(snackBarValue, equals(R.string.err_enter_title))
    }

//    @Test
//    fun validateAndSaveReminder() {
//    }
//
//    @Test
//    fun saveReminder() {
//    }
//
//    @Test
//    fun validateEnteredData() {
//    }
//
//    @Test
//    fun getDataSource() {
//    }

    @After
    fun tearDown() {
        stopKoin()
    }

}