#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

echo "============================================="
echo "   SmartSilk Business Server Engine Starter  "
echo "============================================="

if ! command -v docker &> /dev/null; then
    echo "Error: Docker is not installed or not in PATH."
    echo "Please install Docker from https://docs.docker.com/get-docker/"
    exit 1
fi

if [ ! -f .env ]; then
    echo "Creating secure .env automatically..."
    cp .env.example .env
    RANDOM_JWT=$(LC_ALL=C tr -dc 'A-Za-z0-9' </dev/urandom | head -c 48 || true)
    RANDOM_DB=$(LC_ALL=C tr -dc 'A-Za-z0-9' </dev/urandom | head -c 32 || true)
    if [ -n "$RANDOM_JWT" ]; then
        sed -i "s/JWT_SECRET=.*/JWT_SECRET=$RANDOM_JWT/" .env
    fi
    if [ -n "$RANDOM_DB" ]; then
        sed -i "s/DB_PASSWORD=.*/DB_PASSWORD=$RANDOM_DB/" .env
    fi
    chmod 600 .env 2>/dev/null || true
    echo "Secure environment initialized (JWT secret & DB password generated)."
fi

if ! docker image inspect ghcr.io/nithiishsd/nexora-backend:latest >/dev/null 2>&1; then
    if docker image inspect nexora:local >/dev/null 2>&1; then
        echo "Found local image nexora:local. Tagging as ghcr.io/nithiishsd/nexora-backend:latest..."
        docker tag nexora:local ghcr.io/nithiishsd/nexora-backend:latest
    fi
fi

echo "Starting SmartSilk server engine containers..."
docker compose up -d

PORT=$(grep -E '^SERVER_PORT=' .env | cut -d= -f2 || echo "8088")
PORT=${PORT:-8088}

echo "Waiting for SmartSilk server engine to become healthy..."
for i in {1..30}; do
    if curl -s "http://localhost:${PORT}/actuator/health" | grep -q '"status":"UP"'; then
        echo ""
        echo "========================================================="
        echo "   SUCCESS! SmartSilk Server is UP & READY!              "
        echo "========================================================="
        echo " Local Access:   http://localhost:${PORT}"
        HOST_IP=$(hostname -I 2>/dev/null | awk '{print $1}' || echo "your-server-ip")
        echo " Network Access: http://${HOST_IP}:${PORT}"
        echo ""
        echo " In the SmartSilk Desktop App: Select 'Remote / Cloud Server'"
        echo " and enter the Network Access URL above to connect."
        echo "========================================================="
        exit 0
    fi
    echo -n "."
    sleep 2
done

echo ""
echo "Server started. Check status with: docker compose logs"
