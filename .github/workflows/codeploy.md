### Codeploy 배포 흐름
1. ALB → TG 구조에서 기존 TG(blue)로 트래픽이 전달되는 중
2. 애플리케이션 서버 머지에 따른 CI/CD 실행
3. Codeploy기반 CD 진행
4. blue ASG를 복제하여 green ASG 생성
5. green ASG를 green TG에 매핑
6. green ASG기반으로 생성된 신규 버젼 인스턴스 헬스체크
7. 헬스체크 성공 시 green TG로 alb 트래픽 변경
8. 적정 시간 이후에 기존(blue) asg 삭제 or 중지?

---
### codeploy 작동 흐름?

1. 배포그룹에 Blue TG / Green TG (target group pair) 를 연결해 둔다
2. 현재 트래픽을 받는 쪽(예: Blue TG)에 붙어 있는 원본 ASG를 “템플릿”으로
3. CodeDeploy가 ASG를 복사해서 replacement(= Green 환경) 인스턴스들을 새로 띄우고
4. 그 replacement 인스턴스들을 트래픽을 받지 않는 TG(예: Green TG)에 등록한 뒤
5. 헬스 OK면 리스너를 전환한다

작업을 진행하면서 일종의 태그들은 동적으로 변경해야 할까?

---

## 설계 검토 및 보완

| 항목 | 현재 설계 | 권장 방향 |
|------|----------|----------|
| ASG 복제 방식 | CodeDeploy가 ASG 복제 | ✅ 맞음 (COPY_AUTO_SCALING_GROUP 옵션) |
| TG 전환 | Blue/Green TG pair | ✅ 맞음 |
| 기존 ASG 처리 | "삭제 or 중지?" | **TERMINATE** 권장 (비용 절감) |
| 태그 관리 | 동적 변경 필요? | **불필요** - CodeDeploy가 자동 관리 |

---

## 구현 범위

### 1. AWS 콘솔/CLI에서 설정 필요 (코드 외)
- CodeDeploy Application 생성
- CodeDeploy Deployment Group (Blue/Green 타입)
- 두 번째 Target Group (Green용) 생성
- EC2 인스턴스에 CodeDeploy Agent 설치
- IAM Role 설정 (EC2용, CodeDeploy용)

### 2. 코드베이스에 추가할 파일

```
프로젝트 루트/
├── appspec.yml                    # CodeDeploy 배포 명세
├── scripts/
│   ├── before_install.sh          # 기존 컨테이너 정리
│   ├── after_install.sh           # ECR 로그인 & 이미지 pull
│   ├── application_start.sh       # docker compose up
│   └── validate_service.sh        # 헬스체크
└── .github/workflows/
    └── ci-cd-prod.yml             # main 브랜치 전용 (CodeDeploy 트리거)
```

### 3. 배포 흐름 (Docker Compose 방식)

```
GitHub Push (main)
    ↓
CI (테스트/빌드) → ECR에 이미지 Push
    ↓
CodeDeploy 배포 트리거
    ↓
Green ASG 인스턴스 생성
    ↓
CodeDeploy Agent가 appspec.yml 실행
  - before_install: 기존 컨테이너 정리
  - after_install: ECR 로그인 & docker compose pull
  - application_start: docker compose up -d
  - validate_service: curl 헬스체크
    ↓
ALB 트래픽 Green TG로 전환
    ↓
Blue ASG 인스턴스 종료
```

---

## AWS 리소스 생성 가이드

### 현재 상태
- [x] ALB 생성됨
- [x] ASG 생성됨 (Blue)
- [x] Target Group 생성됨 (Blue)

### 생성해야 할 리소스

---

### 1. Target Group (Green) 생성

**위치**: EC2 > Target Groups > Create target group

| 설정 | 값                                        |
|------|------------------------------------------|
| Target type | Instances                                |
| Name | `damo-prod-be-tg-green` (기존 Blue와 동일 설정) |
| Protocol / Port | HTTP / 8080                              |
| VPC | 기존과 동일                                   |
| Health check path | `/api/health`                            |

> 기존 Blue TG와 동일한 설정으로 생성. ASG에 직접 연결하지 않음 (CodeDeploy가 관리)

---

### 2. IAM Role 생성

#### 2-1. CodeDeploy Service Role

**위치**: IAM > Roles > Create role

| 설정 | 값 |
|------|-----|
| Trusted entity | AWS Service |
| Use case | CodeDeploy |
| Role name | `CodeDeployServiceRole` |
| Policy | `AWSCodeDeployRole` (AWS 관리형) |

#### 2-2. EC2 Instance Role (기존 Role에 정책 추가)

기존 EC2 인스턴스에 연결된 IAM Role에 다음 정책 추가:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:GetObjectVersion",
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::damo-deploy-s3",
        "arn:aws:s3:::damo-deploy-s3/codeploy/prod/be/*"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "ecr:GetAuthorizationToken",
        "ecr:BatchCheckLayerAvailability",
        "ecr:GetDownloadUrlForLayer",
        "ecr:BatchGetImage"
      ],
      "Resource": "*"
    }
  ]
}
```

---

### 3. EC2에 CodeDeploy Agent 설치

**각 EC2 인스턴스에서 실행** (또는 Launch Template의 User Data에 추가):

```bash
#!/bin/bash
sudo yum update -y
sudo yum install -y ruby wget

