package com.weatherapp.ui.flow

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.weatherapp.ui.pages.AuthGateScreen
import com.weatherapp.ui.pages.HomeScreen
import com.weatherapp.ui.pages.InviteQrScreen
import com.weatherapp.ui.pages.OnboardingProfileScreen
import com.weatherapp.ui.pages.QrScannerScreen
import com.weatherapp.ui.pages.ResultScreen
import com.weatherapp.ui.pages.TrainingScreen

object MovementRoute {
    const val AuthGate = "movement_auth_gate"
    const val Onboarding = "movement_onboarding"
    const val Home = "movement_home"
    const val Training = "movement_training"
    const val TrainingArgs = "movement_training/{type}"
    const val Result = "movement_result"
    const val ResultArgs = "movement_result/{type}/{steps}/{points}/{streak}"
    const val InviteQr = "movement_invite_qr"
    const val ScanQr = "movement_scan_qr"
}

@Composable
fun MovementNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = MovementRoute.AuthGate,
        modifier = modifier
    ) {
        composable(MovementRoute.AuthGate) {
            AuthGateScreen(
                onGoHome = {
                    navController.navigate(MovementRoute.Home) {
                        popUpTo(MovementRoute.AuthGate) { inclusive = true }
                    }
                },
                onRequireOnboarding = {
                    navController.navigate(MovementRoute.Onboarding) {
                        popUpTo(MovementRoute.AuthGate) { inclusive = true }
                    }
                }
            )
        }
        composable(MovementRoute.Onboarding) {
            OnboardingProfileScreen(
                onComplete = {
                    navController.navigate(MovementRoute.Home) {
                        popUpTo(MovementRoute.Onboarding) { inclusive = true }
                    }
                }
            )
        }
        composable(MovementRoute.Home) {
            HomeScreen(
                onStartTraining = { type ->
                    navController.navigate("${MovementRoute.Training}/$type")
                },
                onShowInvite = { navController.navigate(MovementRoute.InviteQr) },
                onScanInvite = { navController.navigate(MovementRoute.ScanQr) }
            )
        }
        composable(
            MovementRoute.TrainingArgs,
            arguments = listOf(navArgument("type") { type = NavType.StringType })
        ) { backStackEntry ->
            val type = backStackEntry.arguments?.getString("type") ?: "walk"
            TrainingScreen(
                type = type,
                onFinish = { summary ->
                    navController.navigate(
                        "${MovementRoute.Result}/${summary.type}/${summary.steps}/${summary.points}/${summary.streak}"
                    ) {
                        popUpTo(MovementRoute.TrainingArgs) { inclusive = true }
                    }
                }
            )
        }
        composable(MovementRoute.InviteQr) {
            InviteQrScreen(onClose = { navController.popBackStack() })
        }
        composable(MovementRoute.ScanQr) {
            QrScannerScreen(
                onClose = { navController.popBackStack() },
                onPaired = {
                    navController.popBackStack(MovementRoute.Home, false)
                }
            )
        }
        composable(
            MovementRoute.ResultArgs,
            arguments = listOf(
                navArgument("type") { type = NavType.StringType },
                navArgument("steps") { type = NavType.IntType },
                navArgument("points") { type = NavType.IntType },
                navArgument("streak") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val steps = backStackEntry.arguments?.getInt("steps") ?: 0
            val points = backStackEntry.arguments?.getInt("points") ?: 0
            val streak = backStackEntry.arguments?.getInt("streak") ?: 0
            val type = backStackEntry.arguments?.getString("type") ?: "walk"
            ResultScreen(
                type = type,
                steps = steps,
                points = points,
                streak = streak,
                onRestart = { navController.popBackStack(MovementRoute.Home, false) }
            )
        }
    }
}
