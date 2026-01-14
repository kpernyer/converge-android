# Converge Android

A smart action prediction mobile app built with Kotlin and Jetpack Compose, connecting to [converge-runtime](https://github.com/kpernyer/converge-runtime).

## Features

- **Smart Action Prediction**: Uses on-device ML (TensorFlow Lite) to predict which action the user most likely wants to perform next
- **Domain-Aware JTBD System**: Learns about Jobs To Be Done, Packs, Blueprints, and Artifacts
- **One-Tap Actions**: Minimizes clicks to reach desired outcomes
- **gRPC Streaming**: Bidirectional communication with converge-runtime via HTTP/2
- **Cross-Platform Consistency**: Domain model aligned with [converge-ios](https://github.com/kpernyer/converge-ios) (iOS)

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Smart Action Screen                       │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────┐ │
│  │  Primary Action │  │  Quick Actions  │  │  Blueprints │ │
│  │     (Hero)      │  │     (Row)       │  │    (Cards)  │ │
│  └─────────────────┘  └─────────────────┘  └─────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              │
                    ┌─────────▼─────────┐
                    │  JTBDPredictor    │
                    │  (ML + Heuristics)│
                    └─────────┬─────────┘
                              │
           ┌──────────────────┼──────────────────┐
           │                  │                  │
   ┌───────▼───────┐  ┌───────▼───────┐  ┌──────▼──────┐
   │ DomainKnowledge│  │ OutcomeTracker│  │BehaviorStore│
   │     Base       │  │   (Learning)  │  │(Persistence)│
   └───────────────┘  └───────────────┘  └─────────────┘
```

## Domain Model

### 5 Core Packs
- **Money**: Financial operations (invoicing, payments, reconciliation)
- **Customers**: Revenue generation (leads, opportunities, deals)
- **Delivery**: Value delivery (promises, work items, acceptance)
- **People**: Workforce (hiring, onboarding, payroll)
- **Trust**: Governance (audit trails, compliance, risk)

### Prediction Strategies
1. **Blueprint-driven**: Continue active multi-step workflows
2. **Artifact flow**: Suggest actions that use recently produced outputs
3. **Frequency-based**: Learn from user patterns
4. **Pack affinity**: Stay within frequent domains
5. **Onboarding**: Guide new users to starting points

## Tech Stack

- **Language**: Kotlin 2.0
- **UI**: Jetpack Compose with Material 3
- **Architecture**: MVVM with StateFlow
- **ML**: TensorFlow Lite (with GPU/NNAPI delegate support)
- **Networking**: gRPC with Kotlin Coroutines
- **Design System**: Ported from [converge-www](https://github.com/kpernyer/converge-www)

## Design System

The app uses the Converge design system from the web platform:

| Token | Value | Usage |
|-------|-------|-------|
| Paper | `#f5f4f0` | Background |
| Ink | `#111111` | Primary text |
| Accent | `#2d5a3d` | Brand green |
| Surface | `#eae9e4` | Cards, hover states |

Typography uses monospace fonts for headings and sans-serif for body text.

## Getting Started

### Prerequisites

- Android Studio Hedgehog or later
- JDK 17
- Android SDK 34

### Build

```bash
./gradlew assembleDebug
```

### Run on Emulator

```bash
./gradlew installDebug
adb shell am start -n zone.converge.android/.MainActivity
```

### Run Tests

```bash
./gradlew test
```

## Project Structure

```
app/src/main/java/zone/converge/android/
├── data/
│   ├── Domain.kt                 # Runtime types (Job, Action, etc.)
│   └── DomainKnowledgeBase.kt    # Packs, JTBDs, Blueprints, Flows
├── grpc/
│   └── ConvergeClient.kt         # gRPC client for converge-runtime
├── ml/
│   ├── ActionPredictor.kt        # TFLite-based action prediction
│   ├── BehaviorStore.kt          # Persistent behavior patterns
│   └── JTBDPredictor.kt          # Domain-aware JTBD prediction
└── ui/
    ├── SmartActionScreen.kt      # Main Compose UI
    ├── SmartActionViewModel.kt   # MVVM ViewModel
    └── theme/
        └── Theme.kt              # Converge design system
```

## Related Projects

- [converge-runtime](https://github.com/kpernyer/converge-runtime) - Backend runtime (Rust)
- [converge-ios](https://github.com/kpernyer/converge-ios) - iOS app (Swift/SwiftUI)
- [converge-www](https://github.com/kpernyer/converge-www) - Web platform (React)
- [converge-domain](https://github.com/kpernyer/converge-domain) - Domain documentation

## License

MIT License - see [LICENSE](LICENSE) file for details.

Copyright 2024-2025 Aprio One AB, Sweden
