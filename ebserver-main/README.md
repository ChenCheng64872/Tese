# UserEnergy Project

UserEnergy is a comprehensive framework designed to automate instrumented UI tests across Android applications and profile their energy consumption and system resource usage dynamically.

## Project Structure

The project is split into two primary components:

### 1. [UserEnergy Android App](./UserEnergy/)
This is the Android side of the project. It contains the instrumented UI tests for various applications (e.g., YouTube, WhatsApp, Instagram, Flashlight, etc.).
- Written in Java/Kotlin.
- Uses Android's UI Automator and Espresso for UI testing.
- The tests are designed to execute specific features within target apps to measure their energy footprint.

### 2. [Ebserver (Execution & Battery Server)](./ebserver/)
This is the Node.js orchestration server that controls the execution of the Android tests over ADB.
- **Orchestration:** Parses `config.json` parameters to execute a matrix of experiments automatically.
- **Data Collection:** Pulls `batterystats`, `meminfo`, and `procstats` via `dumpsys` after every run and organizes them neatly.
- **Battery Management:** Capable of managing device charging dynamically (optionally using a Meross smart plug) to ensure tests run consistently unplugged while preventing devices from dying.

### 3. Experiment Results
When tests are executed by `ebserver`, the output logs are placed in the `experiment-results` folder at the root. The logs are organized recursively based on the testing framework, device, app, and exact testing parameters (e.g., `brightness`, `airplane_mode`, `refresh_rate`).

## Getting Started

### Prerequisites
- Node.js (v14+)
- Android SDK (ADB must be in your `PATH`)
- A connected Android device (USB or Wi-Fi) with Developer Options and USB Debugging enabled.

### Quick Start

1. **Install Server Dependencies:**
   ```bash
   cd ebserver
   npm install
   ```

2. **Configure your experiments:**
   Check out the [Configuration Guide](CONFIGURATION_GUIDE.md) to learn how to set up `config.json` to define your test suite and parameters.

3. **Run the Orchestrator:**
   From the `ebserver` folder, run:
   ```bash
   # Run the UI test queue, and optionally install the Android APKs automatically via gradle (-i)
   node src/ebserver.js -ui -i
   ```
   *Note: If you do not have a Meross smart plug configured, you can bypass it using the `--no-meross` flag.*

## Documentation

- [Ebserver README](./ebserver/README.md) - Deep dive into the Node.js server.
- [Configuration Guide](CONFIGURATION_GUIDE.md) - Details on how to structure `config.json` for custom testing matrices.
