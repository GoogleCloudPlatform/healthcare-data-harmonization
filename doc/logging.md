# Package logging

[TOC]

## Import

The logging package can be imported by adding this code to the top of your Whistle file:


```
import "class://com.google.cloud.verticals.foundations.dataharmonization.plugins.logging.LoggingPlugin"
```


## Targets
### logInfo
`logging::logInfo(): ...`

#### Arguments

#### Description
Target which logs a String at the `INFO` level. Takes in no parameters.

Examples Usage: `logging:logInfo(): StringToLog;`

### logSevere
`logging::logSevere(): ...`

#### Arguments

#### Description
Target which logs a String at the `SEVERE` level. Takes in no parameters.

Examples Usage: `logging::logSevere(): StringToLog;`

### logWarning
`logging::logWarning(): ...`

#### Arguments

#### Description
Target which logs a String at the `WARNING` level. Takes in no parameters.

Examples Usage: `logging::logWarning(): StringToLog;`

