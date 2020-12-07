#!/bin/bash

# GDAL java libray path
# Ubuntu:
# /usr/share/java/gdal.jar
# Fedora:
# /usr/lib/java/gdal/gdal.jar

# GDAL java binary library path
# Ubuntu:
# /usr/lib/jni
# Fedora:
# /usr/lib/java/gdal

exec java -Djavax.xml.parsers.DocumentBuilderFactory=com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl -Djava.awt.headless=true -XX:-UsePerfData -Djava.io.tmpdir=/var/tmp -Xmx2g -classpath 'rsdb.jar:/usr/share/java/gdal.jar:/usr/lib/java/gdal/gdal.jar:lib/*' -Djava.library.path='/usr/lib/jni:/usr/lib/java/gdal' run.Terminal "$@"