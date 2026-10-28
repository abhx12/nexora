@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"

echo =============================================
echo    SmartSilk Business Server Engine Starter  
echo =============================================

docker --version >nul 2>&1
if errorlevel 1 (
    echo Error: Docker is not installed or not in PATH.
    echo Please install Docker Desktop from https://www.docker.com/products/docker-desktop/
    pause
    exit /b 1
)

if not exist .env (
    echo Creating secure .env automatically...
    copy .env.example .env >nul
    powershell -Command "$jwt = -join ((65..90) + (97..122) + (48..57) | Get-Random -Count 48 | ForEach-Object {[char]$_}); $db = -join ((65..90) + (97..122) + (48..57) | Get-Random -Count 32 | ForEach-Object {[char]$_}); (Get-Content .env) -replace 'JWT_SECRET=.*', ('JWT_SECRET=' + $jwt) -replace 'DB_PASSWORD=.*', ('DB_PASSWORD=' + $db) | Set-Content .env" >nul 2>&1
    echo Secure environment initialized (JWT secret and DB password generated).
)

docker image inspect ghcr.io/nithiishsd/nexora-backend:latest >nul 2>&1
if errorlevel 1 (
    docker image inspect nexora:local >nul 2>&1
    if not errorlevel 1 (
        echo Found local image nexora:local. Tagging as ghcr.io/nithiishsd/nexora-backend:latest...
        docker tag nexora:local ghcr.io/nithiishsd/nexora-backend:latest
    )
)

echo Starting SmartSilk server engine containers...
docker compose up -d

echo =========================================================
echo    SUCCESS! SmartSilk Server started in the background.
echo =========================================================
echo  Local Access: http://localhost:8088
echo.
echo  In the SmartSilk Desktop App:
echo  Select 'Remote / Cloud Server' and enter your server address.
echo =========================================================
pause
