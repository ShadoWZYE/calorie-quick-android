@echo off
setlocal
title Calorie Quick Test Launcher
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\launch-test.ps1"
if errorlevel 1 (
  echo.
  echo Launcher failed. Review the error above.
  pause
  exit /b 1
)
endlocal

