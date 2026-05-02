# LostFound AI — 智慧室內遺失物定位系統

An Android app that helps users find lost items inside their home using a customizable 2D room map, AI-powered location prediction, and Gemini Vision object recognition.

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
- Browse and edit your item history with photo thumbnails
- Delete or update items at any time

### 📷 AI Photo Recognition (Gemini Vision)
- Tap "拍照" when adding an item to capture a photo
- Gemini 2.5 Flash analyzes the image and automatically fills in:
  - **Item name** — e.g. 手機、鑰匙、錢包
  - **Category** — matched to one of the 8 predefined types
- Photo is saved and shown as a thumbnail in the item history

### 🏠 Furniture Drill-Down
- Tap any furniture tile in normal mode to open a detail sheet
- See all items stored in or near that furniture at a glance
- Add a new item directly linked to a specific furniture piece
- Item count badge on each furniture tile shows how many items are stored there

### 🤖 AI Location Prediction
- Tap any item in the history to trigger AI prediction
- The heuristic engine scores every position on the map based on:
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
| AI — Object Recognition | Gemini 2.5 Flash (Google Generative AI) |
| AI — Location Prediction | Heuristic scoring engine (rule-based) |
| Navigation | Jetpack Navigation Compose |

---

## Getting Started

### Prerequisites
- Android Studio Meerkat or later
- Android SDK 36.1
- Min SDK: API 24 (Android 7.0)
- A free Gemini API key from [aistudio.google.com](https://aistudio.google.com/app/apikey)

### Setup

```bash
git clone https://github.com/LostFound-AI/lost-found-ai-android.git
```

Open in Android Studio and add your Gemini API key to `local.properties`:

```
geminiApiKey=YOUR_API_KEY_HERE
```

Then hit **Run ▶**

> `local.properties` is gitignored — your API key is never committed to version control.

---

## App Flow

```
Home Screen
└── Select / create a room
    └── Map Screen
        ├── Edit mode — drag furniture onto the map, set room boundary
        ├── Tap furniture (normal mode) — view/add items stored there
        ├── 新增物品 — take a photo for AI recognition, or fill in manually
        └── 尋找 — find a lost item
            ├── Has saved location → green dot on map
            └── No location → AI predicts top 3 spots (red dots)
```

---

## Screenshots

_Coming soon_
