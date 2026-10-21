@echo off
chcp 65001 >nul
setlocal
set "JAVA_CMD=java"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
"%JAVA_CMD%" -Dfile.encoding=UTF-8 --source 17 "%~dp0SecretKeyTool.java"
if errorlevel 1 (
  echo.
  echo Tool failed. Please check the error above. Java 17 or newer is required.
  pause
)
endlocal
