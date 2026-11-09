package io.github.cfsima.modernsafe.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import io.github.cfsima.modernsafe.AuthManager

/**
 * Main application navigation host for ModernSafe.
 * Maps top-level routes to their modularized destination graphs.
 */
@Composable
fun ModernSafeNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: Any = if (AuthManager.isSignedIn()) CategoryListRoute else AskPasswordRoute(isLocal = true),
    onFinishActivity: () -> Unit = {},
    onSetResultOkAndFinish: () -> Unit = {},
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        askPasswordDestination(navController, onFinishActivity, onSetResultOkAndFinish)
        categoryListDestination(navController)
        passListDestination(navController)
        passViewDestination(navController)
        passEditDestination(navController)
        passGenDestination(navController)
        searchDestination(navController)
        changePassDestination(navController)
        settingsDestination(navController)
        restoreDestination(navController)
        helpDestination(navController)
        logOffDestination(navController)
    }
}
