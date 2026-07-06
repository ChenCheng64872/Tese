const { networkInterfaces } = require("os");
const adbCommands = require("./adbCommands");
const iosCommands = require("./iosCommands");
const fs = require("fs");
const path = require("path");
const Joi = require("joi");

const QUEUE_FILENAME = "experiments_queue.json";

async function run(args, devices = [""]) {
  console.log("Inside ui.run", args, devices);
  try {
    let experimentsQueue = [];
    const useMeross = !(args.includes('--no-meross') || process.env.USE_MEROSS === 'false');
    const meross = useMeross ? require("./merossCommands") : null;
    const isIOS = args.includes('--ios');
    const adb = isIOS ? iosCommands : adbCommands;

    // Setup graceful exit
    process.on('SIGINT', async () => {
      console.log("\n[SIGINT] Interrupted by user. Cleaning up...");
      if (useMeross) {
        try {
          console.log("Turning Meross Plug ON before exiting...");
          await meross.turnPlugOn();
        } catch (err) {
          console.error("Failed to turn plug on during cleanup:", err);
        }
      }
      console.log("Cleanup complete. Exiting.");
      process.exit(1);
    });

    // ============================================================
    // PHASE 1: PREPARATION
    // Check if the queue file exists. If not, generate it.
    // ============================================================

    if (fs.existsSync(QUEUE_FILENAME)) {
      console.log(`\nFound existing '${QUEUE_FILENAME}'. Loading experiments from file...`);
      const fileContent = fs.readFileSync(QUEUE_FILENAME, "utf8");
      experimentsQueue = JSON.parse(fileContent);
      console.log(`Loaded ${experimentsQueue.length} experiments to run.\n`);
    } else {
      console.log(`\n'${QUEUE_FILENAME}' not found. Generating from config...`);
      try {
          experimentsQueue = generateExperimentsQueue();
          console.log(`Generation complete.`);
      } catch (err) {
          console.log("Error generating queue:", err);
          throw err;
      }

      fs.writeFileSync(QUEUE_FILENAME, JSON.stringify(experimentsQueue, null, 2), "utf8");
      console.log(`Generated and saved ${experimentsQueue.length} experiments to '${QUEUE_FILENAME}'.`);
      console.log(`Starting execution now...\n`);
    }

    // ============================================================
    // PHASE 2: EXECUTION
    // Iterate through the loaded queue and run ADB commands
    // ============================================================

    const totalExperiments = experimentsQueue.length;

    async function worker(device) {
      let completedCount = 0;
      const perfettoSupported = await adb.checkPerfettoSupport(device);
      console.log(`[Device: ${device || 'default'}] Perfetto power rails: ${perfettoSupported ? 'supported' : 'not supported'}`);

      for (let i = 0; i < experimentsQueue.length; i++) {
        const exp = experimentsQueue[i];

        console.log(`\n=======================================================`);
        console.log(`[Device: ${device || 'default'}] EXPERIMENT RUNNING...`);
        console.log(`Function: ${exp.meta.functionName}`);
        console.log(`File:     ${exp.output.fileName}`);
        console.log(`=======================================================`);

        let batteryLevel = await adb.getBatteryLevel(device);
        if (batteryLevel < 25) {
          console.log(`[Device: ${device || 'default'}] Battery is at ${batteryLevel}%. Pausing experiments to charge...`);
          if (useMeross) {
            await meross.turnPlugOn();
          } else {
            console.log(`[Device: ${device || 'default'}] ⚠️ PLEASE MANUALLY PLUG IN THE DEVICE TO CHARGE ⚠️`);
          }
          
          while (batteryLevel < 95) {
            await new Promise(resolve => setTimeout(resolve, 60000));
            batteryLevel = await adb.getBatteryLevel(device);
            console.log(`[Device: ${device || 'default'}] Charging... current level: ${batteryLevel}%`);
          }
          console.log(`[Device: ${device || 'default'}] Battery reached ${batteryLevel}%. Resuming experiments.`);
          
          if (!useMeross) {
            console.log(`[Device: ${device || 'default'}] ⚠️ PLEASE MANUALLY UNPLUG THE DEVICE NOW ⚠️`);
            console.log(`[Device: ${device || 'default'}] Waiting 10 seconds before resuming...`);
            await new Promise(resolve => setTimeout(resolve, 10000));
          }
        }

        if (useMeross) {
          await meross.turnPlugOff();
        }

        // 1. Setup General Parameters
        // ---------------------------
        const generalMsg = await adb.startUITest(
          device,
          "Util",
          "setupGeneralParameters",
          exp.env.packageName,
          exp.env.suffix,
          exp.params.generalArgsString
        );

        if (generalMsg === "error") {
          console.error(`CRITICAL ERROR: Failed in setupGeneralParameters on device ${device}`);
          process.exit(1);
        }

        // 2. Setup Specific Methods (if defined)
        // --------------------------------------
        if (exp.setup && exp.setup.specific.length > 0) {
          for (const setupStep of exp.setup.specific) {
            console.log(`[Device: ${device || 'default'}] > Running setup method: ${setupStep.className}#${setupStep.method}`);
            await adb.startUITest(
              device,
              setupStep.className,
              setupStep.method,
              exp.env.packageName,
              exp.env.suffix
            );
          }
        }

        // 3. Main Execution Loop
        // ----------------------
        console.log(`[Device: ${device || 'default'}] > Executing ${exp.execution.functionName} (Runs ${exp.execution.runRange.start} to ${exp.execution.runRange.end})`);

        for (let run = exp.execution.runRange.start; run <= exp.execution.runRange.end; run++) {
          process.stdout.write(`[Device: ${device || 'default'}] Run ${run}... \n`);

          // Resolve output directory once so all measurement sources write to the same place
          const outputDir = await adb.buildOutputDir(device, exp.env.appName, exp.output.fileName);

          // Start measurement sources before the test
          const batteryPoller = await adb.startBatteryPolling(device);
          let perfettoPid = null;
          if (perfettoSupported) {
            perfettoPid = await adb.startPerfettoTrace(device);
          }

          // Run the specific test function
          await adb.startUITest(
            device,
            exp.execution.className,
            exp.execution.functionName,
            exp.env.packageName,
            exp.env.suffix,
            exp.params.specificArgsString
          );

          // Stop measurement sources after the test
          if (perfettoSupported) {
            await adb.stopAndPullPerfettoTrace(device, perfettoPid, outputDir, run);
          }
          await batteryPoller.stop(outputDir, run);

          // Save battery stats
          await adb.outputBatteryStatsTo(
            device,
            exp.env.appName,
            exp.output.fileName,
            run,
            `${exp.env.packageName}.${exp.env.suffix}`
          );

          // Clean battery stats for next run
          await adb.cleanBatteryStatus(device);
          console.log(`[Device: ${device || 'default'}] Run ${run} Done.`);
        }

        // 4. Reset Default Values
        // -----------------------
        console.log(`[Device: ${device || 'default'}] > Resetting default values`);
        await adb.startUITest(
          device,
          "Util",
          "resetDefaultValues",
          exp.env.packageName,
          exp.env.suffix,
          exp.params.generalArgsString
        );

        completedCount++;
        console.log(`[Device: ${device || 'default'}] Completed ${completedCount}/${totalExperiments}`);
      }
    }

    await Promise.all(devices.map(device => worker(device)));

    for (const device of devices) {
      if (!(args.includes('--no-meross') || process.env.USE_MEROSS === 'false')) {
        await meross.turnPlugOn();
      }
    }

    console.log("\nAll experiments in the queue have been executed successfully.");
    process.exit(0);

  } catch (error) {
    console.error("An error occurred during execution:", error);
    process.exit(1);
  }
}

