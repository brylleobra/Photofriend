package com.example.photofriend.ui.navigation

sealed class Screen(val route: String) {
    object CameraSelect : Screen("camera_select")
    object Viewfinder : Screen("viewfinder/{cameraId}") {
        fun createRoute(cameraId: String) = "viewfinder/$cameraId"
    }
    object CameraSettings : Screen("camera_settings/{cameraId}") {
        fun createRoute(cameraId: String) = "camera_settings/$cameraId"
    }
    object AISuggestion : Screen("ai_suggestion")
    object Recipes : Screen("recipes")
    object RecipeDetail : Screen("recipe_detail")
}
