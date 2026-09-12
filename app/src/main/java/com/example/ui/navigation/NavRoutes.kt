package com.example.ui.navigation

sealed class Screen(val route: String, val title: String) {
    object Auth : Screen("auth", "Authentication")
    object Home : Screen("home", "Simulations")
    object NewDecision : Screen("new_decision", "New Life Decision")
    object SimulationResult : Screen("simulation_result", "Outcome Visualization")
    object MoodTracker : Screen("mood_tracker", "Well-Being")
    object AdvisorChat : Screen("advisor_chat", "AI Advisor")
    object CloudBackup : Screen("cloud_backup", "Cloud Vault")
}
