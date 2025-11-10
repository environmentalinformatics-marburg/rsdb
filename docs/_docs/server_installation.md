---
title: Server Installation
---

RSDB server can be run on **Windows** or on **Ubuntu**. Ubuntu is preferred for multi-user usage over network. Windows may be more easy to use for local single user usage. (Other Linux distributions may work too.)

Quick start with download of [RSDB example distribution](https://github.com/environmentalinformatics-marburg/rsdb-data).

Or download [release package](https://github.com/environmentalinformatics-marburg/rsdb/releases).

## Windows 10 64bit

Needed dependencies are **Java** and **GDAL** which are included in the **RSDB example distribution** but not in **release packages**.

**Java**: Either Java system installation [(Java 21 or newer, JRE or JDK)](https://adoptium.net) needs to be present or [Java files](https://adoptium.net) need to be located at subfolder of this package at folder `java`. *(For RSDB example distribution the java-folder is included already.)*

**GDAL** with Java bindings: [GDAL 3.4.0 or newer](https://www.gisinternals.com/release.php) needs to be located at subfolder `gdal`. *(For RSDB example distribution the gdal-folder is included already.)*

Extract zip-file to a short path without spaces, e.g. `C:/rsdb`

Files of patten `win_*.cmd` are executable for Windows.

Doubleclick `win_server.cmd` to run RSDB server. Wait a few seconds until *"Server running.."* is printed on the terminal. To stop close the console window or press ctrl-c. 

**Web-interface**: Per default your local RSDB server is running at [http://127.0.0.1:8081](http://127.0.0.1:8081)

Consult the [RSDB wiki](https://github.com/environmentalinformatics-marburg/rsdb/wiki/Troubleshooting) if you encounter issues.


## Ubuntu

Execute following commands on terminal (bash).
~~~ bash
# (optional) show Ubuntu Version
lsb_release -a

# update package sources
sudo apt update

# install Java 21 or newer
#sudo apt install default-jdk
sudo apt-get install openjdk-21-jdk

# check if Java is installed and show Java version 21 or newer
java -version

# install library connection GDAL to Java
# If libgdal-java is not available for your os,
# you need to insert libgdal-java manually,
# see wiki below.
sudo apt install libgdal-java

# copy zip-archiv to a new folder

# go to new folder with zip-archiv

# extract zip-archiv
unzip package.zip

# mark sh-files as executable
# Files of patten `*.sh` are executable for Ubuntu.
chmod +x *.sh

# Start server in direct mode.
# To stop server and go back to commandline: press keys "ctrl-c".
./rsdb.sh server
~~~

**Web-interface**: Per default your local RSDB server is running at [http://127.0.0.1:8081](http://127.0.0.1:8081)

Consult the [RSDB wiki](https://github.com/environmentalinformatics-marburg/rsdb/wiki/Troubleshooting) if you encounter issues.

If **libgdal-java** is not available for your os, see in wiki: [Unable to locate package libgdal-java](https://github.com/environmentalinformatics-marburg/rsdb/wiki/Troubleshooting#unable-to-locate-package-libgdal-java)

## Other Linux distributions

You may translate instructions of RSDB installation to other Linux distribution commands.

Additionally you may need to modify `rsdb.sh` to point to correct paths of other Linux distributions.

Currently `rsdb.sh` is prepared  for **Ubuntu** and for **Fedora**.
