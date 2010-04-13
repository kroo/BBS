#!/bin/sh
export JBOOST_DIR="`pwd`/jboost-2.1" 
export CLASSPATH="$CLASSPATH:$JBOOST_DIR/dist/jboost.jar:$JBOOST_DIR/lib/jfreechart-1.0.10.jar:$JBOOST_DIR/lib/jcommon-1.0.8.jar"

java -Xmx500M jboost.controller.Controller -S asphaltClassifier/asphalt