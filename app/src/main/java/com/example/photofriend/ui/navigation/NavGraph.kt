package com.example.photofriend.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.photofriend.ui.screen.aisuggestion.AISuggestionScreen
import com.example.photofriend.ui.screen.cameraselect.CameraSelectScreen
import com.example.photofriend.ui.screen.recipes.RecipesScreen
import com.example.photofriend.ui.screen.settings.CameraSettingsScreen
import com.example.photofriend.ui.screen.viewfinder.ViewfinderScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.CameraSelect.route
    ) {
        composable(Screen.CameraSelect.route) {
            CameraSelectScreen(
                onCameraSelected = { cameraId ->
                    navController.navigate(Screen.Viewfinder.createRoute(cameraId))
                },
                onRecipesClick = {
                    navController.navigate(Screen.Recipes.route)
                }
            )
        }

        composable(
            route = Screen.Viewfinder.route,
            arguments = listOf(navArgument("cameraId") { type = NavType.StringType })
        ) { backStackEntry ->
            val cameraId = backStackEntry.arguments?.getString("cameraId") ?: return@composable
            ViewfinderScreen(
                cameraId = cameraId,
                onBack = { navController.popBackStack() },
                onNavigateToSettings = { id ->
                    navController.navigate(Screen.CameraSettings.createRoute(id))
                },
                onAnalysisDone = {
                    navController.navigate(Screen.AISuggestion.route)
                }
            )
        }

        composable(
            route = Screen.CameraSettings.route,
            arguments = listOf(navArgument("cameraId") { type = NavType.StringType })
        ) { backStackEntry ->
            val cameraId = backStackEntry.arguments?.getString("cameraId") ?: return@composable
            CameraSettingsScreen(
                cameraId = cameraId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AISuggestion.route) {
            AISuggestionScreen(
                onBack = { navController.popBackStack() },
                onSaveRecipe = { navController.popBackStack() }
            )
        }

        composable(Screen.Recipes.route) {
            RecipesScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