cd /home/ec2-user
wget https://aws-codedeploy-ap-northeast-2.s3.ap-northeast-2.amazonaws.com/latest/install
chmod +x ./install
sudo ./install auto

# 설치 확인
sudo service codedeploy-agent status
```

**Launch Template User Data에 추가 권장** (ASG 스케일 아웃 시 자동 설치):

```bash
#!/bin/bash
# CodeDeploy Agent 설치
yum install -y ruby wget
cd /home/ec2-user
wget https://aws-codedeploy-ap-northeast-2.s3.ap-northeast-2.amazonaws.com/latest/install
chmod +x ./install
./install auto
service codedeploy-agent start
```
우분투용 USER DATA
```bash
#!/bin/bash                                                                                                                                                                        
apt-get update                                                                                                                                                                     
apt-get install -y ruby wget                                                                                                                                                       
                                                                                                                                                                                     
cd /home/ubuntu                                                                                                                                                                    
wget https://aws-codedeploy-ap-northeast-2.s3.ap-northeast-2.amazonaws.com/latest/install                                                                                          
chmod +x ./install                                                                                                                                                                 
./install auto    
```

---

### 4. CodeDeploy Application 생성

**위치**: CodeDeploy > Applications > Create application

| 설정 | 값 |
|------|-----|
| Application name | `damo-backend` |
| Compute platform | EC2/On-premises |

---

### 5. CodeDeploy Deployment Group 생성 (Blue/Green)

**위치**: CodeDeploy > Applications > damo-backend > Create deployment group

| 설정 | 값 |
|------|-----|
| Deployment group name | `damo-backend-prod` |
| Service role | `CodeDeployServiceRole` (위에서 생성) |
| Deployment type | **Blue/green** |
| Environment configuration | **Amazon EC2 Auto Scaling groups** |
| Auto Scaling group | 기존 ASG 선택 |

#### Blue/Green 배포 설정

| 설정 | 값 |
|------|-----|
| Traffic rerouting | Reroute traffic immediately |
| Terminate original instances | Yes |
| Wait time before termination | 5 minutes (권장) |

#### Load Balancer 설정

| 설정 | 값 |
|------|-----|
| Load balancer | Application Load Balancer |
| Target group 1 (Blue) | 기존 TG 선택 |
| Target group 2 (Green) | `damo-prod-be-tg-green` (위에서 생성) |

#### 배포 설정

| 설정 | 값 |
|------|-----|
| Deployment configuration | CodeDeployDefault.AllAtOnce |
| Rollback | Roll back when deployment fails ✅ |

---

### 생성 순서 체크리스트

1. [ ] Target Group (Green) 생성
2. [ ] IAM Role - CodeDeploy Service Role 생성
3. [ ] IAM Role - EC2 Role에 S3/ECR 정책 추가
4. [ ] EC2 인스턴스에 CodeDeploy Agent 설치 (또는 Launch Template 수정)
5. [ ] CodeDeploy Application 생성 (`damo-backend`)
6. [ ] CodeDeploy Deployment Group 생성 (`damo-backend-prod`)

---

## GitHub Secrets 설정

### 기존 Secrets (재사용)

| Secret 이름 | 설명 | 비고 |
|------------|------|------|
| `AWS_ACCESS_KEY_ID` | AWS Access Key | CodeDeploy, S3, ECR 권한 필요 |
| `AWS_SECRET_ACCESS_KEY` | AWS Secret Key | |
| `MONGODB_URI` | MongoDB 연결 URI | CI 테스트용 |
| `DISCORD_WEBHOOK_URL` | Discord 알림 웹훅 | |

### 신규 Secrets (추가 필요 없음)

현재 워크플로우에서는 CodeDeploy 관련 설정값들이 환경 변수로 하드코딩되어 있습니다.
필요시 아래 값들을 Secrets로 분리할 수 있습니다:

```yaml
# ci-cd-codeploy.yml의 env 섹션 (현재 하드코딩)
CODEDEPLOY_APP: damo-backend
CODEDEPLOY_GROUP: damo-backend-prod
S3_BUCKET: damo-deploy-s3
S3_KEY_PREFIX: codeploy/prod/be
ECR_REPO: 080598576517.dkr.ecr.ap-northeast-2.amazonaws.com/prod/be
```

### IAM 권한 요구사항

`AWS_ACCESS_KEY_ID`에 연결된 IAM User/Role에 다음 권한 필요:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "codedeploy:CreateDeployment",
        "codedeploy:GetDeployment",
        "codedeploy:GetDeploymentConfig",
        "codedeploy:RegisterApplicationRevision"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject"
      ],
      "Resource": "arn:aws:s3:::damo-deploy-s3/codeploy/prod/be/*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "ecr:GetAuthorizationToken",
        "ecr:BatchCheckLayerAvailability",
        "ecr:GetDownloadUrlForLayer",
        "ecr:BatchGetImage",
        "ecr:PutImage",
        "ecr:InitiateLayerUpload",
        "ecr:UploadLayerPart",
        "ecr:CompleteLayerUpload"
      ],
      "Resource": "*"
    }
  ]
}
```