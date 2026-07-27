#!/bin/bash
set -euo pipefail

cd ~/nyummy
set -a
source .env
set +a

aws ecr get-login-password --region "$AWS_REGION" | docker login --username AWS --password-stdin "$REGISTRY"
docker compose pull
docker compose up -d
docker system prune -f
