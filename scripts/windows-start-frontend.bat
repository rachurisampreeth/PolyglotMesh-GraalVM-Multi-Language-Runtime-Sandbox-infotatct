@echo off
setlocal
cd /d "%~dp0..\frontend"
echo ===========================================
echo  PolyglotMesh - React IDE (localhost:7070)
echo ===========================================
where npm >nul 2>nul || (echo npm not found in PATH. Install Node.js. & pause & exit /b 1)
if not exist node_modules (echo Installing packages... & call npm install || (pause & exit /b 1))
call npm run dev
pause
