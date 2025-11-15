const { networkInterfaces } = require("os");
const adb = require("./adbCommands");
const fs = require("fs");

async function run(args) {
  try {
    const configFilePath = "configUI/config.json";
    const configFileContent = fs.readFileSync(configFilePath, "utf8");
    const { config, general_parameters, tests } = JSON.parse(configFileContent);

    for (const test of tests) {
      const { class_name, short_form, skip_general, functions, setup_methods } =
        test;
      
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

        const sortedParameterCombinations = parameterCombinations.sort(
          (a, b) => {
            const fileNameA = buildFileName(short_form, functionName, a);
            const fileNameB = buildFileName(short_form, functionName, b);
            return fileNameA.localeCompare(fileNameB);
          }
        );

        for (const combination of sortedParameterCombinations) {
          const fileName = buildFileName(short_form, functionName, combination);

          // Prepare arguments for general parameters
          const generalArgs = combination
            .filter((param) => isGeneralParam(param.type, general_parameters))
            .reduce((acc, param) => {
              acc[param.type] = param.command;
              return acc;
            }, {});

          const generalArgsString = Object.entries(generalArgs)
            .map(([key, value]) => `-e ${key} ${value}`)
            .join(" ");

          // Setup general parameters
          const msg = await adb.startUITest(
            "Util",
            "setupGeneralParameters",
            config.package_name,
            config.suffix,
            generalArgsString
          );

          if (msg === "error") {
            console.error("Error in setupGeneralParameters");
            process.exit(1);
          }

          // Handle setup_methods
          if (setup_methods) {
            for (const [setupKey, setupValues] of Object.entries(
              setup_methods
            )) {
              for (const [paramKey, paramValue] of Object.entries(
                specificParameters[setupKey] || {}
              )) {
                if (
                  combination.some(
                    (param) => param.command === paramValue.command
                  )
                ) {
                  const setupMethod = setupValues[paramKey]?.command;
                  if (setupMethod) {
                    console.log(
                      `Calling specific setup method: ${class_name}#${setupMethod}`
                    );
                    await adb.startUITest(
                      class_name,
                      setupMethod,
                      config.package_name,
                      config.suffix
                    );
                  }
                }
              }
            }
          }

          // Prepare specific arguments for the function
          const specificArgs = combination
            .filter((param) => !isGeneralParam(param.type, general_parameters))
            .reduce((acc, param) => {
              acc[param.type] = param.command;
              return acc;
            }, {});

          const specificArgsString = Object.entries(specificArgs)
            .map(([key, value]) => `-e ${key} ${value}`)
            .join(" ");

          console.log(`Executing ${functionName} for: ${fileName}`);

          // Execute the test function multiple times
          for (let run = config.exec_start; run <= config.exec_end; run++) {
            process.stdout.write(`${run} `);
            await adb.startUITest(
              class_name,
              functionName.toLowerCase(), // Call the specific function
              config.package_name,
              config.suffix,
              specificArgsString // Pass specific parameters
            );

            // Save battery stats using the descriptive file name
            await adb.outputBatteryStatsTo(
              "",
              config.app_name,
              fileName,
              run,
              `${config.package_name}.${config.suffix}`
            );

            // Clean battery stats for the next test
            await adb.cleanBatteryStatus("");
          }

          // Reset default values
          console.log("Resetting default values");
          await adb.startUITest(
            "Util",
            "resetDefaultValues",
            config.package_name,
            config.suffix,
            generalArgsString
          );
        }
      }
    }
  } catch (error) {
    console.error(
      "Error reading or parsing the JSON configuration file:",
      error
    );
    process.exit(1);
  }
}

// Check if a parameter is general
function isGeneralParam(type, generalParameters) {
  return Object.keys(generalParameters).includes(type);
}

// Build descriptive file names dynamically
function buildFileName(shortForm, functionName, params) {
  return `${shortForm}_${functionName}_${params
    .map(({ type, command }) => `${type}-${command}`)
    .join("_")}`;
}

// Combine parameters dynamically
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

  // Combine general and specific parameters
  return generalCombinations.flatMap((g) =>
    specificCombinations.map((s) => [...s, ...g])
  );
}

// Cartesian product of arrays
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
