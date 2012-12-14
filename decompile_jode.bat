@echo off
rem Jode decompile
java -cp bin;deps/clientlibs.jar;output.jar jode.decompiler.Main --dest output_src output.jar
pause