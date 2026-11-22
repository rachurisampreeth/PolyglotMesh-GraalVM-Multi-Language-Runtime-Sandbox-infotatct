@echo off
setlocal
cd /d "%~dp0..\backend"
echo =============================================
echo  PolyglotMesh - Java backend (localhost:7071)
echo =============================================
where java >nul 2>nul || (echo Java not found in PATH. Install JDK 21+. & pause & exit /b 1)
where mvn >nul 2>nul || (echo Maven not found in PATH. Install Maven 3.9+. & pause & exit /b 1)
mvn spring-boot:run
pause
