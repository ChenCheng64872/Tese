const fs = require("fs");

function generateMethodNames(jsonFilePath, outputFilePath) {
  try {
    // Load and parse the JSON configuration
    const jsonContent = fs.readFileSync(jsonFilePath, "utf8");
    const { config, general_parameters, tests } = JSON.parse(jsonContent);

    const methodNames = [];

    // Iterate through each test case
    for (const test of tests) {
      const {
        class_name,
        short_form,
        setup_methods,
        parameters,
        skip_general,
      } = test;

      // Identify general parameters to include
      const includedGeneralParams = { ...general_parameters };
      if (skip_general) {
        const keysToSkip = Array.isArray(skip_general)
          ? skip_general
          : [skip_general.command];

        for (const key of keysToSkip) {
          delete includedGeneralParams[key];
        }
      }

      // Generate parameter combinations
      const generalCombinations = combineParameters(includedGeneralParams);
      const specificCombinations = combineParameters(parameters);

      // Generate method names based on parameter combinations
      for (const generalCombination of generalCombinations) {
        for (const specificCombination of specificCombinations) {
          const combinedParams = [
            ...specificCombination,
            ...generalCombination,
          ];
          const methodName = buildMethodName(short_form, combinedParams);

          // Ignore methods with AirOff
          if (methodName.includes("AirOff")) continue;

          methodNames.push(methodName);
        }
      }
    }

    // Write the method names to the output file
    fs.writeFileSync(outputFilePath, methodNames.join("\n"), "utf8");
    console.log(`Method names successfully written to ${outputFilePath}`);
  } catch (error) {
    console.error("Error generating method names:", error);
  }
}

// Build method names dynamically
function buildMethodName(shortForm, params) {
  return `test_${shortForm}_${params.join("_")}`;
}

// Combine parameters dynamically
function combineParameters(params) {
  const paramArrays = Object.values(params).map((options) =>
    Object.values(options).map((o) => o.command)
  );

  return cartesian(paramArrays);
}

// Cartesian product of arrays
function cartesian(arrays) {
  return arrays.reduce(
    (acc, array) =>
      acc.flatMap((accItem) => array.map((item) => [...accItem, item])),
    [[]]
  );
}

// Example usage
generateMethodNames(
  "configUI/config.json", // Path to your JSON configuration file
  "generated_method_names.txt" // Output file for method names
);
