@echo off
java -Xmx4g -Xmn512m -XX:+UseParallelGC -cp bin alterrs.deob.Deobfuscator input.jar output.jar
pause