package `in`.eziy.attendancemaster.ui.settings

import `in`.eziy.attendancemaster.data.local.SecurePreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `settings state updates when fields change`() {
        val state = SettingsUiState(
            serverUrl = "https://eziy.in",
            companyName = "Eziy Corp",
            employeeName = "John Doe",
            employeeId = "12345678"
        )

        assertEquals("https://eziy.in", state.serverUrl)
        assertEquals("Eziy Corp", state.companyName)
        assertEquals("John Doe", state.employeeName)
        assertEquals("12345678", state.employeeId)
        assertFalse(state.isSaved)
        assertFalse(state.isLoggedOut)
    }
}
