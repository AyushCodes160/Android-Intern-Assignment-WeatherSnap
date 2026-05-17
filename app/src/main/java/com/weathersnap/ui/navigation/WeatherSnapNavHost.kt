package com.weathersnap.ui.navigation

import android.net.Uri
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.weathersnap.ui.camera.CameraScreen
import com.weathersnap.ui.report.CreateReportScreen
import com.weathersnap.ui.report.CreateReportViewModel
import com.weathersnap.ui.saved.SavedReportsScreen
import com.weathersnap.ui.weather.WeatherScreen
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

private object Routes {
    const val WEATHER = "weather"
    const val REPORT = "report/{draftId}/{snapshotJson}"
    const val CAMERA = "camera"
    const val SAVED = "saved"

    fun report(draftId: String, snapshotJson: String): String =
        "report/$draftId/${Uri.encode(snapshotJson)}"
}

@Composable
fun WeatherSnapNavHost() {
    val navController = rememberNavController()
    val json = Json { ignoreUnknownKeys = true }

    NavHost(
        navController = navController,
        startDestination = Routes.WEATHER,
        enterTransition = { slideInHorizontally { it / 6 } + fadeIn() },
        exitTransition = { fadeOut() },
        popEnterTransition = { fadeIn() },
        popExitTransition = { slideOutHorizontally { it / 6 } + fadeOut() },
    ) {
        composable(Routes.WEATHER) {
            WeatherScreen(
                onOpenCreateReport = { snapshot ->
                    val draftId = UUID.randomUUID().toString()
                    val payload = json.encodeToString(snapshot)
                    navController.navigate(Routes.report(draftId, payload))
                },
                onOpenSavedReports = { navController.navigate(Routes.SAVED) },
            )
        }

        composable(
            route = Routes.REPORT,
            arguments = listOf(
                navArgument("draftId") { type = NavType.StringType },
                navArgument("snapshotJson") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val draftId = backStackEntry.arguments?.getString("draftId").orEmpty()
            val snapshotJson = backStackEntry.arguments?.getString("snapshotJson").orEmpty()

            // Read directly from this entry's SavedStateHandle (not the @HiltViewModel one
            // — they are different instances). The camera screen writes into this same
            // handle on capture, so the flow below emits as soon as the photo is taken.
            val capturedImagePath: String? by backStackEntry
                .savedStateHandle
                .getStateFlow(CreateReportViewModel.KEY_CAPTURED_IMAGE, null as String?)
                .collectAsStateWithLifecycle()

            CreateReportScreen(
                draftId = draftId,
                incomingSnapshotJson = snapshotJson,
                capturedImagePath = capturedImagePath,
                onCapturedImageConsumed = {
                    backStackEntry.savedStateHandle[CreateReportViewModel.KEY_CAPTURED_IMAGE] = null
                },
                onBack = { navController.popBackStack() },
                onOpenCamera = { navController.navigate(Routes.CAMERA) },
                onSaved = {
                    navController.navigate(Routes.SAVED) {
                        popUpTo(Routes.WEATHER) { inclusive = false }
                    }
                },
            )
        }

        composable(Routes.CAMERA) {
            CameraScreen(
                onClose = { navController.popBackStack() },
                onCaptured = { path ->
                    val reportEntry = navController.previousBackStackEntry
                    reportEntry
                        ?.savedStateHandle
                        ?.set(CreateReportViewModel.KEY_CAPTURED_IMAGE, path)
                    navController.popBackStack()
                },
            )
        }

        composable(Routes.SAVED) {
            SavedReportsScreen(onBack = { navController.popBackStack() })
        }
    }
}
