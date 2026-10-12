<div align="center">

![nyummy_image.png](./images/nyummy_image.png)

# Nyummy Backend API Server

<!-- TODO: 배지가 필요하면 여기에 추가 (예: 커버리지, 방문자 수) -->

> 이 성과는 2026년도 과학기술정보통신부의 재원으로 정보통신기획평가원의 지원을 받아 수행된 연구임. (IITP-2026-SW마에스트로과정) <br><br> This work was supported by the Institute of Information & Communications Technology Planning & Evaluation(IITP) grant funded by the Ministry of Science and ICT(MSIT) (IITP-2026-SW Maestro training course).

</div>

## 목차

- [1. 프로젝트 개요](#1-프로젝트-개요)
  - [1-1. 프로젝트 소개](#1-1-프로젝트-소개)
  - [1-2. 시스템 구성도](#1-2-시스템-구성도)
  - [1-3. 주요 기능](#1-3-주요-기능)
  - [1-4. 개발 환경](#1-4-개발-환경)
- [2. 개발 결과물](#2-개발-결과물)
  - [2-1. 백엔드 아키텍처](#2-1-백엔드-아키텍처)
  - [2-2. AWS 아키텍처](#2-2-aws-아키텍처)
  - [2-3. 데이터베이스 ERD](#2-3-데이터베이스-erd)
  - [2-4. API 문서화 및 테스트](#2-4-api-문서화-및-테스트)
  - [2-5. 단위 테스트 및 통합 테스트](#2-5-단위-테스트-및-통합-테스트)
  - [2-6. CI/CD 파이프라인](#2-6-cicd-파이프라인)
  - [2-7. AI 기반 비동기 영양 분석](#2-7-ai-기반-비동기-영양-분석)
  - [2-8. S3 Presigned URL 기반 이미지 업로드](#2-8-s3-presigned-url-기반-이미지-업로드)
  - [2-9. 인증 시스템](#2-9-인증-시스템)
  - [2-10. 관측(Observability)](#2-10-관측observability)
- [3. 수행 방법 및 프로젝트 관리](#3-수행-방법-및-프로젝트-관리)
  - [3-1. 개발 프로세스](#3-1-개발-프로세스)
  - [3-2. 형상 관리 프로세스](#3-2-형상-관리-프로세스)
  - [3-3. 네이밍 컨벤션](#3-3-네이밍-컨벤션)

## 1. 프로젝트 개요

### 1-1. 프로젝트 소개

식사 사진을 찍으면 AI가 영양 정보를 분석하고, 그 결과에 따라 고양이 캐릭터가 반응하고 성장하는 식습관 관리 게임 앱입니다.

<!-- TODO: 프로젝트 배경·문제 의식, 팀 소개 -->

### 1-2. 시스템 구성도

![system_architecture.png](./images/system_architecture.png)

### 1-3. 주요 기능

![feature_list.png](./images/feature_list.png)

- **식사 기록 및 영양 분석**: 식사 사진을 업로드하면 AI가 칼로리·탄수화물·단백질·지방을 추출
- **고양이 캐릭터**: 식사 기록에 따라 애정도·경험치가 오르고, 일정 기간의 식습관에 따라 체형이 변화
- **캘린더 · 하루 평가**: 월간 캘린더로 일별 식사 기록과 영양 평가 확인
- **코인 · 상점**: 식사 기록으로 코인을 얻고 상점에서 사용
- **알림**: 분석 완료·식사 리마인더 푸시 알림 (FCM)
- **회원**: 이메일 회원가입·비밀번호 찾기, 카카오 로그인

<!-- TODO: 기능 설명 다듬기 / 스크린샷 추가 -->

### 1-4. 개발 환경

- Back-end: `Kotlin`, `Spring Boot 4`, `Spring Security`, `JPA`, `Flyway`, `MySQL`, `Redis`, `Caffeine`
- Infra: `AWS (EC2, ECS, ECR, S3, SES, Secrets Manager)`, `Docker`, `Nginx`, `Jenkins`
- Observability: `OpenTelemetry`, `Grafana Cloud`
- External: `Google Gemini API`, `Firebase Cloud Messaging`, `Kakao OIDC`
- Test: `JUnit 5`, `MockK`, `Testcontainers`

## 2. 개발 결과물

### 2-1. 백엔드 아키텍처

![backend_architecture.png](./images/backend_architecture.png)

도메인 단위로 패키지를 나누고(`user`, `auth`, `meal`, `cat`, `home`, `image`, `notification`), 외부 서비스 연동은 `infra` 패키지(`ai`, `storage`, `email`, `oauth`, `push`)에 인터페이스로 분리하였습니다.

<!-- TODO: 계층 구조, 아키텍처 변천 과정 설명 -->

### 2-2. AWS 아키텍처

![aws_architecture.png](./images/aws_architecture.png)

- **dev**: EC2 + Docker Compose, Nginx에서 TLS 종료(Let's Encrypt)
- **prod**: ECS, 비밀값은 Secrets Manager로 주입

<!-- TODO: VPC·서브넷 구성, 아키텍처 버전별 발전 과정 -->

### 2-3. 데이터베이스 ERD

![erd.png](./images/erd.png)

스키마 변경은 Flyway 마이그레이션(`src/main/resources/db/migration`)으로 버전 관리합니다.

<!-- TODO: 테이블 설계 설명 -->

### 2-4. API 문서화 및 테스트

**Swagger**

![swagger_documentation.png](./images/swagger_documentation.png)

springdoc-openapi로 OpenAPI 3 스펙 기반 API 문서를 제공합니다.

<!-- TODO: .http 파일 기반 API 테스트 등 -->

### 2-5. 단위 테스트 및 통합 테스트

![test_result.png](./images/test_result.png)

MockK 기반 서비스 단위 테스트와 Testcontainers(MySQL) 기반 통합 테스트를 작성하였습니다.

<!-- TODO: 테스트 범위·커버리지 수치 -->

### 2-6. CI/CD 파이프라인

![ci_cd_workflow.png](./images/ci_cd_workflow.png)

Jenkins로 테스트·이미지 빌드·ECR 푸시·dev/prod 배포를 자동화하였습니다.

<!-- TODO: 파이프라인 단계, GitHub Actions → Jenkins 이관 배경 -->

### 2-7. AI 기반 비동기 영양 분석

![nutrition_analysis_flow.png](./images/nutrition_analysis_flow.png)

식사 이미지를 Google Gemini API로 분석해 칼로리·탄단지를 추출합니다. 분석은 비동기로 처리하며 식사 상태(`WAITING` → `ANALYZING` → `COMPLETED` / `FAILED`)로 진행 상황을 관리하고, 실패 시 재시도할 수 있습니다.

<!-- TODO: 비동기 방식 선택 이유, 응답 속도 개선 과정·수치 -->

### 2-8. S3 Presigned URL 기반 이미지 업로드

![presigned_url_flow.png](./images/presigned_url_flow.png)

API 서버가 이미지 바이너리를 직접 받지 않도록 S3 Presigned URL로 클라이언트가 직접 업로드하고, 서버는 업로드 확정 단계에서 파일 시그니처(Apache Tika)와 메타데이터를 검증합니다.

<!-- TODO: 임시 경로 → 확정 흐름 설명 -->

### 2-9. 인증 시스템

![auth_flow.png](./images/auth_flow.png)

- JWT 기반 AccessToken / RefreshToken 인증
- 이메일 인증·비밀번호 재설정 토큰은 JWE로 암호화
- 카카오 로그인: 앱에서 받은 ID 토큰을 서버에서 OIDC(JWKS)로 검증

<!-- TODO: 토큰 수명·재발급 흐름 등 -->

### 2-10. 관측(Observability)

![grafana_dashboard.png](./images/grafana_dashboard.png)

OpenTelemetry로 메트릭·트레이스·로그를 OTLP로 내보내 Grafana Cloud에서 모니터링합니다.

<!-- TODO: 대시보드 구성, 관측으로 발견한 문제 -->

## 3. 수행 방법 및 프로젝트 관리

### 3-1. 개발 프로세스

![dev_process.png](./images/dev_process.png)

<!-- TODO: 스프린트 방식, 이슈 관리 도구, 회고 -->

### 3-2. 형상 관리 프로세스

![git_flow.png](./images/git_flow.png)

<!-- TODO: 브랜치 전략, PR 리뷰 규칙, 커밋 컨벤션 -->

### 3-3. 네이밍 컨벤션

같은 개념에 여러 영어 단어가 섞이지 않도록 동사·클래스 접미사·도메인 용어를 하나로 고정한 용어 사전을 운영합니다. ([CLAUDE.md](./CLAUDE.md))

<!-- TODO: 용어 사전 도입 배경 -->
