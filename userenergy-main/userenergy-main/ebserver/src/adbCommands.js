const fs = require("fs");
const util = require("util");
const exec = util.promisify(require("child_process").exec);

async function runCommand(command, targetDevice, dir, fileName) {
  try {
    await exec(
      `adb ${getTarget(targetDevice)} shell dumpsys ${command.name} ${
        command.params
      } > ${dir}/${command.name}-${fileName}`
    );
  } catch (error) {
    console.log(`ERROR EXECUTING ` + command.name + ` ON ${targetDevice}`);
  }
}

async function connectWifi() {
  try {
    const deviceCmd = await exec("adb devices");
    const strDev = deviceCmd.stdout.toString();
    const numDevices = strDev.split("\n").length - 3;

    if (numDevices > 1) {
      console.error(
        "More than one device connected. Make sure that there is only one device."
      );
      return "error";
    }

    const ipCmd = await exec("adb shell ip addr show wlan0");
    const ipRegex = /inet (\d+\.\d+\.\d+\.\d+)/;
    const ipMatch = ipCmd.stdout.toString().match(ipRegex);

    if (!ipMatch) {
      console.error(
        "Unable to extract WiFi IP address from the adb shell command output"
      );
      return "error";
    }

    const ip = ipMatch[1];
    console.log("Device IP Address: " + ip);
    await exec(`adb connect ${ip}:5555`);
    return ip;
  } catch (error) {
    console.error("Error running adb shell command:", error);
    return "error";
  }
}

async function bootstrapWifiConnections() {
  try {
    const deviceCmd = await exec("adb devices");
    const lines = deviceCmd.stdout.toString().split("\n");
    const usbDevices = [];

    for (let i = 1; i < lines.length; i++) {
      const line = lines[i].trim();
      if (line.endsWith("device")) {
        const deviceId = line.split("\t")[0];
        // If it doesn't have a colon and isn't an emulator, it's a USB device
        if (!deviceId.includes(":") && !deviceId.startsWith("emulator-")) {
           usbDevices.push(deviceId);
        }
      }
    }

    if (usbDevices.length === 0) {
      return;
    }

    console.log(`Bootstrapping WiFi for ${usbDevices.length} USB device(s)...`);

    for (const serial of usbDevices) {
      try {
        await exec(`adb -s ${serial} tcpip 5555`);
        await new Promise(r => setTimeout(r, 2000)); // wait for adbd to restart

        const ipCmd = await exec(`adb -s ${serial} shell ip addr show wlan0`);
        const ipMatch = ipCmd.stdout.toString().match(/inet (\d+\.\d+\.\d+\.\d+)/);

        if (ipMatch) {
          const ip = ipMatch[1];
          console.log(`Connecting to ${ip}:5555...`);
          await exec(`adb connect ${ip}:5555`);
        } else {
          console.error(`Could not find WiFi IP for ${serial}. Make sure WiFi is turned on.`);
        }
      } catch (e) {
        console.error(`Error bootstrapping ${serial}:`, e.message);
      }
    }
  } catch (error) {
    console.error("Error running adb devices for bootstrap:", error);
  }
}

async function getConnectedDevices() {
  try {
    const deviceCmd = await exec("adb devices");
    const lines = deviceCmd.stdout.toString().split("\n");
    const wifiDevices = [];
    const usbDevices = [];

    for (let i = 1; i < lines.length; i++) {
      const line = lines[i].trim();
      if (line.endsWith("device")) {
        const id = line.split("\t")[0];
        if (id.includes(":") || id.startsWith("emulator-")) {
          wifiDevices.push(id);
        } else {
          usbDevices.push(id);
        }
      }
    }

    // Favor WiFi/Emulator devices over raw USB to avoid duplicate runs
    if (wifiDevices.length > 0) {
      return wifiDevices;
    }
    return usbDevices;
  } catch (error) {
    console.error("Error running adb devices:", error);
    return [];
  }
}

function getTarget(targetDevice) {
  return targetDevice === "" ? " " : ` -s ${targetDevice} `;
}

async function cleanBatteryStatus(targetDevice = "") {
  try {
    await exec(
      `adb ${getTarget(targetDevice)} shell dumpsys procstats --clear`
    );
    await exec(
      `adb ${getTarget(targetDevice)} shell dumpsys batterystats --reset`
    );
  } catch (error) {
    throw new Error(`ERROR CLEANING STATUS ${targetDevice}: ${error.message}`);
  }
}

