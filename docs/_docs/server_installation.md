---
title: Server Installation
---

Download package: [https://github.com/environmentalinformatics-marburg/rsdb/releases](https://github.com/environmentalinformatics-marburg/rsdb/releases)

Ubuntu: Execute following commands on terminal (bash).

~~~ bash
# show Ubuntu Version
lsb_release -a

# update package sources
sudo apt update

# install Java
sudo apt-get install default-jdk

# check if Java is installed and show Java version
java version

# install library connection GDAL to Java
sudo apt install libgdal-java

# copy zip-archiv to a new folder

# go to new folder with zip-archiv

# extract zip-archiv
unzip package.zip

# mark sh-files as executable
chmod +x *.sh

# Start server in direct mode (for testing).
# Stop server and go back to commandline by pressing "ctrl c".
./rsdb.sh server
~~~