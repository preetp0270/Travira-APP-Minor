# 🌍 Travira

Travira is an AI-powered Android travel application that helps users discover popular tourist destinations, explore local culture, and interact with an intelligent chatbot for travel assistance. The app is designed to make travel planning simple, informative, and personalized.

## ✨ Features

* 🔐 **User Authentication**

    * Secure Sign Up and Login
    * User and admin profile management

* 📍 **Discover Popular Places**

    * Browse famous tourist attractions
    * Explore historical, cultural, and natural landmarks
    * View detailed information about each destination
    * Community reviews and ratings

* 🗺️ **Interactive Maps**(Future Update)

    * Powered by **Mapbox**
    * View destinations on an interactive map
    * Get directions and location details
    * Explore nearby attractions

* 🤖 **AI Travel Chatbot** (Gemini 3.8 Flash + 3.5 Flash-Lite)

    * Ask questions about destinations
    * Learn about local culture, traditions, and history
    * Receive travel tips and recommendations
    * Get information about nearby places and attractions

* 🏛️ **Destination Information**(Future Update)

    * Historical background
    * Best time to visit
    * Entry fees (if applicable)
    * Opening hours
    * Nearby attractions
    * Travel tips

* 🍛 **Local Guide**(Future Update)

    * Famous local foods
    * Traditional festivals
    * Regional languages
    * Shopping recommendations
    * Cultural experiences

* ❤️ **Favorites**

    * Save favorite destinations
    * Access saved places anytime
    * User can add new place and give a little bit contribution to the world.

---

## 🚀 Tech Stack

| Technology                  | Purpose                         |
| --------------------------- | ------------------------------- |
| **Kotlin**                  | Android App Development         |
| **Android Studio**          | Development Environment         |
| **MVVM Architecture**       | Clean and Scalable Architecture |
| **MongoDB Atlas**           | Cloud Database                  |
| **Authentication**          | Secure User Login               |
| **Gemini API**               | AI Travel Chatbot (3.8 Flash)   |
| **Git & GitHub**            | Version Control                 |

---

## 📱 Future Enhancements

* AI-powered personalized travel recommendations
* Hotel booking integration
* Restaurant recommendations
* Offline destination information
* Voice-enabled AI assistant
* Travel itinerary planner
* Weather forecasts
* Multi-language support
* Emergency contact information

---

## 📂 Project Structure

```text
Travira/
├── README.md
│
├── android/                          # Android (Kotlin + Jetpack Compose)
│   ├── app/
│   │   ├── build.gradle.kts
│   │   └── src/
│   │       ├── main/
│   │       │   ├── AndroidManifest.xml
│   │       │   ├── java/com/example/travira/
│   │       │   │   ├── MainActivity.kt
│   │       │   │   ├── auth/
│   │       │   │   │   └── TokenManager.kt
│   │       │   │   ├── components/
│   │       │   │   │   ├── AppCard.kt
│   │       │   │   │   └── TraviraBottomBar.kt
│   │       │   │   ├── data/
│   │       │   │   │   └── PlaceData.kt
│   │       │   │   ├── model/
│   │       │   │   │   ├── Place.kt
│   │       │   │   │   └── User.kt
│   │       │   │   ├── navigation/
│   │       │   │   │   ├── BottomNavItem.kt
│   │       │   │   │   └── NavGraph.kt
│   │       │   │   ├── remote/                    # API layer (Retrofit)
│   │       │   │   │   ├── AdminApi.kt
│   │       │   │   │   ├── ApiModels.kt
│   │       │   │   │   ├── AuthApi.kt
│   │       │   │   │   ├── ChatApi.kt
│   │       │   │   │   ├── CloudinaryUploader.kt
│   │       │   │   │   ├── Placeapi.kt
│   │       │   │   │   └── retrofitInstance.kt
│   │       │   │   ├── screens/
│   │       │   │   │   ├── admin/
│   │       │   │   │   │   └── AdminDashboardScreen.kt
│   │       │   │   │   ├── ai/
│   │       │   │   │   │   └── AIChatScreen.kt
│   │       │   │   │   ├── auth/
│   │       │   │   │   │   └── LoginScreen.kt
│   │       │   │   │   ├── home/
│   │       │   │   │   │   └── HomeScreen.kt
│   │       │   │   │   ├── places/
│   │       │   │   │   │   ├── AddPlaceScreen.kt
│   │       │   │   │   │   ├── EditPlaceScreen.kt
│   │       │   │   │   │   └── PlacesScreen.kt
│   │       │   │   │   ├── profile/
│   │       │   │   │   │   ├── ContributionScreen.kt
│   │       │   │   │   │   ├── EditProfileScreen.kt
│   │       │   │   │   │   ├── NotificationsScreen.kt
│   │       │   │   │   │   ├── ProfileScreen.kt
│   │       │   │   │   │   ├── VisitedPlacesScreen.kt
│   │       │   │   │   │   └── WishlistScreen.kt
│   │       │   │   │   └── splash/
│   │       │   │   │       ├── IntroVideoScreen.kt
│   │       │   │   │       └── splashScreen.kt
│   │       │   │   └── ui/theme/
│   │       │   │       ├── Color.kt
│   │       │   │       ├── Theme.kt
│   │       │   │       └── Type.kt
│   │       │   └── res/                           # Drawables, values, mipmaps, etc.
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   └── gradle/
│
└── backend/                          # Node.js + Express API
    ├── server.js
    ├── package.json
    ├── public/
    │   ├── admin.html                # Browser place uploader (admin login)
    │   └── reset-password.html       # Password reset form (email link)
    ├── controllers/
    │   ├── admin.js
    │   ├── chat.js
    │   ├── place.js
    │   └── user.js
    ├── middleware/
    │   ├── adminMiddleware.js
    │   └── authMiddleware.js
    ├── models/
    │   ├── place.js
    │   └── user.js
    └── routes/
        ├── adminRoutes.js
        ├── chatRoutes.js
        ├── placeroute.js
        └── user.js
```

---

## 🎯 Project Goal

Travira aims to become an intelligent travel companion by combining AI, interactive maps, and rich destination information into one seamless Android application. Users can discover popular places, learn about local culture, ask travel-related questions to an AI chatbot, and explore destinations using Mapbox-powered maps.

---

## 👨‍💻 Developer

**Preet Patel(Founder , Idea , Most Contributions)** | **Yagnik padaliya(Co-Founder , Solving Problems , Helping and Suggestions)**

---

### ⭐ Vision

**"Explore smarter, discover deeper, and travel with confidence using Travira."**
