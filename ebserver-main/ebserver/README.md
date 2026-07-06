# Ebserver

Ebserver is a test automation and energy profiling orchestration server designed for Android applications (e.g., UserEnergy). It handles running instrumented UI tests, tracking device states, managing battery/charging conditions, and capturing detailed system metrics like memory usage, battery stats, and process statistics after each run.

## Features
- **Two Execution Modes:** Run tests dynamically via an HTTP server or automatically through a queue from the CLI.
- **Automated Energy Profiling:** Captures `meminfo`, `batterystats`, and `procstats` automatically using ADB `dumpsys`.
- **Intelligent Battery Management:** Automatically monitors the device battery. If it drops below 25%, tests are paused, and charging is re-enabled (via Meross smart plugs) until it hits 95%. It disables charging during tests to ensure accurate energy profiling.
- **Config Validation:** Ensures `config.json` is perfectly structured via schema validation (Joi) to prevent runtime crashes.
- **Graceful Interrupts:** Automatically restores charging state when gracefully exited (Ctrl+C) so devices don't die unexpectedly.
- **Multi-Device Concurrency:** Run the entire test suite simultaneously across all connected devices (USB or Wi-Fi) to speed up data collection.
- **Dynamic File Generation:** Test parameters are dynamically parsed to create nested folder structures for easy analysis.

## Prerequisites
- **Node.js** (v14+ recommended)
- **ADB (Android Debug Bridge)** must be installed and added to your system `PATH`.
- One or more Android devices connected via USB or Wi-Fi.

## Installation

1. Navigate to the `ebserver` folder.
2. Install dependencies:
   ```bash
   npm install
   ```

*(Note: Ensure you are running `npm install` in the `ebserver/` root. There is no nested `package.json` in the `src/` folder.)*

## Usage Modes

### 1. UI Queue Mode (Recommended)
This mode automatically reads from your JSON configuration and runs the entire matrix of tests from the CLI.

```bash
node src/ebserver.js -ui
```

**Optional Flags:**
- `-i`: Automatically compile and install the main APK and test APK via Gradle before running the tests.
  ```bash
  node src/ebserver.js -ui -i
  ```

**How it works:**
- It reads the configuration defined in `src/configUI/config.json`.
- It generates a full queue of tests (`experiments_queue.json`).
- All connected devices will simultaneously run the complete queue of experiments.
- Device charging is spoofed to act as "unplugged" during tests for accurate logging. If battery drops < 25%, it will pause and charge up to 95%.
  
**Bypassing the Smart Charger:**
If you do not have a Meross smart plug, you can run the server with the `--no-meross` flag or set `USE_MEROSS=false` in your `.env`. When the battery reaches critical levels, the server will pause and prompt you to plug/unplug the device manually.
```bash
node src/ebserver.js -ui --no-meross
```

### 2. Express Server Mode
If your tests or external agents need to coordinate dynamically, you can run the server in HTTP mode.

```bash
node src/ebserver.js
```

This starts an Express server on port `3000`. The device (acting as a client) can connect over Wi-Fi and use the following endpoints:
- `GET /fetch`: Fetch the next experiment to execute.
- `GET /logdata`: Request the server to dump current battery and memory stats via ADB.
- `GET /done`: Signal that the run is complete, log the data, and advance the session.

*(Note: The server uses `src/configurationsFolder/default.json` for managing test ranges in this mode.)*

## Configuration
For detailed instructions on structuring `config.json` test parameters, please refer to the `CONFIGURATION_GUIDE.md` located in the root of the project.

## Output
Results are dumped into the `experiment-results` folder (outside the src directory), dynamically sorted by framework, device model, and the specific test parameters.

## iOS Testing (iPhone/iPad)

We have expanded the `ebserver` framework to support running **XCUITests** directly on iOS devices natively using the exact same configuration file (`config.json`). 

### Step-by-Step Instructions for iOS

1. **Enable Network Debugging:**
   - Plug your iPhone or iPad into your Mac via USB.
   - Open **Xcode**, then in the menu bar, navigate to **Window > Devices and Simulators**.
   - Select your device on the left pane.
   - Check the box that says **"Connect via network"**. (A network icon should appear next to the device name once successful).
   - Under the device details on the right, copy the **Identifier** (This is your device's UDID, e.g., `00008120-001234567890ABCD`).

2. **Prepare the Device:**
   - You can unplug the USB cable now. The tests will orchestrate over Wi-Fi!
   - Ensure the iOS device screen is unlocked.

3. **Run the Server with the iOS Flag:**
   - In your terminal (inside the `ebserver` folder), execute the following command, replacing the string with your copied UDID:
   
   ```bash
   node src/ebserver.js -ui --ios --device "YOUR-UDID-HERE"
   ```

**What Happens Next:**
The `ebserver` will read the `config.json` (just like it does for Android), generate the parameter permutations, automatically translate the tests (e.g., `WhatsAppTest` -> `WhatsAppUITest`), map parameters to Xcode environment variables, and launch the tests automatically on the iPhone screen using `xcodebuild`!
