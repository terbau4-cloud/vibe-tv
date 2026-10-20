package com.example.model

enum class Gender(val label: String, val emoji: String) {
    MALE("I AM", "👦"),
    FEMALE("I AM", "👩"),
    NON_BINARY("I AM", "🧑")
}

data class Country(
    val code: String,
    val name: String,
    val flag: String,
    val region: String = "Global"
) {
    companion object {
        val ALL = Country("ALL", "ALL COUNTRIES", "🌎", "Global")

        val POPULAR_COUNTRIES = listOf(
            ALL,
            Country("US", "United States", "🇺🇸", "Americas"),
            Country("IN", "India", "🇮🇳", "Asia"),
            Country("BR", "Brazil", "🇧🇷", "Americas"),
            Country("GB", "United Kingdom", "🇬🇧", "Europe"),
            Country("DE", "Germany", "🇩🇪", "Europe"),
            Country("FR", "France", "🇫🇷", "Europe"),
            Country("JP", "Japan", "🇯🇵", "Asia"),
            Country("KR", "South Korea", "🇰🇷", "Asia"),
            Country("CA", "Canada", "🇨🇦", "Americas"),
            Country("AU", "Australia", "🇦🇺", "Oceania"),
            Country("ES", "Spain", "🇪🇸", "Europe"),
            Country("IT", "Italy", "🇮🇹", "Europe"),
            Country("MX", "Mexico", "🇲🇽", "Americas"),
            Country("PH", "Philippines", "🇵🇭", "Asia"),
            Country("ID", "Indonesia", "🇮🇩", "Asia"),
            Country("TR", "Turkey", "🇹🇷", "Europe")
        )
    }
}

enum class ChatConnectionState {
    IDLE,
    SEARCHING,
    CONNECTED,
    ENDED
}

data class PartnerProfile(
    val id: String,
    val name: String,
    val age: Int = 22,
    val country: Country = Country("NP", "Nepal", "🇳🇵", "Asia"),
    val bio: String = "Active Real Device User",
    val avatarBgColor: Long = 0xFF880E4F,
    val avatarEmoji: String = "✨",
    val videoTone: String = "neon",
    val greetings: List<String> = listOf("Hello! Connected from my mobile phone!"),
    val chatReplies: Map<String, String> = emptyMap(),
    val photoUrl: String = "",
    val isRealDevice: Boolean = true
)

data class ChatMessage(
    val id: String,
    val senderIsMe: Boolean,
    val senderName: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class FloatingReaction(
    val id: Long,
    val emoji: String,
    val startXRatio: Float,
    val startYRatio: Float = 0.82f,
    val sizeSp: Int = 34,
    val rotationAngle: Float = 0f,
    val createdAt: Long = System.currentTimeMillis()
)
