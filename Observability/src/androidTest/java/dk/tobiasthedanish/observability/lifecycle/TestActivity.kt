package dk.tobiasthedanish.observability.lifecycle

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dk.tobiasthedanish.observability.navigation.withObservabilityListener

class TestActivity: ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController().withObservabilityListener()

            NavHost(navController = navController, startDestination = "primary") {
                composable("primary") {
                    PrimaryScreen(navController)
                }
                composable("secondary") {
                    SecondaryScreen(navController)
                }
            }
        }
    }
}

@Composable
fun PrimaryScreen(navController: NavHostController) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).fillMaxSize()
        ) {
            Text("PrimaryScreen")
            Button(
                onClick = {
                    Log.d("PrimaryScreen", "Navigate button clicked")
                    navController.navigate("secondary")
                }
            ) {
                Text("Navigate")
            }
        }
    }
}
@Composable
fun SecondaryScreen(navController: NavHostController) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).fillMaxSize()
        ) {
            Text("SecondaryScreen")
            Button(
                onClick = {
                    Log.d("SecondaryScreen", "Navigate button clicked")
                    navController.navigate("primary")
                }
            ) {
                Text("Navigate")
            }
        }
    }
}
