@echo off
cd /d "%~dp0"
start "PolyglotMesh Backend" cmd /k call "%~dp0windows-start-backend.bat"
start "PolyglotMesh Frontend" cmd /k call "%~dp0windows-start-frontend.bat"
echo Started two terminals. Wait for API startup, then open http://localhost:7070
pause
