const Meross = require('meross-iot');
require('dotenv').config();

const options = {
  email: process.env.MEROSS_EMAIL,
  password: process.env.MEROSS_PASSWORD
};

let merossInstance = null;
let plugDevice = null;
let connectionPromise = null;

async function initMeross() {
  if (plugDevice) return plugDevice;
  
  if (!options.email || !options.password) {
    console.error("Meross credentials (MEROSS_EMAIL, MEROSS_PASSWORD) are missing in .env");
    return null;
  }

  if (!connectionPromise) {
    connectionPromise = (async () => {
      try {
        console.log("Connecting to Meross Cloud...");
        merossInstance = await Meross.connect(options);
        
        // Wait a brief moment for devices to populate
        await new Promise(r => setTimeout(r, 2000));
        
        const devices = merossInstance.devices.list();
        for (const device of devices) {
          if (device.name && device.name.toLowerCase() === 'smart plug') {
            plugDevice = device;
            break;
          }
        }
        
        if (!plugDevice && devices.length > 0) {
          // Fallback to the first device if "smart plug" isn't found exactly
          console.log(`Could not find a device strictly named 'smart plug'. Defaulting to first device: ${devices[0].name}`);
          plugDevice = devices[0];
        }

        if (!plugDevice) {
          console.error("No Meross devices found on the account.");
        } else {
          console.log(`Successfully bound to Meross device: ${plugDevice.name}`);
        }
        
        return plugDevice;
      } catch (err) {
        console.error("Error connecting to Meross:", err);
        return null;
      }
    })();
  }
  return connectionPromise;
}

async function turnPlugOn() {
  try {
    const device = await initMeross();
    if (device && device.toggle) {
      console.log(`Turning ON ${device.name}...`);
      await device.toggle.set({ channel: 0, on: true });
      console.log("Meross Plug turned ON.");
    } else {
      console.error("Device does not support toggle commands.");
    }
  } catch (e) {
    console.error("Error turning plug on:", e);
  }
}

async function turnPlugOff() {
  try {
    const device = await initMeross();
    if (device && device.toggle) {
      console.log(`Turning OFF ${device.name}...`);
      await device.toggle.set({ channel: 0, on: false });
      console.log("Meross Plug turned OFF.");
    } else {
      console.error("Device does not support toggle commands.");
    }
  } catch (e) {
    console.error("Error turning plug off:", e);
  }
}

module.exports = {
  turnPlugOn,
  turnPlugOff
};