async function getBatteryLevel(targetDevice = "") {
  try {
    const { stdout } = await exec(`adb ${getTarget(targetDevice)} shell dumpsys battery`);
    const match = stdout.match(/level:\s*(\d+)/);
    if (match) {
      return parseInt(match[1], 10);
    }
    return 100;
  } catch (error) {
    console.log(`ERROR GETTING BATTERY LEVEL ${targetDevice}`);
    return 100;
  }
}

async function disableCharging(targetDevice = "") {
  try {
    await exec(`adb ${getTarget(targetDevice)} shell dumpsys battery unplug`);
  } catch (error) {
    console.log(`ERROR DISABLING CHARGING ${targetDevice}`);
  }
}

async function enableCharging(targetDevice = "") {
  try {
    await exec(`adb ${getTarget(targetDevice)} shell dumpsys battery reset`);
  } catch (error) {
    console.log(`ERROR ENABLING CHARGING ${targetDevice}`);
  }
}

async function outputBatteryStatsTo(
  targetDevice = "",
  framework,
  currentTest,
  counter,
  packageName
) {
  const fileName = `${counter}.txt`;
  const device = await exec(
    "adb" + getTarget(targetDevice) + " shell getprop ro.product.model"
  );

  const deviceName = device.stdout.trim().replace(/ /g, "-");
  const baseDir = "../experiment-results";
  const frameworkDir = `${baseDir}/${framework}`;
  const deviceDir = `${frameworkDir}/${deviceName}`;

  // Create folders dynamically based on the currentTest string
  const parameterFolders = currentTest.split("_").map((segment) => {
    const [key, value] = segment.split("-");
    return key && value ? `${key}-${value}` : segment;
  });

  // Dynamically build the directory path
  let currentPath = deviceDir;
  for (const folder of parameterFolders) {
    currentPath = `${currentPath}/${folder}`;
    createDirIfNotExists(fs, currentPath);
  }

  const dir = currentPath;

  const meminfo = { name: "meminfo", params: `${packageName} -d` };
  const batterystats = { name: "batterystats", params: "" };
  const procstats = { name: "procstats", params: "--hours 1" };

  // Save the results to the dynamically created directories
  await runCommand(meminfo, targetDevice, dir, fileName);
  await runCommand(batterystats, targetDevice, dir, fileName);
  await runCommand(procstats, targetDevice, dir, fileName);
}

// Helper function to ensure a directory exists
function createDirIfNotExists(fs, dir) {
  if (!fs.existsSync(dir)) {
    fs.mkdirSync(dir, {recursive: true});
  }
}

async function outputBatteryStatsTest(
  framework,
  currentTest,
  counter,
  packageName
) {
  outputBatteryStatsTo("", framework, currentTest, counter, packageName);
}

async function startApp(targetDevice, applicationId, mainActivity) {
  try {
    await exec(
      "adb" +
        getTarget(targetDevice) +
        " shell am start -n " +
        applicationId +
        "/" +
        mainActivity
    );
  } catch (error) {
    console.log(`ERROR STARTING APP ${targetDevice}`);
  }
}

async function startUITest(
  targetDevice,
  className,
  methodName,
  packageName,
  suffix,
  setupArgsString = ""
) {
  try {
    const command = `adb ${getTarget(targetDevice)} shell am instrument -w -e debug false ${setupArgsString} -e class ${packageName}.${className}#${methodName} ${packageName}.${suffix}/androidx.test.runner.AndroidJUnitRunner`;
    console.log(command);
    const { stdout } = await exec(command);

    if (stdout.includes("FAILURES!!!")) {
      throw new Error(stdout);
    }
  } catch (error) {
    console.error(`Error occurred during test execution: ${error.message}`);
    return "error";
  }
}

async function killApp(targetDevice, applicationId) {
  try {
    await exec(
      "adb" + getTarget(targetDevice) + " shell pm clear " + applicationId
    );
  } catch (error) {
    console.log(`ERROR KILLING APP ${targetDevice} ${applicationId}`);
  }
}

async function done(targetDevice, applicationId, mainActivity) {
  await killApp(targetDevice, applicationId);
  await startApp(targetDevice, applicationId, mainActivity);
}



function extractParamsFromMethodName(methodName) {
  const parts = methodName.split("_");
  const params = {};

  parts.forEach((part) => {
    if (part.includes("-")) {
      const [key, value] = part.split("-");
      params[key] = value;
    }
  });

  return params;
}

module.exports = {
  cleanBatteryStatus,
  getBatteryLevel,
  disableCharging,
  enableCharging,
  outputBatteryStatsTo,
  outputBatteryStatsTest,
  done,
  startUITest,
  connectWifi,
  bootstrapWifiConnections,
  getConnectedDevices,
};
