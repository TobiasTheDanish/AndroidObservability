package dk.tobiasthedanish.observabilitydemo

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dk.tobiasthedanish.observabilitydemo.ui.theme.ObservabilityDemoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            Log.e("ExceptionHandler", "thread: ${thread.name}, exception: $exception, message: ${exception.message}")
        }

        enableEdgeToEdge()
        setContent {
            ObservabilityDemoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = null,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String?, modifier: Modifier = Modifier) {
    Text(
        text = name!!,
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ObservabilityDemoTheme {
        Greeting("Android")
    }
}