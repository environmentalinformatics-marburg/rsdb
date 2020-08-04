@echo off
echo.

set RSDB_PATH=%cd%

REM Set GDAL_PATH to absolute path of gdal.
REM If you set this path check if following file exists at location: GDAL_PATH/bin/gdal/java/gdal.jar
set GDAL_PATH=%RSDB_PATH%/gdal

set GDAL_BIN_PATH=%GDAL_PATH%/bin

REM Set GDAL data location, e.g. for EPSG support file 'gcs.csv'.
REM This variable is read inernally from GDAL library.
REM https://trac.osgeo.org/gdal/wiki/FAQInstallationAndBuilding#WhatisGDAL_DATAenvironmentvariable
set GDAL_DATA=%GDAL_BIN_PATH%/gdal-data


set GDAL_JAVA_PATH=%GDAL_BIN_PATH%/gdal/java
set GDAL_JAR_PATH=%GDAL_JAVA_PATH%/gdal.jar
REM echo %GDAL_JAR_PATH%

IF NOT EXIST %GDAL_JAR_PATH% (
	echo ERROR: GDAL missing or wrong path:	
	echo %GDAL_PATH%
	echo Check documentation on how to include GDAL.
	echo.
	pause
	exit 1
)

REM Set JAVA_PATH to absolute path of java jre or jdk 8 or newer.
set JAVA_PATH=%RSDB_PATH%/java
set JAVA_BIN_PATH=%JAVA_PATH%/bin

set PATH=%GDAL_BIN_PATH%;%JAVA_BIN_PATH%;%PATH%

java -version

java -Djavax.xml.parsers.DocumentBuilderFactory=com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl -Djava.awt.headless=true -XX:-UsePerfData -Xmx3g -classpath "rsdb.jar;%GDAL_JAR_PATH%;lib/*" -Djava.library.path="%GDAL_JAVA_PATH%" run.Terminal %*

pause