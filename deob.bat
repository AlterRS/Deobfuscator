@echo off
rem -agentpath:C:\PROGRA~1\JPROFI~1\bin\WINDOW~1\jprofilerti.dll=port=8849
"C:\Program Files\Java\jdk1.7.0_02\bin\java.exe" -Xmx4g -Xmn512m -XX:+UseParallelGC -cp bin alterrs.deob.Deobfuscator input.jar output.jar
pause