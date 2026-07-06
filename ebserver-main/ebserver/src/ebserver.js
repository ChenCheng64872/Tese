console.log("ARGV:", process.argv);
const express = require("express");
const cors = require("cors");
const { execSync } = require("child_process"); 
const path = require("path");
const session = require("./session");
const adb = require("./adbCommands");
const ui = require("./UIModule");

const app = express();
app.use(cors());

const timeout_seconds = 120;
let timerObj = {};
function clearTimerObj(device) {
  if (timerObj[device]) {
    clearTimeout(timerObj[device]);
    delete timerObj[device];
  }
}

async function what_now(
  ip,
  device,
  test_type,
  application_id,
  activity,
  timerObj
) {
  await adb.cleanBatteryStatus(ip);
  const [execution, execution_number] = session.getCurrentExecution(
    device,
    test_type
  );
  console.log(
    ` + fetch ${device} ${test_type}, ${execution}, ${execution_number}`
  );
  clearTimerObj(device);
  timerObj[device] = setTimeout(() => {
    console.log(`timeout ${device}-${execution}`);
    done(ip, device, test_type, application_id, activity);
  }, timeout_seconds * 1000);
  return execution;
}

async function logdata(ip, device, test_type, framework, application_id) {
  clearTimerObj(device);
  const [execution, execution_number] = session.getCurrentExecution(
    device,
    test_type
  );
  console.log(
    ` | logdata ${device} ${test_type}, ${execution}, ${execution_number}`
  );
  await adb.outputBatteryStatsTo(
    ip,
    framework,
    execution,
    execution_number,
    application_id
  );
}

async function done(ip, device, test_type, application_id, activity) {
  const [execution, execution_number] = session.getCurrentExecution(
    device,
    test_type
  );
  console.log(
    ` - done ${device} ${test_type}, ${execution}, ${execution_number}`
  );
  session.updateExecution(device, test_type);
  await adb.done(ip, application_id, activity);
}

app.get("/", (req, res) => {
  res.send("index");
});

app.get("/fetch", async (req, res) => {
  let ip = req.ip.includes("::ffff:") ? req.ip.split("::ffff:")[1] : (req.ip === "::1" ? "127.0.0.1" : req.ip);
  if (ip.includes(":")) ip = ip.substring(ip.lastIndexOf(":") + 1);
  ip = ip + ":5555";
  const execution = await what_now(
    ip,
    req.headers.device,
    req.headers.test_type,
    req.headers.application_id,
    req.headers.activity,
    timerObj
  );
  res.send(execution);
});

app.get("/logdata", async (req, res) => {
  let ip = req.ip.includes("::ffff:") ? req.ip.split("::ffff:")[1] : (req.ip === "::1" ? "127.0.0.1" : req.ip);
  if (ip.includes(":")) ip = ip.substring(ip.lastIndexOf(":") + 1);
  ip = ip + ":5555";
  await logdata(
    ip,
    req.headers.device,
    req.headers.test_type,
    req.headers.framework,
    req.headers.application_id
  );
  res.send("done");
});

app.get("/done", async (req, res) => {
  let ip = req.ip.includes("::ffff:") ? req.ip.split("::ffff:")[1] : (req.ip === "::1" ? "127.0.0.1" : req.ip);
  if (ip.includes(":")) ip = ip.substring(ip.lastIndexOf(":") + 1);
  ip = ip + ":5555";

  await logdata(
    ip,
    req.headers.device,
    req.headers.test_type,
    req.headers.framework,
    req.headers.application_id
  );

  await done(
    ip,
    req.headers.device,
    req.headers.test_type,
    req.headers.application_id,
    req.headers.activity
  );

  res.send("");
});

if (process.argv[2] != "-ui") {
  app.listen(3000, () => {
    console.log("Ebserver listening on port " + 3000 + "!");
  });
}

readInput();

async function readInput() {
  let devices = [];
  if (process.argv[2] == "-ui") {
    console.log(">> Running UI Queue Mode");
    let isIOS = process.argv.includes("--ios");

    if (isIOS) {
      console.log(">> Running iOS UI Queue Mode");
      // Find device id from --device flag
      const deviceIdx = process.argv.indexOf("--device");
      if (deviceIdx !== -1 && process.argv.length > deviceIdx + 1) {
        devices = [process.argv[deviceIdx + 1]];
      } else {
        console.error("❌ ERROR: You must provide a device UDID using --device <UDID> when running in --ios mode.");
        process.exit(1);
      }
    } else {
      await adb.bootstrapWifiConnections();
      devices = await adb.getConnectedDevices();
      if (devices.length === 0) {
        console.error("No Android devices connected.");
        process.exit(1);
      }
    }
    
    console.log(">> Running UI Mode on devices:", devices);

    // Check for the -i (install) flag
    if (process.argv[3] == "-i") {
      console.log(">> Installing UserEnergy App via Gradle...");

      try {
        const projectRoot = path.resolve(__dirname, '../../UserEnergy');
        console.log(`-- Project root detected at: ${projectRoot}`);

        //Change based on the OS
        const gradlew = process.platform === 'win32' ? 'gradlew.bat' : './gradlew';
        console.log(`-- Using Gradle command: ${gradlew}`);

        const gradleOptions = {
          cwd: projectRoot,
          stdio: 'inherit'
        };

        // Install the main debug APK
        console.log(`\n-- Running: ${gradlew} installDebug`);
        execSync(`${gradlew} installDebug`, gradleOptions);
        console.log(">> Main app installed successfully.");

        // Install the test APK
        console.log(`\n-- Running: ${gradlew} installDebugAndroidTest`);
        execSync(`${gradlew} installDebugAndroidTest`, gradleOptions);
        console.log(">> Test app installed successfully.");

        console.log("\n✅ All APKs installed!");

      } catch (error) {
        console.error("\n❌ ERROR: Gradle installation failed.");
        process.exit(1);
      }
    }
    ui.run(process.argv, devices);

  } else {
    let ip = await adb.connectWifi();
    if (ip == "error") {
      process.exit(1);
    }
    console.log(">> Running Individual Executions Mode");
  }
}
