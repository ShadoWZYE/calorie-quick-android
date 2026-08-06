@echo off
setlocal
where pyw >nul 2>nul
if %errorlevel%==0 (
  start "" pyw -3 "%~dp0review_inbox.py"
  exit /b 0
)
where pythonw >nul 2>nul
if %errorlevel%==0 (
  start "" pythonw "%~dp0review_inbox.py"
  exit /b 0
)
where py >nul 2>nul
if %errorlevel%==0 (
  py -3 "%~dp0review_inbox.py"
  exit /b %errorlevel%
)
python "%~dp0review_inbox.py"
