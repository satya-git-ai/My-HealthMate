package com.example.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.components.HealthBottomNavigation
import com.example.ui.screens.DayDetailScreen
import com.example.ui.screens.GoalsScreen
import com.example.ui.screens.GpsLocationPermissionScreen
import com.example.ui.screens.GpsTrackerScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MedHistoryScreen
import com.example.ui.screens.MedReminderScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.RouteDetailScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.StatisticsScreen
import com.example.ui.screens.StepCounterScreen
import com.example.ui.screens.StopwatchScreen
import com.example.ui.screens.WaterTrackerScreen
import com.example.ui.screens.WeightLossCalculatorScreen
import com.example.ui.viewmodel.HealthViewModel

@Composable
fun HealthFitApp(
    viewModel: HealthViewModel = viewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "splash"
    val userSettings by viewModel.userSettings.collectAsState()

    val bottomNavRoutes = setOf(
        "home",
        "step_counter",
        "gps_tracker",
        "water_tracker",
        "history",
        "statistics",
        "goals",
        "settings",
        "med_reminder",
        "med_history",
        "stopwatch"
    )

    val showBottomBar = currentRoute in bottomNavRoutes

    val navigateHome = {
        val popped = navController.popBackStack("home", inclusive = false)
        if (!popped && currentRoute != "home") {
            navController.navigate("home") {
                popUpTo("home") {
                    inclusive = true
                }
                launchSingleTop = true
            }
        }
    }

    val navigateBack = {
        if (!navController.popBackStack()) {
            navigateHome()
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                HealthBottomNavigation(
                    currentRoute = currentRoute,
                    onNavigate = { targetRoute ->
                        if (targetRoute == "home") {
                            navigateHome()
                        } else if (targetRoute != currentRoute) {
                            navController.navigate(targetRoute) {
                                popUpTo("home") {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "splash",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Splash Screen
            composable("splash") {
                SplashScreen(
                    onSplashFinished = {
                        val destination = if (!userSettings.hasCompletedOnboarding) "onboarding" else "home"
                        navController.navigate(destination) {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                )
            }

            // 2. Onboarding Screen (Feature overview & profile customization)
            composable("onboarding") {
                OnboardingScreen(
                    userSettings = userSettings,
                    initialStepIndex = 0,
                    onSaveProfileAndFinish = { weightKg, heightCm, age, gender, goal, targetWeightKg ->
                        viewModel.updateUserProfile(
                            weightKg = weightKg,
                            heightCm = heightCm,
                            age = age,
                            gender = gender,
                            primaryGoal = goal,
                            targetWeightKg = targetWeightKg,
                            autoCalculateGoals = true
                        )
                        viewModel.completeOnboarding()
                        navController.navigate("home") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    }
                )
            }

            // 2b. Direct Profile Personalization / Customize Settings Screen
            composable("personalize_plan") {
                OnboardingScreen(
                    userSettings = userSettings,
                    initialStepIndex = 1,
                    onBack = if (userSettings.hasCompletedOnboarding) { { navController.popBackStack() } } else null,
                    onSaveProfileAndFinish = { weightKg, heightCm, age, gender, goal, targetWeightKg ->
                        viewModel.updateUserProfile(
                            weightKg = weightKg,
                            heightCm = heightCm,
                            age = age,
                            gender = gender,
                            primaryGoal = goal,
                            targetWeightKg = targetWeightKg,
                            autoCalculateGoals = true
                        )
                        viewModel.completeOnboarding()
                        if (userSettings.hasCompletedOnboarding) {
                            navController.popBackStack()
                        } else {
                            navController.navigate("home") {
                                popUpTo("personalize_plan") { inclusive = true }
                            }
                        }
                    }
                )
            }

            // 3. Dedicated GPS & Location Permission Asking Screen (Transparent Glossy)
            composable("gps_permission") {
                GpsLocationPermissionScreen(
                    onPermissionGranted = {
                        navController.popBackStack()
                    },
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }

            // 4. Home Dashboard Screen
            composable("home") {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigate = { route -> navController.navigate(route) }
                )
            }

            // 5. Step Counter Screen
            composable("step_counter") {
                StepCounterScreen(
                    viewModel = viewModel,
                    onBack = { navigateBack() },
                    onHome = { navigateHome() }
                )
            }

            // 6. GPS Walking / Running Tracker Screen
            composable("gps_tracker") {
                GpsTrackerScreen(
                    viewModel = viewModel,
                    onBack = { navigateBack() },
                    onViewRouteDetail = { workoutId ->
                        navController.navigate("route_detail/$workoutId")
                    },
                    onHome = { navigateHome() },
                    onOpenGpsPermissionScreen = {
                        navController.navigate("gps_permission")
                    }
                )
            }

            // 7. Route Detail Screen
            composable(
                route = "route_detail/{workoutId}",
                arguments = listOf(navArgument("workoutId") { type = NavType.LongType })
            ) { backStackEntry ->
                val workoutId = backStackEntry.arguments?.getLong("workoutId") ?: 0L
                RouteDetailScreen(
                    workoutId = workoutId,
                    viewModel = viewModel,
                    onBack = { navigateBack() },
                    onHome = { navigateHome() }
                )
            }

            // 8. Water Tracker Screen
            composable("water_tracker") {
                WaterTrackerScreen(
                    viewModel = viewModel,
                    onBack = { navigateBack() },
                    onHome = { navigateHome() }
                )
            }

            // 9. History Screen
            composable("history") {
                HistoryScreen(
                    viewModel = viewModel,
                    onBack = { navigateBack() },
                    onSelectDay = { date ->
                        navController.navigate("day_detail/$date")
                    },
                    onViewWorkout = { workoutId ->
                        navController.navigate("route_detail/$workoutId")
                    },
                    onHome = { navigateHome() }
                )
            }

            // 11. Day Detail Screen
            composable(
                route = "day_detail/{date}",
                arguments = listOf(navArgument("date") { type = NavType.StringType })
            ) { backStackEntry ->
                val date = backStackEntry.arguments?.getString("date") ?: viewModel.todayDate
                DayDetailScreen(
                    date = date,
                    viewModel = viewModel,
                    onBack = { navigateBack() },
                    onViewWorkout = { workoutId ->
                        navController.navigate("route_detail/$workoutId")
                    },
                    onHome = { navigateHome() }
                )
            }

            // 12. Statistics Screen
            composable("statistics") {
                StatisticsScreen(
                    viewModel = viewModel,
                    onBack = { navigateBack() },
                    onHome = { navigateHome() }
                )
            }

            // 13. Goals Screen
            composable("goals") {
                GoalsScreen(
                    viewModel = viewModel,
                    onBack = { navigateBack() },
                    onHome = { navigateHome() }
                )
            }

            // 14. Settings Screen
            composable("settings") {
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = { navigateBack() },
                    onNavigateGoals = { navController.navigate("goals") },
                    onNavigatePermissions = { navController.navigate("gps_permission") },
                    onNavigateCalculator = { navController.navigate("weight_loss_calculator") },
                    onNavigateProfile = { navController.navigate("onboarding") },
                    onHome = { navigateHome() }
                )
            }

            // 15. Med reminder Screen
            composable("med_reminder") {
                MedReminderScreen(
                    viewModel = viewModel,
                    onBack = { navigateBack() },
                    onHome = { navigateHome() },
                    onNavigateHistory = { navController.navigate("med_history") }
                )
            }

            // 16. Med History Screen
            composable("med_history") {
                MedHistoryScreen(
                    viewModel = viewModel,
                    onBack = { navigateBack() },
                    onHome = { navigateHome() }
                )
            }

            // 17. Stopwatch Screen
            composable("stopwatch") {
                StopwatchScreen(
                    onBack = { navigateBack() },
                    onHome = { navigateHome() }
                )
            }

            // 18. Weight Loss Walking Calculator Screen (GetSteps.app Engine)
            composable("calculator") {
                WeightLossCalculatorScreen(
                    userSettings = userSettings,
                    onApplyStepGoal = { newGoal ->
                        viewModel.updateStepGoal(newGoal)
                    },
                    onNavigateBack = { navigateBack() }
                )
            }

            composable("weight_loss_calculator") {
                WeightLossCalculatorScreen(
                    userSettings = userSettings,
                    onApplyStepGoal = { newGoal ->
                        viewModel.updateStepGoal(newGoal)
                    },
                    onNavigateBack = { navigateBack() }
                )
            }
        }
    }
}
