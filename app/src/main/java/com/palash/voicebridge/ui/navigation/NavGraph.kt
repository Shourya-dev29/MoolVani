package com.palash.voicebridge.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.palash.voicebridge.ui.curriculum.CurriculumExplorerScreen
import com.palash.voicebridge.ui.flashcards.FlashcardGeneratorScreen
import com.palash.voicebridge.ui.home.HomeScreen
import com.palash.voicebridge.ui.settings.SettingsScreen
import com.palash.voicebridge.ui.translator.LiveTranslatorScreen
import com.palash.voicebridge.ui.worksheets.WorksheetGeneratorScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object LiveTranslator : Screen("live_translator")
    object CurriculumExplorer : Screen("curriculum_explorer")
    object WorksheetGenerator : Screen("worksheet_generator")
    object FlashcardGenerator : Screen("flashcard_generator")
    object Settings : Screen("settings")
}

@Composable
fun PalashNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToTranslator = { navController.navigate(Screen.LiveTranslator.route) },
                onNavigateToWorksheets = { navController.navigate(Screen.WorksheetGenerator.route) },
                onNavigateToFlashcards = { navController.navigate(Screen.FlashcardGenerator.route) },
                onNavigateToCurriculum = { navController.navigate(Screen.CurriculumExplorer.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }
        composable(Screen.LiveTranslator.route) {
            LiveTranslatorScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.CurriculumExplorer.route) {
            CurriculumExplorerScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.WorksheetGenerator.route) {
            WorksheetGeneratorScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.FlashcardGenerator.route) {
            FlashcardGeneratorScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
