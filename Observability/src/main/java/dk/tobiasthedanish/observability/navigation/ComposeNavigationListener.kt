package dk.tobiasthedanish.observability.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import dk.tobiasthedanish.observability.Observability

@Composable
fun NavHostController.withObservabilityListener(): NavHostController {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, this) {
        val listener = ComposeNavigationListener(
            this@withObservabilityListener
        )

        lifecycle.addObserver(listener)

        onDispose {
            listener.dispose()
            lifecycle.removeObserver(listener)
        }
    }

    return this
}

private class ComposeNavigationListener(
    private val navController: NavHostController
) : LifecycleEventObserver {
    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (event == Lifecycle.Event.ON_RESUME) {
            navController.addOnDestinationChangedListener(destinationChangedListener)
        } else if (event == Lifecycle.Event.ON_PAUSE) {
            navController.removeOnDestinationChangedListener(destinationChangedListener)
        }
    }

    fun dispose() {
        navController.removeOnDestinationChangedListener(destinationChangedListener)
    }

    private val destinationChangedListener =
        NavController.OnDestinationChangedListener { controller, _, _ ->
            controller.currentDestination?.route?.let { to ->
                Observability.onNavigation(to)
            }
        }
}