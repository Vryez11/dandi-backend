#!/bin/bash
set -euo pipefail

# prod 내부 서비스(어드민·Metabase)로 SSM 포트 포워딩 터널을 연다.
#
# 사용법:
#   ./scripts/tunnel.sh admin      # http://localhost:18080/admin
#   ./scripts/tunnel.sh metabase   # http://localhost:13000
#
# 두 서비스는 로드 밸런서·퍼블릭 IP가 없고, 보안 그룹이 ECS 인스턴스에서 오는 요청만 받는다.
# 그래서 ECS 인스턴스 하나를 거쳐 태스크의 private IP로 포워딩한다.
# 태스크 IP는 배포할 때마다 바뀌므로 매번 조회한다. Ctrl+C로 닫는다.
#
# AWS 자격 증명은 AWS_PROFILE(기본 dandi-ryo)을 쓴다. 만료됐으면 먼저 aws sso login --profile dandi-ryo.
# 로컬 포트는 바꾸지 않는다. 어드민은 허용 출처·Google 승인 출처가 localhost:18080이다.

export AWS_PROFILE="${AWS_PROFILE:-dandi-ryo}"
export AWS_REGION="ap-northeast-2"
CLUSTER="nyummy-prod-cluster"

case "${1:-}" in
    admin)
        SERVICE="nyummy-prod-admin-service"
        REMOTE_PORT=8080
        LOCAL_PORT=18080
        OPEN_URL="http://localhost:18080/admin"
        ;;
    metabase)
        SERVICE="nyummy-prod-metabase-service"
        REMOTE_PORT=3000
        LOCAL_PORT=13000
        OPEN_URL="http://localhost:13000"
        ;;
    *)
        echo "사용법: $0 admin|metabase" >&2
        exit 1
        ;;
esac

if ! aws sts get-caller-identity >/dev/null 2>&1; then
    echo "[tunnel] AWS 자격 증명이 없거나 만료됐습니다: aws sso login --profile $AWS_PROFILE" >&2
    exit 1
fi

if lsof -iTCP:"$LOCAL_PORT" -sTCP:LISTEN >/dev/null 2>&1; then
    echo "[tunnel] 로컬 포트 $LOCAL_PORT 를 이미 쓰고 있습니다. 열려 있는 터널이나 프로그램을 먼저 닫으세요." >&2
    exit 1
fi

TASK_ARN=$(aws ecs list-tasks --cluster "$CLUSTER" --service-name "$SERVICE" --desired-status RUNNING \
    --query 'taskArns[0]' --output text)
if [[ -z "$TASK_ARN" || "$TASK_ARN" == "None" ]]; then
    echo "[tunnel] $SERVICE 에 실행 중인 태스크가 없습니다." >&2
    exit 1
fi

TASK_IP=$(aws ecs describe-tasks --cluster "$CLUSTER" --tasks "$TASK_ARN" \
    --query 'tasks[0].containers[0].networkInterfaces[0].privateIpv4Address' --output text)

# 경유지: SSM에 연결된 클러스터의 ECS 인스턴스 하나
CONTAINER_INSTANCES=$(aws ecs list-container-instances --cluster "$CLUSTER" --query 'containerInstanceArns' --output text)
INSTANCE_IDS=$(aws ecs describe-container-instances --cluster "$CLUSTER" --container-instances $CONTAINER_INSTANCES \
    --query 'containerInstances[].ec2InstanceId' --output text)
BASTION=$(aws ssm describe-instance-information \
    --filters "Key=InstanceIds,Values=$(echo "$INSTANCE_IDS" | tr '\t' ',')" "Key=PingStatus,Values=Online" \
    --query 'InstanceInformationList[0].InstanceId' --output text)
if [[ -z "$BASTION" || "$BASTION" == "None" ]]; then
    echo "[tunnel] SSM에 연결된 ECS 인스턴스가 없습니다." >&2
    exit 1
fi

echo "[tunnel] $SERVICE ($TASK_IP:$REMOTE_PORT) ← $BASTION ← localhost:$LOCAL_PORT"
echo "[tunnel] 연결되면 $OPEN_URL 을 여세요. 닫으려면 Ctrl+C"

aws ssm start-session --target "$BASTION" \
    --document-name AWS-StartPortForwardingSessionToRemoteHost \
    --parameters "{\"host\":[\"$TASK_IP\"],\"portNumber\":[\"$REMOTE_PORT\"],\"localPortNumber\":[\"$LOCAL_PORT\"]}"
