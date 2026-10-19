package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.VoiceConversationManager
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.MainChatScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.VideoChatViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: VideoChatViewModel by viewModels()
    private var voiceManager: VoiceConversationManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        voiceManager = VoiceConversationManager(this)
        viewModel.onSpeakRequested = { text, countryCode ->
            voiceManager?.speak(text, countryCode)
        }

        setContent {
            MyApplicationTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                ) { innerPadding ->
                    if (!uiState.isUserSignedIn) {
                        // "continue with google hatau ani login and register page hala yesto bala"
                        AuthScreen(
                            onAuthSuccess = { name, email, gender ->
                                viewModel.authenticateUser(
                                    name = name,
                                    email = email,
                                    gender = gender
                                )
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        MainChatScreen(
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceManager?.release()
        voiceManager = null
    }
}
