const fs = require("fs");
const util = require("util");
const exec = util.promisify(require("child_process").exec);
const path = require("path");

function createDirIfNotExists(fs, dir) {
  if (!fs.existsSync(dir)) {
    fs.mkdirSync(dir, {recursive: true});
  }
}

async function getConnectedDevices() {
  // Since Xcode handles devices, we just return the hardcoded or detected UDID.
  // We can assume the user passes `--device "UDID"` which populates `devices`.
  // If not provided, we fallback to a default generic name or look for it in the args later.
  return []; // UIModule handles device parsing, we just return empty array if called directly for discovery.
}

async function connectWifi() { return "127.0.0.1"; }
async function bootstrapWifiConnections() { return; }

async function cleanBatteryStatus(targetDevice = "") { return; }
async function getBatteryLevel(targetDevice = "") { return 100; }
async function disableCharging(targetDevice = "") { return; }
async function enableCharging(targetDevice = "") { return; }

async function buildOutputDir(targetDevice = "", framework, currentTest) {
  const deviceName = targetDevice.replace(/ /g, "-") || "iOS-Device";
  const baseDir = "../experiment-results";

  const parameterFolders = currentTest.split("_").map((segment) => {
    const [key, value] = segment.split("-");
    return key && value ? `${key}-${value}` : segment;
  });

  let currentPath = `${baseDir}/${framework}/${deviceName}`;
  for (const folder of parameterFolders) {
    currentPath = `${currentPath}/${folder}`;
    createDirIfNotExists(fs, currentPath);
  }

  return currentPath;
}

async function outputBatteryStatsTo(targetDevice, framework, currentTest, counter, packageName) {
  // iOS doesn't have CLI batterystats without Instruments. Stubbing to satisfy server flow.
  const dir = await buildOutputDir(targetDevice, framework, currentTest);
  fs.writeFileSync(`${dir}/${counter}.txt`, "iOS Energy Stats via ebserver not supported. Use Xcode Instruments/sysdiagnose.");
}

async function outputBatteryStatsTest(framework, currentTest, counter, packageName) {
  await outputBatteryStatsTo("", framework, currentTest, counter, packageName);
}

async function startBatteryPolling(targetDevice = "") {
  return { stop: async (dir, counter) => { return; } };
}

async function checkPerfettoSupport(targetDevice = "") { return false; }
async function startPerfettoTrace(targetDevice = "") { return null; }
async function stopAndPullPerfettoTrace(targetDevice = "", pid, dir, counter) { return; }

async function done(targetDevice, applicationId, mainActivity) { return; }

// --- iOS Specific xcodebuild logic ---

function extractEnvFromArgs(argsString) {
  if (!argsString) return "";
  const parts = argsString.split("-e ");
  let envs = [];
  parts.forEach(part => {
    part = part.trim();
    if (part) {
      const split = part.split(" ");
      const key = split[0].toUpperCase();
      const value = split.slice(1).join(" ").toUpperCase();
      envs.push(`${key}="${value}"`);
    }
  });
  return envs.join(" ");
}

function toIOSMethodName(name) {
  // play_video -> testPlayVideo
  // sendmessage -> testSendMessage
  return 'test' + name.split('_').map(w => w.charAt(0).toUpperCase() + w.slice(1)).join('');
}

async function startUITest(targetDevice, className, methodName, packageName, suffix, setupArgsString = "") {
  try {
    // If it's a utility call from Util or UtilManual (Android specific), skip it on iOS
    if (className === "Util" || className === "UtilManual") {
        console.log(`[iOS] Skipping Android specific utility setup: ${methodName}`);
        return;
    }

    const iosClassName = className.includes("UITest") ? className : className.replace("Test", "UITest");
    const iosMethodName = toIOSMethodName(methodName);
    const envVars = extractEnvFromArgs(setupArgsString);
    
    const projectPath = path.resolve(__dirname, "../../UserEnergyIOS/UserEnergyIOS.xcodeproj");
    const scheme = "UserEnergyIOS";
    const destination = `id=${targetDevice}`;

    const command = `xcodebuild test -project ${projectPath} -scheme ${scheme} -destination '${destination}' -allowProvisioningUpdates -only-testing:UserEnergyIOSUITests/${iosClassName}/${iosMethodName} ${envVars}`;
    
    console.log(`[iOS Executing]: ${command}`);
    const { stdout, stderr } = await exec(command);
    
    if (stdout.includes("TEST FAILED") || stderr.includes("TEST FAILED")) {
      throw new Error("xcodebuild test failed");
    }
    
  } catch (error) {
    console.error(`Error occurred during iOS test execution: ${error.message}`);
    return "error";
  }
}

module.exports = {
  cleanBatteryStatus,
  getBatteryLevel,
  disableCharging,
  enableCharging,
  buildOutputDir,
  outputBatteryStatsTo,
  outputBatteryStatsTest,
  startBatteryPolling,
  checkPerfettoSupport,
  startPerfettoTrace,
  stopAndPullPerfettoTrace,
  done,
  startUITest,
  connectWifi,
  bootstrapWifiConnections,
  getConnectedDevices,
};
