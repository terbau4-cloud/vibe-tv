package com.example.data

import com.example.model.Country
import com.example.model.Gender
import com.example.model.PartnerProfile
import kotlin.random.Random

object PartnerRepository {

    val samplePartners = listOf(
        PartnerProfile(
            id = "p1",
            name = "Lucas",
            age = 22,
            country = Country("BR", "Brazil", "🇧🇷", "Americas"),
            bio = "Samba & chill ⚽ Looking for good vibes",
            avatarBgColor = 0xFF1B5E20,
            avatarEmoji = "😎",
            videoTone = "sunset",
            photoUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=800&q=80",
            greetings = listOf(
                "Olá! Tudo bem? Where are you from?",
                "Hey there! What time is it where you are?",
                "Oi! Having a great day?"
            ),
            chatReplies = mapOf(
                "hi" to "Hey buddy! How's it going?",
                "hello" to "Hello! Having fun on VibeTV?",
                "where are you from" to "I'm in Rio de Janeiro, Brazil! 🌴 Where are you?",
                "cool" to "Totally! Always love meeting new people here.",
                "default" to "Haha that's nice! How long have you been chatting today?"
            )
        ),
        PartnerProfile(
            id = "p2",
            name = "Aarav",
            age = 24,
            country = Country("IN", "India", "🇮🇳", "Asia"),
            bio = "Techie & music lover 🎧 Let's chat!",
            avatarBgColor = 0xFFE65100,
            avatarEmoji = "✌️",
            videoTone = "warm",
            photoUrl = "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=800&q=80",
            greetings = listOf(
                "Namaste! Hi from Mumbai! How are you?",
                "Hey! Nice to connect with you!",
                "Hello friend! How is your day going?"
            ),
            chatReplies = mapOf(
                "hi" to "Namaste! Hope you're having an awesome day!",
                "hello" to "Hello! What are you up to right now?",
                "where are you from" to "I'm from Mumbai, India! 🇮🇳 What country are you in?",
                "how are you" to "Doing great, just chilling with some coffee! You?",
                "default" to "Sounds great! Do you chat on video apps often?"
            )
        ),
        PartnerProfile(
            id = "p3",
            name = "Emma",
            age = 21,
            country = Country("US", "United States", "🇺🇸", "Americas"),
            bio = "Art student from NYC 🎨 Coffee enthusiast",
            avatarBgColor = 0xFF4A148C,
            avatarEmoji = "✨",
            videoTone = "neon",
            photoUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=800&q=80",
            greetings = listOf(
                "Hey! What's up? I like your camera setup!",
                "Hi there! NYC checking in 🗽",
                "Hello! How are things on your side?"
            ),
            chatReplies = mapOf(
                "hi" to "Hey!! So cool to meet you!",
                "hello" to "Hi! What's the weather like there?",
                "where are you from" to "I'm in New York City! 🗽 You?",
                "nice" to "Thanks so much! Your vibes are great!",
                "default" to "Haha love that! Tell me something interesting about your day!"
            )
        ),
        PartnerProfile(
            id = "p4",
            name = "Liam",
            age = 23,
            country = Country("GB", "United Kingdom", "🇬🇧", "Europe"),
            bio = "London guy ☕ Always up for a laugh",
            avatarBgColor = 0xFF0D47A1,
            avatarEmoji = "👋",
            videoTone = "cozy",
            photoUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=800&q=80",
            greetings = listOf(
                "Cheers mate! How are you doing today?",
                "Hey! Greetings from London!",
                "Hello! Great connection we got here!"
            ),
            chatReplies = mapOf(
                "hi" to "Alright mate! How are things?",
                "hello" to "Hello there! Splendid to meet you!",
                "where are you from" to "London, UK! 🇬🇧 Raining outside as usual haha!",
                "default" to "Brilliant! What kind of hobbies are you into?"
            )
        ),
        PartnerProfile(
            id = "p5",
            name = "Yuki",
            age = 22,
            country = Country("JP", "Japan", "🇯🇵", "Asia"),
            bio = "Anime, gaming & ramen lover 🍜",
            avatarBgColor = 0xFF880E4F,
            avatarEmoji = "🌸",
            videoTone = "soft",
            photoUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=800&q=80",
            greetings = listOf(
                "Konnichiwa! Hello from Tokyo! 🌸",
                "Hi! Nice to meet you!",
                "Hello! What's your favorite hobby?"
            ),
            chatReplies = mapOf(
                "hi" to "Konnichiwa! ✨ Nice to see you!",
                "hello" to "Hello! Welcome to Japan time!",
                "where are you from" to "Tokyo, Japan! 🇯🇵 It's night time here!",
                "default" to "Sugoi! So fun chatting with international friends!"
            )
        ),
        PartnerProfile(
            id = "p6",
            name = "Mateo",
            age = 25,
            country = Country("ES", "Spain", "🇪🇸", "Europe"),
            bio = "Guitar player & traveler 🎸 Sunny vibes",
            avatarBgColor = 0xFFBF360C,
            avatarEmoji = "🎸",
            videoTone = "warm",
            photoUrl = "https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?auto=format&fit=crop&w=800&q=80",
            greetings = listOf(
                "¡Hola amigo! How's your evening going?",
                "Hey! Sending sunshine from Barcelona! ☀️",
                "Buenas! Ready for a friendly chat?"
            ),
            chatReplies = mapOf(
                "hi" to "¡Hola! Good to meet you!",
                "hello" to "Hola! How are you doing?",
                "where are you from" to "I'm in Barcelona, Spain! 🇪🇸 Where are you calling from?",
                "default" to "¡Qué bueno! That's really awesome."
            )
        ),
        PartnerProfile(
            id = "p7",
            name = "Sophie",
            age = 23,
            country = Country("FR", "France", "🇫🇷", "Europe"),
            bio = "Parisienne 🥐 Fashion and photography",
            avatarBgColor = 0xFF004D40,
            avatarEmoji = "🎨",
            videoTone = "chic",
            photoUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&w=800&q=80",
            greetings = listOf(
                "Bonjour! So nice to meet you on VibeTV!",
                "Salut! How is your day so far?",
                "Coucou! Greeting from Paris! 🗼"
            ),
            chatReplies = mapOf(
                "hi" to "Bonjour! Hope you are doing well!",
                "hello" to "Salut! Nice smile you got there!",
                "where are you from" to "Paris, France! 🇫🇷 And you?",
                "default" to "Magnifique! It's so nice discovering people here."
            )
        ),
        PartnerProfile(
            id = "p8",
            name = "Maximilian",
            age = 26,
            country = Country("DE", "Germany", "🇩🇪", "Europe"),
            bio = "Engineer & cyclist 🚴 Berlin vibes",
            avatarBgColor = 0xFF263238,
            avatarEmoji = "⚡",
            videoTone = "clean",
            photoUrl = "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?auto=format&fit=crop&w=800&q=80",
            greetings = listOf(
                "Hallo! Good to connect with you!",
                "Hey there! Greetings from Berlin!",
                "Moin moin! How's your connection?"
            ),
            chatReplies = mapOf(
                "hi" to "Hallo! Everything good?",
                "where are you from" to "Berlin, Germany! 🇩🇪",
                "default" to "Interesting! Thanks for sharing."
            )
        ),
        PartnerProfile(
            id = "p9",
            name = "Min-jun",
            age = 23,
            country = Country("KR", "South Korea", "🇰🇷", "Asia"),
            bio = "K-Pop fan & foodie in Seoul 🥢",
            avatarBgColor = 0xFF311B92,
            avatarEmoji = "⭐",
            videoTone = "neon",
            photoUrl = "https://images.unsplash.com/photo-1501196354995-cbb51c65aaea?auto=format&fit=crop&w=800&q=80",
            greetings = listOf(
                "Annyeonghaseyo! Hi from Seoul! 🇰🇷",
                "Hey! Nice to meet you today!",
                "Hello friend! Having a good time?"
            ),
            chatReplies = mapOf(
                "hi" to "Annyeong! Good vibes to you!",
                "where are you from" to "Seoul, South Korea! 🇰🇷",
                "default" to "Haha that's so cool! VibeTV is awesome today!"
            )
        ),
        PartnerProfile(
            id = "p10",
            name = "Chloe",
            age = 22,
            country = Country("CA", "Canada", "🇨🇦", "Americas"),
            bio = "Toronto photographer 📸 Nature lover",
            avatarBgColor = 0xFFC2185B,
            avatarEmoji = "🍁",
            videoTone = "soft",
            photoUrl = "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&w=800&q=80",
            greetings = listOf(
                "Hey there! Greetings from Canada! 🍁",
                "Hi! How is your day going so far?",
                "Hello! So great to meet you!"
            ),
            chatReplies = mapOf(
                "hi" to "Hey!! Happy to connect!",
                "where are you from" to "I'm in Toronto, Canada! 🇨🇦",
                "default" to "Awesome! Such a cool chat."
            )
        ),
        PartnerProfile(
            id = "p11",
            name = "Jack",
            age = 24,
            country = Country("AU", "Australia", "🇦🇺", "Oceania"),
            bio = "Surfer from Sydney 🏄‍♂️ Good day mate!",
            avatarBgColor = 0xFF00695C,
            avatarEmoji = "🌊",
            videoTone = "sunset",
            photoUrl = "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?auto=format&fit=crop&w=800&q=80",
            greetings = listOf(
                "G'day mate! How are ya?",
                "Hey there! Sunny day in Sydney! ☀️",
                "G'day! Ready for a quick chat?"
            ),
            chatReplies = mapOf(
                "hi" to "G'day mate! Hope you're doing great!",
                "where are you from" to "Sydney, Australia! 🇦🇺 Waves are amazing today!",
                "default" to "Spot on! Great to meet ya!"
            )
        ),
        PartnerProfile(
            id = "p12",
            name = "Carlos",
            age = 25,
            country = Country("MX", "Mexico", "🇲🇽", "Americas"),
            bio = "Musician in Mexico City 🌮 Fiesta vibes",
            avatarBgColor = 0xFFE64A19,
            avatarEmoji = "🎉",
            videoTone = "warm",
            photoUrl = "https://images.unsplash.com/photo-1507081323647-4d2504a4b919?auto=format&fit=crop&w=800&q=80",
            greetings = listOf(
                "¡Hola amigo! Saludos desde México! 🇲🇽",
                "Hey! How is everything today?",
                "¡Buenas! Nice to connect with you!"
            ),
            chatReplies = mapOf(
                "hi" to "¡Hola! Mucho gusto!",
                "where are you from" to "Mexico City! 🇲🇽",
                "default" to "¡Excelente! Having fun!"
            )
        ),
        PartnerProfile(
            id = "p13",
            name = "Matteo",
            age = 23,
            country = Country("IT", "Italy", "🇮🇹", "Europe"),
            bio = "Designer in Milan ☕ Ciao!",
            avatarBgColor = 0xFF2E7D32,
            avatarEmoji = "✨",
            videoTone = "cozy",
            photoUrl = "https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?auto=format&fit=crop&w=800&q=80",
            greetings = listOf(
                "Ciao! Greetings from Milan! 🇮🇹",
                "Buongiorno! How are you today?",
                "Ciao bello! Nice to meet you!"
            ),
            chatReplies = mapOf(
                "hi" to "Ciao! Wonderful to meet you!",
                "where are you from" to "Milan, Italy! 🇮🇹",
                "default" to "Bellissimo! Have an awesome day!"
            )
        )
    )

    fun getRandomPartner(selectedCountry: Country, lastPartnerId: String? = null): PartnerProfile {
        val eligible = if (selectedCountry.code != "ALL") {
            samplePartners.filter { it.country.code == selectedCountry.code }
                .ifEmpty { samplePartners }
        } else {
            samplePartners
        }.filter { it.id != lastPartnerId }.ifEmpty { samplePartners }

        return eligible[Random.nextInt(eligible.size)]
    }
}
