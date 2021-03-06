#!/bin/bash
# 1st param is the path to genprog for java
# 2nd param is the path to the junit jars on your machine
# claire note to self: junit jars on her machine are: /Applications/eclipse/external-jars/
#Example of how to run it:
#./prepareSimpleExample.sh /home/mau/Research/genprog4java

# Does the compile script build the test files?

PATHTOGENPROG="$1"
JUNITJARS="$2"

PATHTOZUNEBUG=`pwd`

if [[ ! -d bin/ ]] ; then
    mkdir bin
fi

javac -d bin/ src/packageZuneBug/ZuneBug.java 
javac -classpath $JUNITJARS/junit-4.12.jar:$JUNITJARS/hamcrest-core-1.3.jar:bin/ -sourcepath src/tests/*java -d bin/ src/tests/*java
rm -rf bin/packageZuneBug/

PACKAGEDIR=${JAVADIR//"/"/"."}

#Create config file 
FILE=./zuneBug.config
/bin/cat <<EOM >$FILE
javaVM = /usr/bin/java
popsize = 20
seed = 0
classTestFolder = bin/
workingDir = $PATHTOZUNEBUG/
outputDir = $PATHTOZUNEBUG/tmp/
libs = $PATHTOGENPROG/lib/junit-4.10.jar:$PATHTOGENPROG/lib/junittestrunner.jar
sanity = yes
regenPaths
sourceDir = src/
positiveTests = $PATHTOZUNEBUG/pos.tests
negativeTests = $PATHTOZUNEBUG/neg.tests
jacocoPath = $PATHTOGENPROG/lib/jacocoagent.jar
classSourceFolder = bin/
targetClassName = packageZuneBug.ZuneBug
EOM
