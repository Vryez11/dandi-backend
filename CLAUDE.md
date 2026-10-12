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
| 변환 | convert | parse, transform, map, to | `convertMealStatus()` |
| 재시도 | retry | resume, redo | `retryNutritionAnalysis()` |
| 분석 | analyze | analysis(명사를 동사 자리에 금지), analyse | `analyzeNutrition()` |
| 확정(임시→최종 승격) | confirm | finalize, promote, complete | `confirmUpload()` |
| 바이너리 콘텐츠 조회(외부 스토리지) | download | fetch, retrieve (단, `get`은 도메인 리소스 조회 전용) | `downloadObject()`, `downloadText()` |

※ 예외: Mapper의 변환 확장 함수는 Kotlin 관례에 따라 `toXxx`를 사용한다. Xxx는 반환 타입 이름과 일치시킨다. 예: `Meal.toNutrition()`, `CreateMealRequest.toEntity()`

## 클래스 접미사

| 용도 | 접미사 | 금지 |
|---|---|---|
| 요청 DTO | ~Request | ~Req, ~Command |
| 응답 DTO | ~Response | ~Res, ~Dto, ~Result (단, 컨트롤러에 노출되지 않는 서비스 내부 반환값은 예외 — 예: `S3UploadResult`) |
| 서비스 | ~Service | ~Manager, ~Handler |
| 리포지토리 | ~Repository | ~Dao |
| 컨트롤러 | ~Controller | ~Api, ~Resource |
| 설정 바인딩(@ConfigurationProperties) | ~Properties | ~Config |
| DTO-엔티티 변환 | ~Mapper | ~Converter, ~Transformer |
| 계산 로직(최상위 함수 파일) | ~Calculator | ~Calc, ~Calculation, ~Util |
| 서블릿/보안 필터 | ~Filter | ~Interceptor (인터셉터는 MVC 계층의 다른 개념) |
| 예외를 에러 응답으로 변환 | ~ExceptionHandler | ~Writer, ~Responder — 예: `GlobalExceptionHandler`(@RestControllerAdvice), `SecurityExceptionHandler`(필터 단계 예외를 HandlerExceptionResolver로 위임) |
| 토큰 발급/검증 컴포넌트 | ~Provider | ~Manager, ~Generator, ~Factory, ~TokenService — 예: `JwtProvider` |
| 외부 호출과 분리한 짧은 트랜잭션 구간(별도 빈) | ~TransactionService | 같은 클래스의 @Transactional 메서드 호출은 프록시를 거치지 않아 별도 빈으로 둔다 — 예: `InquiryTransactionService` |

## Boolean 네이밍

| 개념 | 접두사 | 예시 |
|---|---|---|
| 상태/여부 | is~ | `isActive`, `isDeleted` |
| 보유/존재 여부 | has~ | `hasCat`, `hasNext`, `hasUnreadAnswer` — Boolean 프로퍼티 전용. 존재를 확인하는 메서드는 exists 동사 |
| 가능 여부 | can~ | `canEvolve` |

## 도메인 용어

이 프로젝트 고유 명사는 아래로 고정한다.

### 공통

| 개념 | 사용 단어 | 비고 |
|---|---|---|
| 임시 구현 | Stub | 프로덕션 코드의 임시 대역 접두 (예: StubPushClient). Fake, Mock 금지 — Mock은 테스트 전용 |
| 커서 | Cursor | 무한 스크롤에서 다음 묶음의 기준점(직전 묶음의 마지막 id). 응답은 nextCursor + hasNext, 조회는 id < cursor. 페이지 번호 방식은 Offset으로 구분 |

### 사용자

| 개념 | 사용 단어 | 비고 |
|---|---|---|
| 사용자 | User | |
| 프로필 | Profile | 신체/개인 정보 (users에서 분리됨) |
| 성별 | Gender | MALE / FEMALE / OTHER. DB는 VARCHAR(20)에 enum 이름 저장 (V0.14에서 TINYINT 0/1 인코딩 대체) |

### 고양이

| 개념 | 사용 단어 | 비고 |
|---|---|---|
| 고양이 캐릭터 | Cat | |
| 재화 | Coin | |
| 애정도 | Love | |
| 경험치 | Exp | |
| 고양이 체형 | Weight | CatWeight(-2~2, LEAN~PLUMP). 사용자 몸무게(Profile.weight, kg)와 단어가 겹치니 Cat/Profile 맥락으로 구분 |
| 체형 변화 단계 | Step | 한 평가 구간에서 체형이 움직이는 칸 수(-1/0/1). calculateWeightStep, updateWeightByStep. Delta, Diff 금지 |

### 식사

| 개념 | 사용 단어 | 비고 |
|---|---|---|
| 식사 기록 | Meal | history 아님 (V0.4에서 history → meal, V0.31에서 meal → meals로 rename됨) |
| 아이콘 | Icon | |
| 식사 상태 | MealStatus | WAITING / ANALYZING / COMPLETED / FAILED / UNKNOWN. DB status 문자열을 enum으로 변환해 응답 |
| 분석 | Analysis | 영양 분석. 명사 위치에 사용 (AnalysisService, retryNutritionAnalysis). 진행 중 상태는 MealStatus.ANALYZING |

### 영양

