package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.ChatConnectionState
import com.example.model.Country
import com.example.model.Gender
import com.example.viewmodel.VideoChatViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    if (FirebaseApp.getApps(context).isEmpty()) {
      FirebaseApp.initializeApp(
        context,
        FirebaseOptions.Builder()
          .setApplicationId("com.aistudio.vibetv.rxmflk")
          .setProjectId("gen-lang-client-0930602584")
          .setApiKey("AIzaSyAk_ONPKvyV-Qkf-7dWqhmoUY8o36jSj7E")
          .build()
      )
    }
  }

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("VibeTV", appName)
  }

  @Test
  fun `verify initial viewmodel state`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = VideoChatViewModel(app)
    val state = viewModel.uiState.value
    assertEquals(ChatConnectionState.IDLE, state.connectionState)
    assertEquals(Gender.MALE, state.selectedGender)
    assertEquals(Country.ALL, state.selectedCountry)
    assertTrue(state.isFrontCamera)
    assertTrue(state.isCameraOn)
    assertFalse(state.isKeyboardChatOpen)
    assertTrue(state.onlineUsersCount >= 1)
  }

  @Test
  fun `verify camera power toggle and keyboard chat toggle`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = VideoChatViewModel(app)
    assertTrue(viewModel.uiState.value.isCameraOn)

    // Toggle camera power off
    viewModel.toggleCameraPower()
    assertFalse(viewModel.uiState.value.isCameraOn)

    // Toggle keyboard chat open
    viewModel.toggleKeyboardChat()
    assertTrue(viewModel.uiState.value.isKeyboardChatOpen)

    viewModel.closeKeyboardChat()
    assertFalse(viewModel.uiState.value.isKeyboardChatOpen)
  }

  @Test
  fun `verify country filter changes, active filter display and reactions`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = VideoChatViewModel(app)
    viewModel.setGender(Gender.FEMALE)
    assertEquals(Gender.FEMALE, viewModel.uiState.value.selectedGender)

    val targetCountry = Country("BR", "Brazil", "🇧🇷", "Americas")
    viewModel.setCountry(targetCountry)
    assertEquals(targetCountry, viewModel.uiState.value.selectedCountry)
    assertTrue(viewModel.uiState.value.bannerNotice?.contains("Brazil") == true)

    // Reset filter
    viewModel.resetCountryFilter()
    assertEquals(Country.ALL, viewModel.uiState.value.selectedCountry)

    // Reaction burst
    viewModel.sendEmojiReaction("😂")
    assertTrue(viewModel.uiState.value.floatingReactions.size >= 3)
    assertEquals("😂", viewModel.uiState.value.floatingReactions[0].emoji)
  }
}
