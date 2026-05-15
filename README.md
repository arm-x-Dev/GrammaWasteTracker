# 🌲 Gramma-Waste Tracker 🚛
### *Digitizing Rural Waste Management for Smart Villages*

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android)](https://www.android.com/)
[![Language](https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin)](https://kotlinlang.org/)
[![Database](https://img.shields.io/badge/Database-Firebase-FFCA28?logo=firebase)](https://firebase.google.com/)

Gramma-Waste Tracker is a high-performance, real-time waste management solution designed for Gram Panchayats. It bridges the communication gap between village residents and waste collection authorities through live GPS tracking, automated alerts, and a community-driven reporting system.

---

## ❓ The Problem
Waste collection in rural areas often lacks a predictable schedule. Residents frequently miss the collection truck, leading to unhygienic garbage buildup (blackspots). Conversely, authorities have no real-time way to identify where extra collection is needed most.

## 💡 The Solution
A dual-mode mobile platform that provides:
1.  **Transparency:** Real-time visibility of collection vehicles.
2.  **Predictability:** Automated proximity alerts when the truck is near.
3.  **Accountability:** Proof of collection via photo verification.
4.  **Accessibility:** Full support for local languages (**Kannada**) to ensure every villager can use it.

---

## ✨ Key Features

### 🏠 For Residents
- **📍 Live Fleet Tracking:** High-accuracy map view of all on-duty waste collection trucks.
- **🔔 Proximity Alerts:** A smart background service notifies you when a truck is within **300 meters** of your home.
- **📸 Smart Reporting:** Snap a photo and pinpoint the GPS location of any garbage overflow or blackspot.
- **🌍 Multilingual UI:** Seamlessly switch between **Kannada** and **English** with one tap.
- **📜 Reporting History:** Track the status of your reports (Pending, In-Progress, Collected).

### 🚛 For Drivers & Authorities
- **🟢 Duty Management:** Toggle duty status to broadcast location to residents only during working hours.
- **📝 Real-time Task Feed:** Instantly see a list of garbage spots reported by residents in the area.
- **✅ Work Verification:** Mandatory "After" photo upload to mark a task as "Collected."
- **📡 Background GPS:** Robust Foreground Service ensures tracking works even when the app is minimized.

---

## 🛠️ Technology Stack

| Category | Technology |
| :--- | :--- |
| **Language** | Native Kotlin |
| **Backend/Cloud** | Firebase Realtime Database, Auth, Storage |
| **Maps & Location** | OSMDroid (OpenStreetMap), Google Play Services Location |
| **Architecture** | MVVM (Model-View-ViewModel) |
| **Notifications** | Firebase Cloud Messaging (FCM), Local Foreground Services |
| **Camera** | Android CameraX API |
| **Design** | Custom "Forest Glow" System (Material 3) |

---

## 🎨 Design Philosophy: "Forest Glow"
The app features a unique nature-inspired design system:
- **Premium Aesthetics:** Uses soft "Bleeding Light" effects and glassmorphism.
- **Pill UI:** Rounded elements (28dp radius) for a modern, tactile interface.
- **Accessibility:** Large, high-contrast buttons optimized for outdoor visibility and local language fonts.

---

## 🚀 Installation & Setup

### 📥 1. Download the App
Click the link below to download the latest production-ready APK:
[**Download GrammaWasteTracker.apk**](./GrammaWasteTracker.apk)

### ⚙️ 2. Configuration
1.  Enable **"Install from Unknown Sources"** in your Android settings.
2.  Install the APK and grant **Location** and **Notification** permissions.
3.  Sign in with your registered village credentials (Resident or Driver).

---

## 🏗️ Project Architecture
The project follows the **MVVM** pattern to ensure a clean separation of concerns:
- **Model:** Data structures for Users, Locations, and Reports.
- **View:** XML-based layouts with ViewBinding for type-safe UI interaction.
- **ViewModel:** Managing UI state and observing real-time changes from Firebase.
- **Repository:** Centralized data access layer for all network operations.

---

## 🔮 Future Scope
- **AI Analytics:** Automatic waste type detection using AI image recognition.
- **Route Optimization:** Dynamic pathfinding for drivers to minimize fuel consumption.
- **Reward System:** "Green Points" for residents who consistently report and maintain cleanliness.

---

## 👨‍💻 Developed By
**Alok Muranal**  
*USN: 2KE22EC006*  
**KLE Institute of Technology, Hubli**  
*8th-Semester Internship Project*

---
