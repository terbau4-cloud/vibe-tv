package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.Gender

// Primary Brand Colors from the screenshot
private val VibeRed = Color(0xFFFF1E56)
private val VibeRedSecondary = Color(0xFFFF473A)
private val VibeDarkBg = Color(0xFF0B0C10)
private val VibeInputBg = Color(0xFF13151F)
private val VibeInputBorder = Color(0xFF222638)

/**
 * Authentication Screen providing exact Login ("Welcome Back")
 * and Register ("Create Your Account") with strict Firebase credential validation.
 */
@Composable
fun AuthScreen(
    onLogin: (emailOrUsername: String, password: String, onResult: (Boolean, String?) -> Unit) -> Unit,
    onRegister: (name: String, emailOrUsername: String, password: String, gender: Gender, onResult: (Boolean, String?) -> Unit) -> Unit,
    onGoogleSignIn: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isRegisterMode by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VibeDarkBg)
    ) {
        // Red neon glow curves at bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color(0x33FF1E56),
                            Color(0x88FF1E56),
                            Color(0xEEFF1E56)
                        )
                    )
                )
        )

        AnimatedContent(
            targetState = isRegisterMode,
            transitionSpec = {
                if (targetState) {
                    (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                } else {
                    (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
                }
            },
            label = "auth_transition"
        ) { inRegister ->
            if (inRegister) {
                RegisterScreenContent(
                    onBackToLogin = { isRegisterMode = false },
                    onRegisterSubmit = onRegister
                )
            } else {
                LoginScreenContent(
                    onGoToRegister = { isRegisterMode = true },
                    onLoginSubmit = onLogin,
                    onGoogleSignIn = onGoogleSignIn
                )
            }
        }
    }
}

/**
 * Screen 1: Login ("Welcome Back")
 */
@Composable
private fun LoginScreenContent(
    onGoToRegister: () -> Unit,
    onLoginSubmit: (emailOrUsername: String, password: String, onResult: (Boolean, String?) -> Unit) -> Unit,
    onGoogleSignIn: (() -> Unit)? = null
) {
    var emailOrUsername by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val handleLoginAction = {
        val trimmedInput = emailOrUsername.trim()
        val trimmedPass = password.trim()
        if (trimmedInput.isBlank()) {
            errorMessage = "Please enter your email or username."
        } else if (trimmedPass.isBlank()) {
            errorMessage = "Please enter your password."
        } else {
            isLoading = true
            errorMessage = null
            onLoginSubmit(trimmedInput, trimmedPass) { success, error ->
                isLoading = false
                if (!success) {
                    errorMessage = error ?: "Incorrect credentials. Please try again."
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(18.dp))

        // Hero Header Collage with Vignette
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(20.dp))
        ) {
            Image(
                painter = painterResource(id = R.drawable.vibetv_auth_hero_1792413002725),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0x33000000), Color(0x990B0C10), VibeDarkBg)
                        )
                    )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Brand Logo: VIBE TV
        VibeTvBrandLogo()

        Spacer(modifier = Modifier.height(22.dp))

        // Welcome Back Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                text = "Welcome ",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Back",
                color = VibeRed,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Sign in to connect with real friends",
            color = Color(0xFF9E9EB0),
            fontSize = 13.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Error message banner if credentials fail
        if (errorMessage != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x22FF1E56))
                    .border(1.dp, VibeRed.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = "Error",
                    tint = VibeRed,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = errorMessage ?: "",
                    color = Color(0xFFFF8DA1),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Email or Username Input
        OutlinedTextField(
            value = emailOrUsername,
            onValueChange = {
                emailOrUsername = it
                errorMessage = null
            },
            placeholder = { Text("Email or Username", color = Color(0xFF70738A), fontSize = 14.sp) },
            leadingIcon = {
                Icon(Icons.Default.Mail, contentDescription = null, tint = Color(0xFF8E92A8), modifier = Modifier.size(20.dp))
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = VibeInputBg,
                unfocusedContainerColor = VibeInputBg,
                focusedBorderColor = VibeRed,
                unfocusedBorderColor = VibeInputBorder,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("login_email_input")
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Password Input with Visibility toggle
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                errorMessage = null
            },
            placeholder = { Text("Password", color = Color(0xFF70738A), fontSize = 14.sp) },
            leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF8E92A8), modifier = Modifier.size(20.dp))
            },
            trailingIcon = {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle password visibility",
                        tint = Color(0xFF8E92A8),
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { handleLoginAction() }),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = VibeInputBg,
                unfocusedContainerColor = VibeInputBg,
                focusedBorderColor = VibeRed,
                unfocusedBorderColor = VibeInputBorder,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("login_password_input")
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Remember me & Forgot Password Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { rememberMe = !rememberMe }
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (rememberMe) VibeRed else Color.Transparent)
                        .border(1.5.dp, if (rememberMe) VibeRed else Color(0xFF6E7288), RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (rememberMe) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Remember me",
                    color = Color(0xFFD0D0DC),
                    fontSize = 13.sp
                )
            }

            Text(
                text = "Forgot Password?",
                color = VibeRed,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable {
                    errorMessage = "Enter your registered email to reset password."
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Big Red Gradient Login Button
        Button(
            onClick = { handleLoginAction() },
            enabled = !isLoading,
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(
                    Brush.horizontalGradient(listOf(VibeRed, VibeRedSecondary)),
                    RoundedCornerShape(26.dp)
                )
                .shadow(12.dp, RoundedCornerShape(26.dp), spotColor = VibeRed)
                .testTag("submit_login_button")
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Authenticating...", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Login",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Optional Google Sign-In fallback
        if (onGoogleSignIn != null) {
            Button(
                onClick = onGoogleSignIn,
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2130)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("google_signin_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("G", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color(0xFF4285F4))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Continue with Google", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
        }

        // "———— OR ————" Divider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF282B3C), thickness = 1.dp)
            Text(
                text = "OR",
                color = Color(0xFF70738A),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 14.dp)
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF282B3C), thickness = 1.dp)
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Switch to Register
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(bottom = 28.dp)
        ) {
            Text(
                text = "Don't have an account? ",
                color = Color(0xFF9E9EB0),
                fontSize = 13.sp
            )
            Text(
                text = "Register",
                color = VibeRed,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { onGoToRegister() }
                    .testTag("goto_register_button")
            )
        }
    }
}

