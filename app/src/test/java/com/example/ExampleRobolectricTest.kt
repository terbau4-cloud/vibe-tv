package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.ChatConnectionState
import com.example.model.Country
import com.example.model.Gender
import com.example.viewmodel.VideoChatViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("VibeTV", appName)
  }

  @Test
  fun `verify initial viewmodel state`() {
    val viewModel = VideoChatViewModel()
    val state = viewModel.uiState.value
    assertEquals(ChatConnectionState.IDLE, state.connectionState)
    assertEquals(Gender.MALE, state.selectedGender)
    assertEquals(Country.ALL, state.selectedCountry)
    assertTrue(state.isFrontCamera)
    assertTrue(state.isCameraOn)
    assertFalse(state.isKeyboardChatOpen)
    assertTrue(state.onlineUsersCount > 200000)
  }

  @Test
  fun `verify camera power toggle and keyboard chat toggle`() {
    val viewModel = VideoChatViewModel()
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
    val viewModel = VideoChatViewModel()
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
