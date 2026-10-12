package com.example.myapplication3.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication3.data.remote.AuthService
import com.example.myapplication3.ui.theme.*
import kotlinx.coroutines.launch

// Helper to provide clear white text and visible placeholder styling across all text fields
@Composable
private fun authTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextWhite,
    unfocusedTextColor = TextWhite,
    focusedLabelColor = PrimaryPurple,
    unfocusedLabelColor = TextMuted,
    cursorColor = TextWhite,
    focusedBorderColor = PrimaryPurple,
    unfocusedBorderColor = CardBorder,
    focusedContainerColor = CardDark,
    unfocusedContainerColor = CardDark
)

@Composable
fun OnboardingScreen(onNavigateToAuth: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("StockIt", fontSize = 42.sp, fontWeight = FontWeight.Bold, color = PrimaryPurple)
        Spacer(modifier = Modifier.height(12.dp))
        Text("Track & trade Indian markets in real time", fontSize = 16.sp, color = TextMuted)
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onNavigateToAuth,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Get Started", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextWhite)
        }
    }
}

@Composable
fun SignInScreen(
    authService: AuthService? = null,
    onSignInSuccess: () -> Unit,
    onNavigateToSignUp: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Welcome Back", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = TextWhite)
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            colors = authTextFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            colors = authTextFieldColors(),
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(errorMessage!!, color = AccentRed, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                if (authService != null && email.isNotBlank() && password.isNotBlank()) {
                    isLoading = true
                    errorMessage = null
                    scope.launch {
                        val result = authService.signIn(email, password)
                        isLoading = false
                        if (result.isSuccess) {
                            onSignInSuccess()
                        } else {
                            errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Sign in failed"
                        }
                    }
                } else {
                    onSignInSuccess()
                }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = TextWhite, modifier = Modifier.size(24.dp))
            } else {
                Text("Sign In", fontWeight = FontWeight.Bold, color = TextWhite)
            }
        }
        TextButton(onClick = onNavigateToSignUp, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Don't have an account? Sign Up", color = TextMuted)
        }
    }
}

@Composable
fun SignUpScreen(
    authService: AuthService? = null,
    onSignUpSuccess: () -> Unit,
    onNavigateToSignIn: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Create Account", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = TextWhite)
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Full Name") },
            colors = authTextFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            colors = authTextFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            colors = authTextFieldColors(),
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(errorMessage!!, color = AccentRed, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                if (authService != null && email.isNotBlank() && password.isNotBlank()) {
                    isLoading = true
                    errorMessage = null
                    scope.launch {
                        val result = authService.signUp(email, password, name.ifBlank { "User" })
                        isLoading = false
                        if (result.isSuccess) {
                            onSignUpSuccess()
                        } else {
                            errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Sign up failed"
                        }
                    }
                } else {
                    onSignUpSuccess()
                }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = TextWhite, modifier = Modifier.size(24.dp))
            } else {
                Text("Register", fontWeight = FontWeight.Bold, color = TextWhite)
            }
        }
        TextButton(onClick = onNavigateToSignIn, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Already registered? Sign In", color = TextMuted)
        }
    }
}

@Composable
fun AuthSuccessScreen(onProceedToDashboard: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("You're In!", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = AccentGreen)
        Spacer(modifier = Modifier.height(12.dp))
        Text("Your demo trading wallet has ₹9,80,763.06", color = TextMuted)
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onProceedToDashboard,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Enter Terminal", fontWeight = FontWeight.Bold, color = TextWhite)
        }
    }
}