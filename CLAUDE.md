# 네이밍 컨벤션 사전

코드, 메서드, 클래스 이름을 지을 때 같은 개념에 여러 영어 단어가 혼용되는 것을 막기 위한 용어 통일 표입니다.

## 사용 규칙

1. 이름에 개념을 표현할 때, 아래 표에 해당 개념이 있으면 **반드시 그 단어를 사용**한다.
2. 표에 없는 개념이면 단어를 하나 정해 사용하고, **같은 커밋/PR에서 이 표에도 추가**한다.
3. 금지(동의어) 열에 있는 단어는 사용하지 않는다.
4. Claude도 동일 규칙을 따른다: 코드를 작성하다 표에 없는 새 개념의 단어를 쓰게 되면, 이 표를 함께 갱신할 것.

## 동사 (메서드 네이밍)

| 개념 | 사용 단어 | 금지(동의어) | 예시 |
|---|---|---|---|
| 생성 | create | add, register, make, new | `createMeal()` |
| 조회(단건) | get | find, fetch, retrieve, read | `getMeal()` |
| 조회(목록) | get + 복수형 | list, findAll | `getMeals()` |
| 수정 | update | modify, edit, change | `updateCat()` |
| 삭제 | delete | remove, destroy, drop | `deleteMeal()` |
| 검색(조건) | search | query, lookup | `searchMeals()` |
| 존재 확인 | exists | has, contains | `existsUser()` |
| 계산 | calculate | compute, count | `calculateScore()` |
| 검증 | validate | check, verify | `validateEmail()` |
| 증가 | increase | add, plus | `increaseCoin()` |
| 감소 | decrease | minus, subtract | `decreaseCoin()` |
| 재시도 | retry | resume, redo | `retryNutritionAnalysis()` |

## 클래스 접미사

| 용도 | 접미사 | 금지 |
|---|---|---|
| 요청 DTO | ~Request | ~Req, ~Command |
| 응답 DTO | ~Response | ~Res, ~Dto, ~Result |
| 서비스 | ~Service | ~Manager, ~Handler |
| 리포지토리 | ~Repository | ~Dao |
| 컨트롤러 | ~Controller | ~Api, ~Resource |
| 설정 바인딩(@ConfigurationProperties) | ~Properties | ~Config |
| DTO-엔티티 변환 | ~Mapper | ~Converter, ~Transformer |

## Boolean 네이밍

| 개념 | 접두사 | 예시 |
|---|---|---|
| 상태/여부 | is~ | `isActive`, `isDeleted` |
| 소유 여부 | has~ | `hasCat` |
| 가능 여부 | can~ | `canEvolve` |

## 도메인 용어

이 프로젝트 고유 명사는 아래로 고정한다.

| 개념 | 사용 단어 | 비고 |
|---|---|---|
| 사용자 | User | |
| 고양이 캐릭터 | Cat | |
| 식사 기록 | Meal | history 아님 (V0.4 마이그레이션에서 rename됨) |
| 재화 | Coin | |
| 애정도 | Love | |
| 경험치 | Exp | |
| 프로필 | Profile | 신체/개인 정보 (users에서 분리됨) |
| 아이콘 | Icon | |
| 성별 | Gender | TINYINT: 0=남, 1=여 (팀 확정 전 임시 인코딩) |
| 영양 | Nutrition | |
| 하루 평가 | DailyNutritionEvaluation | POSITIVE / NEGATIVE / UNRECORDED |
| 권장 섭취량 | RecommendedDailyIntake | Recommended(권장) + Intake(섭취량) |
| 월간 | Monthly | 연간/주간이 생기면 Yearly/Weekly로 통일 |
| 일일 | Daily | |
| 캘린더 | Calendar | 주 시작 = 일요일 (프론트 확인 전 임시 가정) |
| 기간 | Period | 시작~종료 구간. 조회는 반개구간 [start, end) |

## 케이스 규칙

| 대상 | 규칙 | 예시 |
|---|---|---|
| 클래스 | PascalCase | `MealService` |
| 함수/프로퍼티 | camelCase | `getMeal`, `imageUrl` |
| DB 테이블/컬럼 | snake_case | `meal`, `image_url` |
