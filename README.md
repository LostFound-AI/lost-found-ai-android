# LostFound AI — 智慧室內遺失物定位系統

An Android app that helps users find lost items inside their home using a customizable digital twin room map and AI-powered prediction.

> 期末專案 — 01157110 曾宇晨、01157135 梁祐嘉、01157151 廖崇劭

---

## Features

### 🗺️ Interactive Room Map
- Drag and drop 18 furniture types (bed, desk, sofa, wardrobe…) onto a 2D canvas
- Pan and zoom the map with gestures
- Draw custom room boundary shapes or choose from 4 presets (rectangle, L, T, U)
- Save and reuse named custom boundaries
- Optional background grid for alignment

### 📦 Item Management
- Add items with name, category (8 types), and size
- Pin an item's exact location directly on the map
- Browse and edit your item history
- Delete or update items at any time

### 🤖 AI Prediction
- Tap any item in the history to trigger AI prediction
- The engine scores every position on the map based on:
  - **Item size** — small items scored higher near furniture edges
  - **Category affinity** — bathroom items near the sink, electronics near the desk
  - **Walk path proximity** — positions near your recorded walking path score higher
  - **Exclusion zones** — positions inside solid furniture (bed, wardrobe…) are eliminated
- Top 3 most likely spots shown as red markers on the map

### 🟡 Walk Path Recording
- Enable "記錄行走路線" in the settings drawer
- Tap the map to add points along your usual walking path
- The yellow path is saved and used by the AI to improve predictions

### 🟢 Live Item Pins
- Items with a saved manual location appear as green dots on the map
- See where everything is stored at a glance without opening any sheet

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM (ViewModel + StateFlow) |
| Storage | SharedPreferences + Gson |
| AI | Heuristic scoring engine (rule-based) |
| Navigation | Jetpack Navigation Compose |

---

## Getting Started

### Prerequisites
- Android Studio Meerkat or later
- Android SDK 36.1
- Min SDK: API 24 (Android 7.0)

### Run locally
```bash
git clone https://github.com/LostFound-AI/lost-found-ai-android.git
```
Open in Android Studio → wait for Gradle sync → hit **Run ▶**

---

## App Flow

```
Home Screen
└── Select / create a room
    └── Map Screen
        ├── Edit mode — drag furniture onto the map
        ├── 新增物品 — log where an item is (or let AI predict)
        └── 尋找 — find a lost item
            ├── Manual location → blue dot on map
            └── AI prediction  → red dots on map
```

---

## Screenshots

_Coming soon_
