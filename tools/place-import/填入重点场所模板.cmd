@echo off
setlocal
chcp 65001 >nul
set "PYTHONIOENCODING=utf-8"
set "PLACE_RUNTIME=%USERPROFILE%\.cache\codex-runtimes\codex-primary-runtime\dependencies"
if defined FIRE_SPREADSHEET_RUNTIME set "PLACE_RUNTIME=%FIRE_SPREADSHEET_RUNTIME%"
"%PLACE_RUNTIME%\python\python.exe" "%~dp0fill_standard_template.py" %*
if errorlevel 1 (
  echo Conversion failed. Please review the message above.
  pause
  exit /b 1
)
echo Done.
pause
