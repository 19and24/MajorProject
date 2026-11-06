@echo off
cd /d "%~dp0"
"%~dp0venv\Scripts\uvicorn.exe" app.main:app --host 0.0.0.0 --port 8080 >> "%~dp0uvicorn.log" 2>&1