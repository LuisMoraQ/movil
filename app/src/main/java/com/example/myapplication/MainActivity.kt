package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.screens.LoginScreen
import com.example.myapplication.screens.MainScreen
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.utils.PreferencesManager
import com.example.myapplication.viewmodel.LoginViewModel
import com.example.myapplication.viewmodel.LoginViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") {
            val viewModel: LoginViewModel = viewModel(
                factory = LoginViewModelFactory(context)
            )
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = {
                    // Navegar a MainScreen con el nombre del usuario
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("main") {
            // Recuperar datos del usuario desde Preferences
            val preferencesManager = PreferencesManager(context)
            val userName = preferencesManager.getUserName()

            MainScreen(
                userName = userName,
                onLogout = {
                    // Limpiar sesión y volver a login
                    preferencesManager.clearSession()
                    navController.navigate("login") {
                        popUpTo("main") { inclusive = true }
                    }
                }
            )
        }
    }
}