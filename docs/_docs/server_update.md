---
title: Server Update (Ubuntu only)
---

New Versions of RSDB are available as releases.

## automatic update

In RSDB releases included is the script file `github_update.sh`. 

When you run it, the latest package is downloaded, folders/files are replaced: `lib`, `webcontent`, `rsdb.jar` and backup of old files/folders is placed in folder `backup`. Other folders/files are not changed. The remaining files of the downloaded release are left in folder `update` for the case that you want to replace some further files/folders.

~~~ bash
# First stop a running RSDB server and then execute following update script.
./github_update.sh
~~~

## manual update

1. stop a running RSDB server

2. **download** package: [https://github.com/environmentalinformatics-marburg/rsdb/releases](https://github.com/environmentalinformatics-marburg/rsdb/releases)

3. **extract** package into a temporary folder: `unzip package.zip`

4. **replace** following **folders** in your RSDB folder with that of your extracted package: `lib`, `webcontent`

5. **replace** following **files** in your RSDB folder with that of your extracted package: `rsdb.jar`

6. start RSDB server