| 개념 | 사용 단어 | 비고 |
|---|---|---|
| 영양 | Nutrition | 영양 4종(calory/carbs/protein/fat) 값 객체로도 사용 (응답 current/target 공용) |
| 영양소 | Nutrient | Nutrition의 개별 항목(calory/carbs/protein/fat)을 가리킬 때. 목록은 복수형 Nutrients |
| 칼로리 | Calory | 코드·응답·엔티티/DB 모두 calory로 통일 (calories 금지) |
| 탄수화물 | Carbs | carbohydrate 아님 |
| 단백질 | Protein | 단수형 고정 (proteins 아님) |
| 지방 | Fat | 단수형 고정 (fats 아님) |
| 하루 평가 | DailyNutritionEvaluation | POSITIVE / NEGATIVE / UNRECORDED |
| 권장 섭취량 | RecommendedDailyIntake | Recommended(권장) + Intake(섭취량). 전용 클래스 없음 — calculateRecommendedDailyIntake()가 Nutrition을 반환 |
| 현재 섭취량 | Current | 하루 영양 합계. 응답에서 Target과 쌍으로 사용 |
| 목표 섭취량 | Target | 응답 표기용. 값은 RecommendedDailyIntake 계산 결과 |

### 기간·캘린더

| 개념 | 사용 단어 | 비고 |
|---|---|---|
| 월간 | Monthly | 연간/주간이 생기면 Yearly/Weekly로 통일 |
| 일일 | Daily | |
| 캘린더 | Calendar | 주 시작 = 일요일 (프론트 확인 전 임시 가정) |
| 기간 | Period | 시작~종료 구간. 조회는 반개구간 [start, end) |
| 캘린더 범위 | Range | 달력 그리드의 [시작일, 종료일] 양끝 포함 구간 (calculateMonthlyCalendarRange). 조회용 반개구간은 Period |

### 인증·토큰

| 개념 | 사용 단어 | 비고 |
|---|---|---|
| 인증 | Auth | authentication 축약. 패키지명(auth), 에러코드(api.auth.*) 접두로 사용 |
| 인증된 현재 사용자 | AuthUser | SecurityContext의 principal 타입. Principal, LoginUser, SessionUser 금지 |
| 현재 사용자 주입 | CurrentUser | @AuthenticationPrincipal 메타 어노테이션(@CurrentUser). 인증 필수 경로의 컨트롤러 파라미터 전용 |
| 소유권 | Ownership | 리소스가 요청 사용자 소유인지. 검증은 validateOwnership — ownership은 한 단어 (OwnerShip 금지) |
| 보안 인프라 | Security | Spring Security 메커니즘(설정·필터·principal)의 패키지명(security). 인증 API(컨트롤러/DTO)는 auth 패키지와 분리 |
| JWT | Jwt | 식별자 표기는 Jwt (클래스 `JwtProvider`, 패키지 security.jwt). JWT 전체 대문자, JsonWebToken 금지 |
| 접근 토큰 | AccessToken | 30분 수명, API 인증용. type 클레임 값은 "access". 필드명 accessToken |
| 갱신 토큰 | RefreshToken | 15일 수명, 재발급용. type 클레임 값은 "refresh". 필드명 refreshToken |
| 토큰 수명 | TimeToLive | accessTimeToLive / refreshTimeToLive. Expiration, Ttl, ExpiresIn, Validity 금지 |
| 시크릿 키 | SecretKey | 설정의 Base64 문자열(JwtProperties.secretKey, .env의 JWT_SECRET_KEY)과 디코딩된 키 객체 모두 secretKey |

### 알림

| 개념 | 사용 단어 | 비고 |
|---|---|---|
| 서비스 알림 | ServicePush | 사용자가 요청한 기능을 수행하는 알림(분석 완료·식사 리마인더). 기본 수신 |
| 마케팅 알림 | MarketingPush | 광고성 정보로 볼 여지가 있는 알림(리텐션 복귀 유도 등). 사전 동의 필요, 기본 거부 |
| 알림 수신 설정 | PushSetting | profile의 수신 여부 묶음. PushConfig, PushPreference, PushOption 금지 |
| 동의 | Agreed | 광고성 정보 수신 동의. 시점은 marketingAgreedAt. Consent, Approval, Accept 금지 |

### 문의

| 개념 | 사용 단어 | 비고 |
|---|---|---|
| 문의 | Inquiry | 문의함의 문의 한 건. 테이블 inquiries. 우편함·문의함 화면도 코드에서는 Inquiry로 통일 (Mailbox 금지) |
| 문의 유형 | InquiryCategory | BUG_REPORT / QUESTION / SUGGESTION. Type, Kind 금지 |
| 질문 | Question | 사용자가 작성한 문의 부분(유형·제목·내용·첨부 이미지). 필드는 questionTitle / questionContent, 수정은 updateQuestion |
| 답변 | Answer | 운영자가 작성한 답변. 필드는 answerTitle / answerContent. Reply 금지 |
| 답변 시각 | AnsweredAt | 최초 답변 시각으로 고정. 답변 여부는 상태 컬럼 없이 isAnswered(= answeredAt != null)로 계산 |
| 답변 읽음 | Read | 사용자가 답변을 읽음. 시각은 answerReadAt(처음 읽은 시각 고정). 조회 동사 read 금지와는 별개의 도메인 의미이며, 메서드 동사로는 쓰지 않는다 |
| 안 읽은 답변 | UnreadAnswer | 답변이 있고 아직 읽지 않음. hasUnreadAnswer(문의 단건), hasUnreadInquiryAnswer(홈), existsUnreadAnswer(조회). New 금지 |

## 케이스 규칙

| 대상 | 규칙 | 예시 |
|---|---|---|
| 클래스 | PascalCase | `MealService` |
| 함수/프로퍼티 | camelCase | `getMeal`, `imageUrl` |
| DB 테이블/컬럼 | snake_case | `meals`, `image_url` |
| DB 테이블 이름 | 복수형 | `meals`, `refresh_tokens` (V0.31에서 통일). 엔티티 클래스는 단수형(`Meal`) + `@Table(name = "meals")` |
