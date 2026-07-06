
# Ebserver Configuration Guide

This guide describes configuring **Ebserver** to automate experiments and tests across various mobile applications. The configuration file is used to handle tests and ensure consistent execution across different scenarios dynamically.

---

## Configuration File Overview

The configuration file is structured as follows:

### Example JSON
```json
{
    "config": {
        "app_name": "MYAPP",
        "package_name": "my.package",
        "suffix": "test",
        "exec_start": 1,
        "exec_end": 15
    },
    "general_parameters": {
        "brightness": {
            "zero": { "command": "zero" },
            "quart": { "command": "quarter" },
            "half": { "command": "half" },
            "threeq": { "command": "threeq" },
            "full": { "command": "full" }
        },
        "theme": {
            "light": { "command": "light" },
            "dark": { "command": "dark" }
        },
        "airplane": {
            "on": { "command": "on" },
            "off": { "command": "off" }
        },
        "saving": {
            "on": { "command": "on" },
            "off": { "command": "off" }
        },
        "refresh": {
            "fps_30": { "command": "fps30" },
            "fps_60": { "command": "fps60" },
            "fps_120": { "command": "fps120" }
        }
    },
    "tests": [
        {
            "class_name": "WhatsAppTest",
            "short_form": "WPP",
            "functions": [
                {
                    "name": "send_message"
                }
            ]
        },
        {
            "class_name": "GoogleKeepTest",
            "short_form": "KEEP",
            "functions": [
                {
                    "name": "create_note"
                }
            ]
        },
        {
            "class_name": "YouTubeTest",
            "short_form": "YT",
            "skip_general": [
                { "command": "theme" }
            ],
            "functions": [
                {
                    "name": "play_video"
                }
            ]
        },
        {
            "class_name": "FlashlightTest",
            "short_form": "LIGHT",
            "skip_general": [
                { "command": "brightness" },
                { "command": "refresh" },
                { "command": "airplane" },
                { "command": "saving" },
                { "command": "theme" }
            ],
            "functions": [
                {
                    "name": "runtest",
                    "parameters": {
                        "duration": {
                            "quart_min": { "command": "s15" },
                            "half_min": { "command": "s30" },
                            "one_min": { "command": "s60" }
                        },
                        "flashpower": {
                            "max": { "command": "max" },
                            "medium": { "command": "half" },
                            "quart": { "command": "quarter" },
                            "min": { "command": "min" },
                            "off": { "command": "off" }
                        }
                    }
                }
            ]
        }
    ]
}
```

---

## Configuration Breakdown

### General Configurations
The `config` section contains global settings:
```json
"config": {
    "app_name": "MYAPP",
    "package_name": "my.package",
    "suffix": "test",
    "exec_start": 1,
    "exec_end": 15
}
```
- **`app_name`**: Name of the application.
- **`package_name`**: Package name of the app.
- **`suffix`**: Used for running tests (e.g., `"test"`).
- **`exec_start`** & **`exec_end`**: Define the range of executions.

### General Parameters
The `general_parameters` section defines configurations applied to all tests:
```json
"general_parameters": {
    "brightness": { ... },
    "theme": { ... },
    "airplane": { ... },
    "saving": { ... },
    "refresh": { ... }
}
```
Each key corresponds to a specific setting, such as brightness or airplane mode, that can be applied across tests.

### Test-Specific Configurations
The `tests` section defines individual test cases:
- **`class_name`**: The test class to execute.
- **`short_form`**: A shorthand used in naming (e.g., `WPP` for WhatsAppTest).
- **`functions`**: *(Required)* A list of test functions inside the class to execute. Each function must specify a `name` and optional `parameters`.
- **`skip_general`**: List of general parameters that should not apply to the test.
- **`setup_methods`**: Functions executed before each test. This leaves room for specific behavior for an application that does not fit the general parameters.
Example: search_user for Instagram.
It's essential to notice that the **setup_methods** function invocation is based on one of the parameters of the class. In the example, the **user_name**

For example:
```json
{
    "class_name": "InstagramTest",
    "short_form": "IG",
    "skip_general": [
        { "command": "theme" }
    ],
    "functions": [
        {
            "name": "view_profile",
            "parameters": {
                "user_name": {
                    "jane": { "command": "jane" },
                    "john": { "command": "john" }
                }
            }
        }
    ],
    "setup_methods": {
        "user_name": {
            "jane": { "command": "search_jane" },
            "john": { "command": "search_john" }
        }
    }
}
```

### Example Method Names
Based on the configuration, Ebserver generates method names like:
```plaintext
WPP_brightness-full_theme-dark_airplane-off_saving-off_refresh-fps120
KEEP_brightness-zero_theme-light_airplane-off_saving-off_refresh-fps120
YT_brightness-full_airplane-off_saving-off_refresh-fps30
LIGHT_duration-s15_flash_power-half
```

---

## How to Use
1. Define global configurations in the `general_parameters` section.
2. Add test-specific configurations under the `tests` section.
3. Skip unnecessary parameters using `skip_general`.
4. Use `exec_start` and `exec_end` to control execution ranges.
5. Run Ebserver, and it will handle all combinations dynamically.

If you want more details, please consult the project's README or documentation.
