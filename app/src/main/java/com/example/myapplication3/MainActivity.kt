package com.example.myapplication3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.myapplication3.data.domain.Stock
import com.example.myapplication3.data.local.AppDatabase
import com.example.myapplication3.data.remote.AuthService
import com.example.myapplication3.data.repository.StockRepository
import com.example.myapplication3.screens.*
import com.example.myapplication3.ui.theme.BgDark
import com.example.myapplication3.ui.theme.CardDark
import com.example.myapplication3.ui.theme.PrimaryPurple
import com.example.myapplication3.ui.theme.TextMuted

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext)
        val repository = StockRepository(database)
        val authService = AuthService()

        setContent {
            StockItApp(repository = repository, authService = authService)
        }
    }
}

sealed class Screen {
    object Onboarding : Screen()
    object SignIn : Screen()
    object SignUp : Screen()
    object AuthSuccess : Screen()
    object Main : Screen()
}

enum class MainTab { HOME, PORTFOLIO }

@Composable
fun StockItApp(repository: StockRepository, authService: AuthService) {
    val startScreen = if (authService.isUserLoggedIn) Screen.Main else Screen.Onboarding
    var currentScreen by remember { mutableStateOf<Screen>(startScreen) }
    var currentTab by remember { mutableStateOf(MainTab.HOME) }
    var selectedStock by remember { mutableStateOf<Stock?>(null) }

    val liveStocks by repository.stockFeed.collectAsState(initial = emptyList())

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        when (currentScreen) {
            Screen.Onboarding -> {
                OnboardingScreen(
                    onNavigateToAuth = { currentScreen = Screen.SignIn }
                )
            }
            Screen.SignIn -> {
                SignInScreen(
                    authService = authService,
                    onSignInSuccess = { currentScreen = Screen.AuthSuccess },
                    onNavigateToSignUp = { currentScreen = Screen.SignUp }
                )
            }
            Screen.SignUp -> {
                SignUpScreen(
                    authService = authService,
                    onSignUpSuccess = { currentScreen = Screen.AuthSuccess },
                    onNavigateToSignIn = { currentScreen = Screen.SignIn }
                )
            }
            Screen.AuthSuccess -> {
                AuthSuccessScreen(
                    onProceedToDashboard = { currentScreen = Screen.Main }
                )
            }
            Screen.Main -> {
                if (selectedStock != null) {
                    val activeStock = liveStocks.find { it.symbol == selectedStock!!.symbol } ?: selectedStock!!
                    StockDetailScreen(
                        stock = activeStock,
                        repository = repository,
                        authService = authService,
                        onBack = { selectedStock = null }
                    )
                } else {
                    Scaffold(
                        bottomBar = {
                            NavigationBar(
                                containerColor = CardDark,
                                contentColor = Color.White
                            ) {
                                NavigationBarItem(
                                    selected = currentTab == MainTab.HOME,
                                    onClick = { currentTab = MainTab.HOME },
                                    icon = { Icon(Icons.Default.TrendingUp, contentDescription = "Markets") },
                                    label = { Text("Markets") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = PrimaryPurple,
                                        selectedTextColor = PrimaryPurple,
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted,
                                        indicatorColor = Color.Transparent
                                    )
                                )
                                NavigationBarItem(
                                    selected = currentTab == MainTab.PORTFOLIO,
                                    onClick = { currentTab = MainTab.PORTFOLIO },
                                    icon = { Icon(Icons.Default.AccountCircle, contentDescription = "Portfolio") },
                                    label = { Text("Portfolio") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = PrimaryPurple,
                                        selectedTextColor = PrimaryPurple,
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted,
                                        indicatorColor = Color.Transparent
                                    )
                                )
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(BgDark)
                                .padding(innerPadding)
                        ) {
                            when (currentTab) {
                                MainTab.HOME -> {
                                    HomeScreen(
                                        stocks = liveStocks,
                                        repository = repository,
                                        onSelectStock = { stock -> selectedStock = stock }
                                    )
                                }
                                MainTab.PORTFOLIO -> {
                                    ProfileScreen(
                                        stocks = liveStocks,
                                        repository = repository,
                                        authService = authService,
                                        onSignOut = { currentScreen = Screen.SignIn }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}