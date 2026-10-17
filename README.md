# SkyPilot Sim - Multirotor Drone Flight Simulator

Realistic multirotor drone flight simulator built with Android, Jetpack Compose, and OpenGL ES 3.0. Features 6-DOF rigid body physics, cascaded PID loops (inner rate + outer angle/alt/GPS hold), virtual transmitter (Mode 1 & 2), and interactive training missions.

---

## 📱 How to Build & Install APK on Your Phone via GitHub

### Option 1: Automated GitHub Actions Build (Zero Setup on your PC)
This repository includes an automated GitHub Actions CI workflow in `.github/workflows/build-apk.yml`.

1. **Push or Export to GitHub**:
   - Push this repository to your GitHub account (or use the AI Studio settings menu -> **Push to GitHub** / **Export as ZIP**).
2. **Trigger Build**:
   - Every push to `main` or `master` automatically triggers the build!
   - You can also trigger it manually anytime:
     1. Open your repository on GitHub.
     2. Click on the **Actions** tab.
     3. Select **Build Android APK** from the left sidebar.
     4. Click **Run workflow** -> Select **debug** -> Click green **Run workflow** button.
3. **Download & Install the APK on Your Phone**:
   - Once the action finishes (usually 2–3 minutes):
     1. Click on the completed workflow run.
     2. Scroll down to the **Artifacts** section at the bottom.
     3. Tap **SkyPilot-Sim-APK** to download the zip containing `app-debug.apk`.
     4. Extract and open `app-debug.apk` on your Android phone.
     5. If prompted, allow **"Install unknown apps"** from your browser/files app.
     6. Tap **Install** and enjoy flying!

---

### Option 2: Build Locally using Android Studio / Terminal
If you have Android Studio or the command line on your computer:

```bash
# Clone the repository
git clone <your-repo-url>
cd skypilot-sim

# Ensure execution rights for gradlew
chmod +x gradlew

# Build the Debug APK
./gradlew assembleDebug

# Install directly to your connected Android phone via ADB:
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## 🎮 Flight Simulator Features
- **Realistic 6-DOF Multirotor Physics**: Mass, inertia, motor response lag, quadratic aerodynamic drag, ground effect cushion, and LiPo battery voltage sag.
- **Flight Modes**:
  - `ACRO`: Raw angular rate control with customizable rates & super-expo.
  - `ANGLE`: Self-leveling pitch & roll limits with auto-centering.
  - `HORIZON`: Acro flips at stick extremes, self-leveling near center.
  - `ALT_HOLD`: Barometer/PID closed-loop altitude hold.
  - `GPS_HOLD`: Loiter in 3D space with position correction.
- **Virtual Dual Gimbals**: Mode 1 & Mode 2 stick mapping, spring throttle toggle, deadzones, and dual-rates.
- **Multiple Camera Views**: FPV Cockpit, Chase Cam with lag damping, Orbit Inspection Cam, and Drone Belly Gimbal.
- **Full OSD & Telemetry**: Synthetic artificial horizon, battery cell voltage, motor RPM indicators, and compass heading.
