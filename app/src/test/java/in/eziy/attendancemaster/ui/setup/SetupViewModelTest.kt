package `in`.eziy.attendancemaster.ui.setup

import `in`.eziy.attendancemaster.fakes.FakeAttendanceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SetupViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeAttendanceRepository
    private lateinit var viewModel: SetupViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeAttendanceRepository()
        viewModel = SetupViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `authenticate with non-HTTPS URL sets error message`() {
        viewModel.onServerUrlChange("http://insecure-server.com")
        viewModel.onEmployeeIdChange("12345678")
        viewModel.onPinChange("1234")

        viewModel.authenticate()

        val state = viewModel.uiState.value
        assertEquals("Please enter an HTTPS server URL", state.errorMessage)
        assertFalse(state.isSuccess)
    }

    @Test
    fun `authenticate with invalid badge ID length sets error message`() {
        viewModel.onServerUrlChange("https://valid-server.com")
        viewModel.onEmployeeIdChange("123") // Less than 8 digits
        viewModel.onPinChange("1234")

        viewModel.authenticate()

        val state = viewModel.uiState.value
        assertEquals("Employee ID must be 8 digits", state.errorMessage)
        assertFalse(state.isSuccess)
    }

    @Test
    fun `authenticate with blank PIN sets error message`() {
        viewModel.onServerUrlChange("https://valid-server.com")
        viewModel.onEmployeeIdChange("12345678")
        viewModel.onPinChange("   ") // Blank PIN

        viewModel.authenticate()

        val state = viewModel.uiState.value
        assertEquals("Please enter your Attendance PIN", state.errorMessage)
        assertFalse(state.isSuccess)
    }

    @Test
    fun `authenticate with valid parameters succeeds`() = runTest {
        viewModel.onServerUrlChange("https://valid-server.com")
        viewModel.onEmployeeIdChange("12345678")
        viewModel.onPinChange("1234")

        viewModel.authenticate()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.errorMessage)
        assertTrue(state.isSuccess)
        assertFalse(state.isLoading)
    }

    @Test
    fun `authenticate when repository returns error sets error state`() = runTest {
        fakeRepository.shouldReturnError = true
        viewModel.onServerUrlChange("https://valid-server.com")
        viewModel.onEmployeeIdChange("12345678")
        viewModel.onPinChange("1234")

        viewModel.authenticate()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Invalid credentials", state.errorMessage)
        assertFalse(state.isSuccess)
        assertFalse(state.isLoading)
    }
}
