package dk.tobiasthedanish.observability.tracing

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import dk.tobiasthedanish.observability.Observability

class TraceTestActivity: ComponentActivity() {
    companion object {
        @VisibleForTesting
        var rootTrace: Trace? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rootTrace = Observability.startTrace("TraceTestActivity")
        Log.d("TraceTestActivity", "rootTrace: $rootTrace")

        val testRepository = TestRepository()
        val viewModel = TestViewModel(testRepository)

        enableEdgeToEdge()
        setContent {
            PrimaryScreen(viewModel)
        }
    }

    override fun onStop() {
        super.onStop()

        rootTrace?.end()
    }
}

@Composable
fun PrimaryScreen(viewModel: TestViewModel) {
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).fillMaxSize()
        ) {
            Text("PrimaryScreen")
            if (isLoading) {
                Text("Loading")
            }
            Button(
                onClick = {
                    Log.d("PrimaryScreen", "Fetch data button clicked")
                    viewModel.fetchData()
                }
            ) {
                Text("Fetch data")
            }
        }
    }
}
