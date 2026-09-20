<div align="center">

# beating-yesterday

**Android app that gamifies daily self-improvement against yesterday's record**

![Platform](https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white)
![Language](https://img.shields.io/badge/language-Java-007396?logo=java&logoColor=white)
![Min SDK](https://img.shields.io/badge/minSdk-21-blue)
[![Verify](https://github.com/jumincho/beating-yesterday/actions/workflows/verify.yml/badge.svg)](https://github.com/jumincho/beating-yesterday/actions/workflows/verify.yml)
![License](https://img.shields.io/badge/license-MIT-green)
![Year](https://img.shields.io/badge/year-2021-blue)

</div>

---

## Overview

Logs daily calorie intake and study time, then judges today's record against
yesterday's. Calorie scoring is BMI-aware, so this is less a diet tracker than a
personalized self-management tool that frames each day as a contest with
yesterday's you.

## Features

- **User profile** — name, sex, age, height, weight → auto-computed BMI.
- **Diet** — log breakfast / lunch / dinner; the Korean Food Safety OpenAPI returns
  calories, and a BMI-bracket-aware (under / normal / over) calorie score is computed.
- **Study timer** — circular timer with drag-to-set hours / minutes / seconds, plus a stopwatch.
- **TODO** — SQLite-backed list with swipe-to-refresh.
- **Daily contest** — yesterday's calorie score + study time vs today's, reported as
  "did you beat yesterday?"

## Screens

Bottom navigation with three tabs:

| Tab | Description |
| --- | --- |
| Diet | Food entry · weight management |
| Home | Profile · daily yesterday-vs-today contest |
| Productivity | TODO · study timer |

## Tech stack

- **Language**: Java
- **Platform**: Android (minSdk 21 / targetSdk 32)
- **Architecture**: Fragment + ViewModel
- **Storage**: SharedPreferences (profile, daily records) + SQLite (TODO)
- **Network**: HttpURLConnection + Korean Food Safety OpenAPI
- **UI**: Material Components, ConstraintLayout, RecyclerView, Navigation Component

## Project layout

```
app/src/main/
├── java/com/jumincho/beatingyesterday/
│   ├── MainActivity.java
│   ├── domain/                        # framework-free pure logic (JVM unit-tested)
│   │   └── HealthMetrics.java         # BMI / calorie scoring
│   ├── data/                          # data layer
│   │   ├── FoodCalorieApi.java        # Food Safety API client
│   │   ├── Note.java                  # TODO model
│   │   ├── NoteAdapter.java
│   │   └── NoteDatabase.java          # SQLite helper
│   └── ui/
│       ├── ProfileSetupActivity.java  # first-launch profile entry
│       ├── home/                      # home (contest result)
│       ├── diet/                      # diet / weight management
│       └── productivity/              # TODO + timer
│           ├── CircularTimerView.java # custom circular timer view
│           └── TimerMode.java
└── res/
    ├── layout/        # screen layouts
    ├── navigation/    # bottom-tab navigation graph
    └── ...
```

## Secrets handling

The build system reads the Food Safety OpenAPI key from `local.properties` and
injects it via Gradle `buildConfigField` as `BuildConfig.FOOD_API_KEY`. No
secrets are present in source. `local.properties` is gitignored.

| `local.properties` key | `BuildConfig` field | Use site |
| --- | --- | --- |
| `FOOD_API_KEY` | `BuildConfig.FOOD_API_KEY` | Food Safety calorie lookup |

## Build

1. Get an API key from the [Korean Food Safety OpenAPI](https://various.foodsafetykorea.go.kr/nutrient/).
2. Add a `local.properties` entry at the repo root:

   ```properties
   FOOD_API_KEY=your_key_here
   ```

3. Open the project in Android Studio → `Run`, or build from the command line:

   ```bash
   ./gradlew assembleDebug
   ```

Requirements:

- Android Studio Bumblebee or later
- JDK 8 or later
- Android SDK 32

## Materials

- Demo video: <https://youtu.be/vW4CvgCHdco>
- Slides: [`docs/presentation.pptx`](docs/presentation.pptx)

## Screenshots

<img width="20%" src="https://user-images.githubusercontent.com/77545063/200374902-2da72615-5cf8-4d20-b950-f00962a1c795.png" alt="app screenshot"/>

## License

[MIT License](./LICENSE)
