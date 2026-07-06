const meross = require('./src/merossCommands');

async function run() {
    console.log("Testing Meross Connection via merossCommands.js...");
    console.log("Attempting to turn ON...");
    await meross.turnPlugOn();
    
    console.log("Waiting 5 seconds...");
    await new Promise(r => setTimeout(r, 5000));
    
    console.log("Attempting to turn OFF...");
    await meross.turnPlugOff();
    
    console.log("Test finished. Exiting...");
    process.exit(0);
}

run();
