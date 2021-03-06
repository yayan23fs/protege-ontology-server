#!/bin/sh

cd `dirname $0`

java -Xmx3500M -Xms250M \
     -server \
     -Djava.rmi.server.hostname=`hostname` \
     -DentityExpansionLimit=100000000 \
     -Dfile.encoding=UTF-8 \
     -Dorg.protege.owl.server.configuration=metaproject.owl \
     -agentlib:jdwp=transport=dt_socket,address=8100,server=y,suspend=y \
     -Djava.util.logging.config.file=logging.properties \
     -classpath ./lib/felix.jar:./lib/ProtegeLauncher.jar \
     org.protege.osgi.framework.Launcher
