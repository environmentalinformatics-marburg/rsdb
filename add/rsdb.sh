#!/bin/bash

exec java -Djavax.xml.parsers.DocumentBuilderFactory=com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl -Djava.awt.headless=true -XX:-UsePerfData -Djava.io.tmpdir=/var/tmp -Xmx2g -classpath 'rsdb.jar:/usr/share/java/gdal.jar:lib/*' -Djava.library.path='/usr/lib/jni' run.Terminal "$@"