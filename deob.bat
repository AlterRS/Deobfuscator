@echo off
"C:\Program Files\Java\jdk1.7.0_02\bin\java.exe" -Xmx2g -Xms512m -Xmn256m -XX:+UseParallelGC -cp bin alterrs.deob.Deobfuscator input.jar output.jar
pause