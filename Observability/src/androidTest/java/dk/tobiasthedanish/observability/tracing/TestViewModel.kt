package dk.tobiasthedanish.observability.tracing

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dk.tobiasthedanish.observability.Observability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TestViewModel(
    private val repo: TestRepository,
): ViewModel() {
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean>
        get() = _isLoading

    fun fetchData() {
        val trace = Observability.createTrace("TestViewModel.fetchData")
        trace?.apply {
            if (TraceTestActivity.rootTrace != null) {
                setParent(TraceTestActivity.rootTrace!!)
            }
            start()
        }
        Log.d("TestViewModel", "ViewModel trace: ${trace?.traceId}")
        _isLoading.update { true }
        viewModelScope.launch {
            val data = repo.fetchData(trace)
            Log.d("TestViewModel", "Data: $data")

            trace?.end()
            _isLoading.update { false }
        }
    }
}