/**
 * Reads config.json and returns an array of experiment objects
 */
function generateExperimentsQueue() {
  const configFilePath = path.join(__dirname, "configUI/config.json");
  const configFileContent = fs.readFileSync(configFilePath, "utf8");
  const rawConfig = JSON.parse(configFileContent);

  // Validate config using Joi
  const schema = Joi.object({
    config: Joi.object({
      app_name: Joi.string().required(),
      package_name: Joi.string().required(),
      suffix: Joi.string().required(),
      exec_start: Joi.number().integer().min(1).required(),
      exec_end: Joi.number().integer().min(1).required()
    }).required(),
    general_parameters: Joi.object().pattern(
      Joi.string(),
      Joi.object().pattern(
        Joi.string(),
        Joi.object({ command: Joi.string().required() })
      )
    ).optional(),
    tests: Joi.array().items(
      Joi.object({
        class_name: Joi.string().required(),
        short_form: Joi.string().required(),
        skip_general: Joi.array().items(Joi.object({ command: Joi.string().required() })).optional(),
        functions: Joi.array().items(
          Joi.object({
            name: Joi.string().required(),
            parameters: Joi.object().pattern(
              Joi.string(),
              Joi.object().pattern(
                Joi.string(),
                Joi.object({ command: Joi.string().required() })
              )
            ).optional()
          })
        ).required(),
        setup_methods: Joi.object().optional()
      }).unknown(true)
    ).required()
  }).unknown(true);

  const { error, value: parsedConfig } = schema.validate(rawConfig, { abortEarly: false });
  
  if (error) {
    console.error("❌ ERROR: config.json validation failed!");
    error.details.forEach(detail => console.error(` - ${detail.message}`));
    process.exit(1);
  }

  const { config, general_parameters, tests } = parsedConfig;

  const queue = [];

  for (const test of tests) {
    const { class_name, short_form, skip_general, functions, setup_methods } = test;

    // Handle skip_general logic
    const generalParams = { ...general_parameters };
    if (skip_general) {
      skip_general.forEach((skipParam) => {
        delete generalParams[skipParam.command];
      });
    }

    for (const func of functions) {
      const { name: functionName, parameters: specificParameters } = func;

      const parameterCombinations = combineParameters(
        generalParams,
        specificParameters
      );

      // Sort to ensure consistent naming order
      const sortedParameterCombinations = parameterCombinations.sort((a, b) => {
        const fileNameA = buildFileName(short_form, functionName, a);
        const fileNameB = buildFileName(short_form, functionName, b);
        return fileNameA.localeCompare(fileNameB);
      });

      for (const combination of sortedParameterCombinations) {
        const fileName = buildFileName(short_form, functionName, combination);

        // Prepare General Arguments String
        const generalArgs = combination
          .filter((param) => isGeneralParam(param.type, general_parameters))
          .reduce((acc, param) => {
            acc[param.type] = param.command;
            return acc;
          }, {});

        const generalArgsString = Object.entries(generalArgs)
          .map(([key, value]) => `-e ${key} ${value}`)
          .join(" ");

        // Identify Specific Setup Methods
        const requiredSetupMethods = [];
        if (setup_methods) {
          for (const [setupKey, setupValues] of Object.entries(setup_methods)) {
            for (const [paramKey, paramValue] of Object.entries(
              specificParameters[setupKey] || {}
            )) {
              if (combination.some((param) => param.command === paramValue.command)) {
                const setupMethod = setupValues[paramKey]?.command;
                if (setupMethod) {
                  requiredSetupMethods.push({
                    className: class_name,
                    method: setupMethod,
                  });
                }
              }
            }
          }
        }

        // Prepare Specific Arguments String
        const specificArgs = combination
          .filter((param) => !isGeneralParam(param.type, general_parameters))
          .reduce((acc, param) => {
            acc[param.type] = param.command;
            return acc;
          }, {});

        const specificArgsString = Object.entries(specificArgs)
          .map(([key, value]) => `-e ${key} ${value}`)
          .join(" ");

        // Build the experiment object
        queue.push({
          meta: {
            shortForm: short_form,
            functionName: functionName,
          },
          env: {
            packageName: config.package_name,
            appName: config.app_name,
            suffix: config.suffix,
          },
          execution: {
            className: class_name,
            functionName: functionName.toLowerCase(),
            runRange: { start: config.exec_start, end: config.exec_end },
          },
          params: {
            generalArgsString: generalArgsString,
            specificArgsString: specificArgsString,
          },
          setup: {
            specific: requiredSetupMethods,
          },
          output: {
            fileName: fileName,
          },
        });
      }
    }
  }

  return queue;
}

// ---------------------------------------------------------
// HELPER FUNCTIONS
// ---------------------------------------------------------

function isGeneralParam(type, generalParameters) {
  return Object.keys(generalParameters).includes(type);
}

function buildFileName(shortForm, functionName, params) {
  return `${shortForm}_${functionName}_${params
    .map(({ type, command }) => `${type}-${command}`)
    .join("_")}`;
}

function combineParameters(generalParams, specificParams) {
  const generalCombinations = cartesian(
    Object.entries(generalParams).map(([type, options]) =>
      Object.values(options).map((o) => ({ type, command: o.command }))
    )
  );

  const specificCombinations = cartesian(
    Object.entries(specificParams || {}).map(([type, options]) =>
      Object.values(options).map((o) => ({ type, command: o.command }))
    )
  );

  return generalCombinations.flatMap((g) =>
    specificCombinations.map((s) => [...s, ...g])
  );
}

function cartesian(arrays) {
  return arrays.reduce(
    (acc, array) =>
      acc.flatMap((accItem) => array.map((item) => [...accItem, item])),
    [[]]
  );
}

module.exports = {
  run,
};