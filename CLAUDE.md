# CLAUDE.md

이 파일은 Claude Code가 이 저장소에서 작업할 때 참고하는 안내 문서다.

## 프로젝트 한 줄 소개

퇴원환자 지역사회 케어 플랫폼 — 병원이 퇴원 예정자에게 필요한 서비스를 제안하고, 퇴원 예정자가 Care Plan을 결정한 뒤 지역 내 서비스 제공자와 연결하여 퇴원 후 돌봄을 지속 관리하는 MSA 기반 플랫폼.

자세한 배경/목표/MVP 범위는 `docs/기획안.md` 참고.

## 문서 지도

| 문서 | 내용 | 범위 |
| --- | --- | --- |
| `docs/기획안.md` | 프로젝트 배경, 문제 정의, 타겟 사용자, 페르소나, 시나리오, MVP 범위, 성공 기준 | 프로젝트 전체 |
| `docs/코드_컨벤션.md` | 개발 환경, 패키지 구조, 네이밍, Git/PR, API/Response/ErrorCode/Logging/Security/DB 컨벤션 (설명 위주) | 프로젝트 전체 (모든 서비스 공통) |
| `docs/코드_컨벤션_구현용.md` | 위 컨벤션을 실제 코드/템플릿 위주로 압축한 요약본 (200줄 이내, 구현 시 참고용) | 프로젝트 전체 (모든 서비스 공통) |
| `schedule-service/schedule-service.md` | Schedule-Service 도메인 모델, 비즈니스 규칙, 이벤트 흐름, API 목록 | Schedule-Service 전용 |
| `schedule-service/api/` | Schedule-Service API 13개 엔드포인트/이벤트 개별 상세 스펙 (`01_`~`13_` 순번) | Schedule-Service 전용 |

각 서비스는 `{service-name}/{service-name}.md` + `{service-name}/api/` 구조로 문서화한다 (Schedule-Service 참고). 향후 다른 서비스 문서를 추가할 때도 이 구조를 따른다.

## ⚠️ 현재 문서화 범위

이 저장소는 현재 **Schedule-Service만 상세 문서화되어 있다.** 아래 서비스들은 아키텍처 다이어그램과 `Table 명세서`(DB 스키마) 수준에서만 파악되어 있으며, 기능/API 상세 문서는 아직 작성되지 않았다. 해당 서비스 작업 시에는 먼저 관련 문서를 요청하거나 확인해야 한다.

- `user-service` (Port 19000)
- `discharge-service` (Port 19001)
- `social-worker-service` (Port 19002)
- `provider-service` (Port 19003)
- `care-plan-service` (Port 19005)

## 서비스 구성 (MSA)

인프라: `eureka-server`(8761), `config-server`(8888), `api-gateway`(8080)

| 서비스 | Port | 담당 도메인 |
| --- | --- | --- |
| user-service | 19000 | 사용자, 인증, 지역, 동의 |
| discharge-service | 19001 | 퇴원 정보 |
| social-worker-service | 19002 | 사회복지사 매칭 |
| provider-service | 19003 | 서비스 제공자, 제공 서비스, 매칭 |
| **schedule-service** | 19004 | **서비스 일정, 서비스 수행 결과** (`schedule-service/` 상세 문서화 대상) |
| care-plan-service | 19005 | Care Plan, 서비스 희망 일정 |

메시징: **RabbitMQ** (기획안 문서상의 "Kafka 후보" 기재는 배경 논의 당시 기록이며, 실제 구현 기준은 RabbitMQ로 확정됨 — `docs/코드_컨벤션.md` 참고)

## 작업 시 참고 원칙

- 새 API를 추가/수정할 때는 `docs/코드_컨벤션_구현용.md`의 코드 템플릿(Response/ErrorCode/Entity 등)을 우선 참고하고, 배경 설명이 필요하면 `docs/코드_컨벤션.md`를 참고한다.
- Schedule-Service 관련 작업은 `schedule-service/schedule-service.md`의 "8. 알려진 미확정/논의 사항"을 먼저 확인한다 — 이벤트 페이로드, Exchange/Queue 이름 등은 아직 확정되지 않았다.
- `schedule-service/api/` 문서를 코드로 옮길 때, 문서 내 "⚠️ 확인 필요" 또는 "✅ 확정" 표시를 반드시 확인한다. 미확정 항목은 임의로 구현하지 말고 팀 확인 후 진행한다.
- 각 서비스는 DDD + Layered Architecture 구조를 따른다 (자세한 패키지 구조는 `docs/코드_컨벤션.md` 2장 참고).