/**
 * Screen 2: Register ("Create Your Account")
 */
@Composable
private fun RegisterScreenContent(
    onBackToLogin: () -> Unit,
    onRegisterSubmit: (name: String, emailOrUsername: String, password: String, gender: Gender, onResult: (Boolean, String?) -> Unit) -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var emailAddress by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var selectedGender by remember { mutableStateOf(Gender.MALE) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val handleRegisterAction = {
        val trimmedName = fullName.trim()
        val trimmedEmail = emailAddress.trim()
        val trimmedPass = password.trim()

        if (trimmedName.isBlank()) {
            errorMessage = "Please enter your full name."
        } else if (trimmedEmail.isBlank()) {
            errorMessage = "Please enter an email address or username."
        } else if (trimmedPass.length < 6) {
            errorMessage = "Password must be at least 6 characters."
        } else {
            isLoading = true
            errorMessage = null
            onRegisterSubmit(trimmedName, trimmedEmail, trimmedPass, selectedGender) { success, error ->
                isLoading = false
                if (!success) {
                    errorMessage = error ?: "Registration failed. Please try again."
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(18.dp))

        // Top Row: Back button to return to Login
        Box(modifier = Modifier.fillMaxWidth()) {
            IconButton(
                onClick = onBackToLogin,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x33FFFFFF))
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Login",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Brand Logo: VIBE TV
        VibeTvBrandLogo()

        Spacer(modifier = Modifier.height(20.dp))

        // Create Your Account Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                text = "Create Your ",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Account",
                color = VibeRed,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Join now and connect with real users",
            color = Color(0xFF9E9EB0),
            fontSize = 13.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Error message banner if registration fails
        if (errorMessage != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x22FF1E56))
                    .border(1.dp, VibeRed.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = "Error",
                    tint = VibeRed,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = errorMessage ?: "",
                    color = Color(0xFFFF8DA1),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Full Name Input
        OutlinedTextField(
            value = fullName,
            onValueChange = {
                fullName = it
                errorMessage = null
            },
            placeholder = { Text("Full Name", color = Color(0xFF70738A), fontSize = 14.sp) },
            leadingIcon = {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF8E92A8), modifier = Modifier.size(20.dp))
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = VibeInputBg,
                unfocusedContainerColor = VibeInputBg,
                focusedBorderColor = VibeRed,
                unfocusedBorderColor = VibeInputBorder,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("register_fullname_input")
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Email Address Input
        OutlinedTextField(
            value = emailAddress,
            onValueChange = {
                emailAddress = it
                errorMessage = null
            },
            placeholder = { Text("Email Address or Username", color = Color(0xFF70738A), fontSize = 14.sp) },
            leadingIcon = {
                Icon(Icons.Default.Mail, contentDescription = null, tint = Color(0xFF8E92A8), modifier = Modifier.size(20.dp))
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = VibeInputBg,
                unfocusedContainerColor = VibeInputBg,
                focusedBorderColor = VibeRed,
                unfocusedBorderColor = VibeInputBorder,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("register_email_input")
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Password Input
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                errorMessage = null
            },
            placeholder = { Text("Password (min 6 characters)", color = Color(0xFF70738A), fontSize = 14.sp) },
            leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF8E92A8), modifier = Modifier.size(20.dp))
            },
            trailingIcon = {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle password visibility",
                        tint = Color(0xFF8E92A8),
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = VibeInputBg,
                unfocusedContainerColor = VibeInputBg,
                focusedBorderColor = VibeRed,
                unfocusedBorderColor = VibeInputBorder,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("register_password_input")
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Gender Selector Section
        Text(
            text = "Gender",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Male Pill
            GenderSelectionPill(
                label = "Male",
                symbol = "♂",
                isSelected = selectedGender == Gender.MALE,
                modifier = Modifier.weight(1f),
                onClick = { selectedGender = Gender.MALE }
            )

            // Female Pill
            GenderSelectionPill(
                label = "Female",
                symbol = "♀",
                isSelected = selectedGender == Gender.FEMALE,
                modifier = Modifier.weight(1f),
                onClick = { selectedGender = Gender.FEMALE }
            )

            // Other Pill
            GenderSelectionPill(
                label = "Other",
                symbol = "⚧",
                isSelected = selectedGender == Gender.NON_BINARY,
                modifier = Modifier.weight(1f),
                onClick = { selectedGender = Gender.NON_BINARY }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Big Red Gradient Register Button
        Button(
            onClick = { handleRegisterAction() },
            enabled = !isLoading,
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(
                    Brush.horizontalGradient(listOf(VibeRed, VibeRedSecondary)),
                    RoundedCornerShape(26.dp)
                )
                .shadow(12.dp, RoundedCornerShape(26.dp), spotColor = VibeRed)
                .testTag("submit_register_button")
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Registering...", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Register",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Switch to Login
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(bottom = 28.dp)
        ) {
            Text(
                text = "Already have an account? ",
                color = Color(0xFF9E9EB0),
                fontSize = 13.sp
            )
            Text(
                text = "Login",
                color = VibeRed,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { onBackToLogin() }
                    .testTag("goto_login_button")
            )
        }
    }
}

/**
 * Gender Selection Pill button matching the screenshot
 */
@Composable
private fun GenderSelectionPill(
    label: String,
    symbol: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) VibeRed else VibeInputBg)
            .border(
                1.5.dp,
                if (isSelected) VibeRed else VibeInputBorder,
                RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = symbol,
                color = if (isSelected) Color.White else Color(0xFF9E9EB0),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = if (isSelected) Color.White else Color(0xFF9E9EB0),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Centered VIBE TV Logo matching exact screenshot design:
 * Red rounded TV set with 2 cute antennas and white play triangle inside + VIBE (white) TV (red) + MEET • CHAT • CONNECT
 */
@Composable
private fun VibeTvBrandLogo(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Neon Red TV Box with Play Triangle
            Box(
                modifier = Modifier
                    .size(46.dp, 36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(VibeRed)
                    .shadow(8.dp, RoundedCornerShape(10.dp), spotColor = VibeRed),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text: VIBE TV
            Text(
                text = "VIBE ",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Text(
                text = "TV",
                color = VibeRed,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "MEET   •   CHAT   •   CONNECT",
            color = Color(0xFFB0B0C4),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.sp
        )
    }
}
