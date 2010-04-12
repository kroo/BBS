@echo off
set CLASSPATH=%CLASSPATH%;./dist/jboost.jar;./lib/jfreechart-1.0.10.jar;./lib/jcommon-1.0.8.jar
java -Xmx100M jboost.controller.Controller %*
