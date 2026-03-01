package `in`.eziy.attendancemaster.ui.history

import `in`.eziy.attendancemaster.fakes.FakeAttendanceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeAttendanceRepository
    private lateinit var viewModel: HistoryViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeAttendanceRepository()
        viewModel = HistoryViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadHistory populates records list on success`() = runTest {
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals(2, state.records.size)
        assertEquals("Today 09:00 AM", state.records[0].checkIn)
    }

    @Test
    fun `loadHistory sets error message on repository failure`() = runTest {
        fakeRepository.shouldReturnError = true
        viewModel.loadHistory()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Could not load history", state.errorMessage)
        assertTrue(state.records.isEmpty())
    }
}
