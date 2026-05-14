# LostFound AI — Indoor Lost Item Locator

An Android application that helps users locate misplaced items inside their home using a customizable 2D room map, rule-based location prediction, and Gemini Vision object recognition.

> Final Project — 01157110 曾宇晨、01157135 梁祐嘉、01157151 廖崇劭

---

## Features

### Interactive Room Map
- Place 17 furniture types onto a 2D canvas with drag-and-drop
- Pan and zoom the map with touch gestures
- Draw a custom room boundary or choose from 4 presets (rectangle, L, T, U)
- Save and reuse named custom boundary shapes
- Optional background grid for alignment
- Furniture items support scaling and rotation

### Item Management
- Add items with a name, category (8 types), and size
- Pin an item's exact location directly on the map
- Browse and edit item history with photo thumbnails
- Delete or update items at any time

### Photo Recognition (Gemini Vision)
- Capture a photo when adding an item
- Gemini 2.5 Flash analyzes the image and automatically fills in the item name and category
- Photo is saved and displayed as a thumbnail in the item history

### Furniture Detail View
- Tap any furniture piece to open a detail sheet listing all items linked to it
- Add a new item directly associated with a specific furniture piece
- Each furniture tile shows a count badge of how many items are stored there

### Location Prediction
- Select any item to trigger the prediction engine
- The heuristic engine scores map positions based on item size, category affinity, walk path proximity, and exclusion zones around solid furniture
- The top 3 predicted locations are shown as markers on the map

### Walk Path Recording
- Record your usual walking path by tapping points on the map
- The recorded path is factored into location predictions

### Manual Item Pins
- Items with a saved manual location are shown as markers on the map for quick reference

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM (ViewModel + StateFlow) |
| Storage | SharedPreferences + Gson |
| Object Recognition | Gemini 2.5 Flash (Google Generative AI) |
| Location Prediction | Heuristic scoring engine |
| Navigation | Jetpack Navigation Compose |

---

## Getting Started

### Prerequisites
- Android Studio Meerkat or later
- Android SDK 36
- Min SDK: API 24 (Android 7.0)
- A Gemini API key from [aistudio.google.com](https://aistudio.google.com/app/apikey)

### Setup

```bash
git clone https://github.com/LostFound-AI/lost-found-ai-android.git
```

Open in Android Studio and add your Gemini API key to `local.properties`:

```
geminiApiKey=YOUR_API_KEY_HERE
```

Then run the app via **Run > Run 'app'**.

> `local.properties` is listed in `.gitignore` and will never be committed to version control.

---

## App Flow

```
Home Screen
└── Select or create a room
    └── Map Screen
        ├── Edit mode — place and arrange furniture, set room boundary
        ├── Tap furniture (normal mode) — view or add items linked to that piece
        ├── Add item — capture a photo for automatic recognition, or enter details manually
        └── Find item
            ├── Has saved location — shown as a pin on the map
            └── No saved location — prediction engine marks the top 3 likely spots
```

---

## Screenshots

_Coming soon_
