# dBLE

An Android app for scanning, monitoring proximity, and keeping a history of nearby BLE devices.

## 🛠️ Setup & Running

### Prerequisites

| Requirement | Version |
|---|---|
| JDK | 17+ |
| Android Studio | Latest version supporting AGP 9.4+ |
| Android Gradle Plugin | 9.4.1 |
| Gradle | 9.6.0 (via wrapper, auto-downloaded) |
| Kotlin | 2.2.10 |
| compileSdk / targetSdk | 37 |
| minSdk | 30 (Android 11+) |
| Device | Physical device with BLE (the emulator does not support BLE scanning) |

### Steps

1. **Clone the repository**
   ```bash
   git clone <repo-url>
   cd dBLE
   ```

2. **Open in Android Studio**
   - `File → Open` → select the project folder.
   - Wait for Gradle Sync to finish (the wrapper will automatically download Gradle 9.6.0).
   - If Android Studio reports an AGP compatibility error, update Android Studio via `Tools → SDK Manager`.

3. **Run the app**
   - Select a physical device (Android 11+) with Bluetooth & Location enabled.
   - Click `Run ▶` (Shift+F10), or from the terminal:
     ```bash
     ./gradlew installDebug          # Windows: gradlew.bat installDebug
     ```

4. **Build the APK**
   ```bash
   ./gradlew assembleDebug         # debug
   ./gradlew assembleRelease       # release
   ```

5. **Run unit tests**
   ```bash
   ./gradlew testDebugUnitTest
   ```

> ℹ️ On first scan, the app will request *Nearby Devices* and *Location* permissions. Both must be granted for BLE scanning to work.

## 🏗️ Architecture & Libraries

### Architecture: MVI (Model–View–Intent)

This project uses the **MVI architecture with unidirectional data flow (UDF)**, combined with Jetpack Compose.

**Data flow:**

```
User Interaction → Intent → ViewModel (reduce) → State → Compose re-render
                                   ↓
                              Effect (one-shot: navigation, permission requests)
```

Each feature (`scanner`, `tracking`, `history`) defines a single **Contract** containing:

- **`State`** — an immutable data class (`@Immutable`), the single source of truth for the screen's UI.
- **`Intent`** — a sealed interface listing every action the user can perform.
- **`Effect`** — a sealed interface for one-shot side effects (navigation, permission requests), consumed via `EventHandler`.

**Why MVI?**

- **Predictable & immutable** — state is never mutated, only replaced with a new state. State changes are easy to trace and easy to test.
- **Matches Compose** — Compose is declarative UI: UI = f(state). MVI provides a single immutable state, so Compose reconciliation works optimally without state scattered across multiple places.
- **Fewer bugs** — every possible screen state is forced to be explicitly defined in the Contract. There's no "forgot to update this state"; every screen change must go through a clearly defined Intent.
- **Easy to test** — the reducer is a pure function `(State, Intent) → State`; with Turbine + coroutines-test, ViewModel flows can be tested deterministically.

### Module Structure

```
app/src/main/java/id/project/df/dble/
├── app/            # Application class, navigation (NavKey-based Routes)
├── feature/        # UI features: scanner, tracking, history
│   └── <feature>/  # Screen (Compose) + Contract (State/Intent/Effect) + ViewModel
└── core/
    ├── common/     # MVI framework (Mvi, BaseViewModel, EventHandler), TimeProvider
    ├── data/       # Data implementations: ble, scan, history (Room), permission, monitor, audio, DI
    ├── domain/     # Use cases (ObserveDevices, ObserveTarget, FilterDevices)
    ├── model/      # Domain models (TrackedDevice, Proximity, RelativeTime)
    └── designsystem/ # Colors & visual tokens
```

Dependency flow: `feature → domain → data`, injected via Hilt.

**Why single module?**

- The app's scope is still small, so a single module keeps the build simple and fast — no multi-module ceremony (API/implementation boundaries, module wiring) to maintain prematurely.
- The package structure is already organized as if it were multi-module (`feature/` and `core/` mirror what would become separate Gradle modules), so migrating to multi-module later is mostly a mechanical split, not a refactor.

### Key Libraries

| Category | Library | Role |
|---|---|---|
| UI | Jetpack Compose (BOM) + Material 3 | Declarative UI |
| Navigation | Navigation 3 + `NavKey` (kotlinx.serialization) | Type-safe navigation |
| DI | Hilt + hilt-navigation-compose | Dependency injection |
| Persistence | Room | Detected-device history |
| Async | Kotlin Coroutines & Flow | Scan result streaming, state flow |
| Immutability | kotlinx-collections-immutable | `ImmutableList` for stable state (smart recomposition) |
| Lifecycle | lifecycle-process / runtime-compose | Lifecycle-aware scanning (pause when app is backgrounded) |
| Logging | Timber | Logging |
| Testing | JUnit4, MockK, Turbine, coroutines-test, Compose UI Test | Unit & UI testing |

## ⚠️ Disclaimer

This project was built with the help of **AI (AI-generated / AI-assisted)**. The code, documentation, and technical decisions have been manually reviewed, but are provided as is, without any warranty. Use at your own risk.
