---
title: Package
---

RSDB package does contain all files needed to run RSDB.

Prebuilt packages can be found at [https://github.com/environmentalinformatics-marburg/rsdb/releases](https://github.com/environmentalinformatics-marburg/rsdb/releases)

## Package Content

| package content | description |
| ------------- | ------------- |
| **lib/**  | Java libraries |
| **webcontent/**  | web frontend files |
| **webfiles/**  | user generated files accessible from web frontend |
| **[config.yaml](../config.yaml)**  | config file: general RSDB settings |
| **[github_update.sh](../server_update)**  | update script |
| **[realm.properties](../realm.properties)**  | config file: local user accounts |
| **[realm_ip.csv](../realm_ip.csv)**  | config file: direct assignment of IPs to local user accounts |
| **rsdb.jar**  | application |
| **rsdb.sh**  | bash entry point to run application |
| **[server_restart.sh](../server_operation)** | stop and then start background server |
| **[server_start.sh](../server_operation)** | start background server |
| **[server_status.sh](../server_operation)** | get running state of background server |
| **[server_stop.sh](../server_operation)** | stop background server |
| **yaml.sh** | (internal helper script) |


## Package Creation

Development process is managed by [Gradle](https://gradle.org/).

Gradle task _package (in file `build.gradle`) creates package into folder `package`.

bash command:

``` bash
# create package
gradle _package
```

sources:

| package source | description |
| ------------- | ------------- |
| **add/**  | additional files that are copied to package (e.g. default config files) |
| **dsl/generated-sources/**  | generated java source files |
| **src/**  | java source files |
| **webcontent/**  | web frontend files |
| **webfiles/**  | user generated files accessible from web frontend |

Before package creation you may compile the [web-frontend](../frontend) which places compiled files into folder `webcontent`.