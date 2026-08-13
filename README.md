# 🚀 프로젝트 이름

![배너 이미지 또는 로고](링크)

# 🌅 MORU (모루) - 모닝 루틴 알람 서비스
> **"매일 아침, 당신의 가장 평온하고 활기찬 시작을 돕는 모닝 루틴 알람 앱"**
> 모루(MORU)는 모닝 루틴의 줄임말로, 사용자가 아침에 눈을 떠서 기분 좋은 루틴을 완수할 수 있도록 돕는 음성 코칭 기반 알람 어플리케이션의 서버 프로젝트입니다.


---

<br>

## 👥 멤버
| **신민정** | **이수종** | **이영선** | **정연욱** |

Team-Moru 백엔드 팀은 화면 단위가 아닌 도메인 단위 책임제를 통해 기능 간 경계를 명확히 하고, 코드 리뷰(CodeRabbit + 팀원 리뷰)를 통해 파편화를 방지합니다.


| 이름 | 역할 및 담당 영역 |
| :--- | :--- |
| **신민정** | Foundation/SwiftData + 온보딩(첫 루틴 생성 및 알람 설정 후 SwiftData에 저장) 구현 |
| **이수종** | 루틴 실행 및 완료 UI + 번들 MP3 음성 안내 + Speech Input 구현 |
| **이영선** | Home/Routine 탭에 해당하는 UI 구현  |
| **정연욱** | 이력 + 프로필 + 설정 UI 구현 |

<br>


## 📱 소개

> 단순한 REST API 제공을 넘어, 기상 직후 루틴 수행을 돕는 코칭 중심 서비스의 서버 기반을 담당합니다. Gemini API 기반 AI 루틴 추천, Redis 기반 멱등성/중복 방지, Google Cloud TTS 음성 안내 리소스 제공을 통해 클라이언트가 안정적으로 루틴 데이터를 동기화하고 실행할 수 있도록 지원합니다.

<br>

## 📆 프로젝트 기간
- 전체 기간: `2026.06.23 - YYYY.MM.DD`
- 개발 기간: `2026.07.02 - YYYY.MM.DD`

<br>

## 🤔 요구사항
For building and running the application you need:

Java 21 <br> Spring Boot 3.x <br> Gradle 8.x <br> Docker & Docker Compose <br> MySQL 8.0 (RDS, ap-northeast-2) <br> Redis 7.x

<br>

## ✅ v1 기능 계약

- 지원 범위는 iOS 클라이언트(v2 서버 연동), 한국어 응답 메시지입니다. 다국어 응답, 웹 클라이언트는 후속 범위입니다.
- 인증은 JWT(Access/Refresh) 기반이며, 소셜 로그인(Kakao/Apple)을 지원합니다. Refresh Token은 회원 탈퇴 시 cascade 삭제됩니다.
- 리소스 접근 시 소유자가 아닌 경우 IDOR 방지를 위해 동일한 404 응답을 반환합니다.
동시성이 필요한 orderIndex 갱신은 PESSIMISTIC_WRITE 락으로 보호하며, 목록 조회는 @OrderBy("orderIndex ASC")로 순서를 보장합니다.
- 생성/삭제 등 상태 변경 API는 IdempotencyService(Lua Script 기반 원자적 상태 전이)로 중복 요청을 방지하며, 삭제는 Redis tombstone 패턴으로 멱등성을 보장합니다.
AI 루틴 추천은 Gemini API(gemini-3.5-flash)를 통해 구조화된 JSON 응답으로 제공되며, 타임아웃이 설정된 RestClient를 사용합니다.
- 음성 안내는 Google Cloud TTS를 통해 생성되며, 환경변수(GOOGLE_TTS_ENABLED)로 기능을 제어합니다. 배포 시 EC2 .env 값이 GitHub Actions에 의해 덮어써지지 않도록 주의합니다.
- 자동 검증 기준 및 배포 파이프라인은 docs/DeploymentGate.md를 따릅니다.

### v2 서버 연동 준비

v1은 핵심 도메인(루틴 그룹/아이템, 인증, AI 추천)의 안정화에 집중합니다. 
서버 기능은 도메인별로 점진 확장합니다. 공통 예외 처리, 인증 경계, 모듈 추가 방법은 docs/ServerFoundation.md를 따릅니다.

<br>

## ⚒️ 개발 환경
- Backend : Spring Boot, Java 21
- 버전 및 이슈 관리 : Github, Github Issues
- 협업 툴 : Discord (Webhook 연동), Notion
- 코드 리뷰 : CodeRabbit (자동 리뷰) + 팀원 리뷰

<br>

## 🔎 기술 스택
### Envrionment
<div align="left"> <img src="https://img.shields.io/badge/git-%23F05033.svg?style=for-the-badge&logo=git&logoColor=white" /> <img src="https://img.shields.io/badge/github-%23121011.svg?style=for-the-badge&logo=github&logoColor=white" /> <img src="https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white" /> </div>

### Development
<div align="left"> <img src="https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" /> <img src="https://img.shields.io/badge/Spring%20Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" /> <img src="https://img.shields.io/badge/Spring%20Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white" /> <img src="https://img.shields.io/badge/JPA%2FHibernate-59666C?style=for-the-badge&logo=hibernate&logoColor=white" /> <img src="https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white" /> <img src="https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white" /> <img src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white" /> </div>

##Infra##
<div align="left"> <img src="https://img.shields.io/badge/AWS%20EC2-FF9900?style=for-the-badge&logo=amazonec2&logoColor=white" /> <img src="https://img.shields.io/badge/AWS%20RDS-527FFF?style=for-the-badge&logo=amazonrds&logoColor=white" /> <img src="https://img.shields.io/badge/AWS%20S3-569A31?style=for-the-badge&logo=amazons3&logoColor=white" /> <img src="https://img.shields.io/badge/Nginx-009639?style=for-the-badge&logo=nginx&logoColor=white" /> <img src="https://img.shields.io/badge/GitHub%20Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white" /> </div>


### Communication
<div align="left"> <img src="https://img.shields.io/badge/Notion-white.svg?style=for-the-badge&logo=Notion&logoColor=000000" /> <img src="https://img.shields.io/badge/Discord-5865F2?style=for-the-badge&logo=Discord&logoColor=white" /> </div> <br>

<br>

🖼️ 아키텍처 구성
<table> <tr> <td> <img width="3022" height="2022" alt="모루 인프라 아키텍처" src="https://github.com/user-attachments/assets/33ec1a86-abe6-4e14-8e0b-d88b1218048d" />
 </td> </tr> </table>

## 🔖 브랜치 컨벤션
속도감 있는 개발과 유연한 통합을 위해 main → dev → feature 3단계 브랜치 전략을 채택합니다. 데모 직전에만 안정화 브랜치를 분기합니다.

### Git-Flow 학습 및 팀 적용 방식

Team-Moru 백엔드 팀은 Git-Flow의 핵심 개념인 역할별 브랜치 분리와 PR 기반 병합을 참고하되, 현재 프로젝트 규모와 일정에 맞게 단순화해서 적용합니다.

- main은 항상 배포 가능한 최신 상태를 유지합니다. (ddl-auto: validate)
- dev는 통합 개발 브랜치로, 기능 브랜치는 이곳을 기준으로 분기/병합합니다. (ddl-auto: update)
- 기능 개발, 버그 수정, 문서 작업은 각각 목적에 맞는 브랜치에서 진행합니다.
- 작업 완료 후 Pull Request를 생성하고, CodeRabbit 자동 리뷰 + 팀원 리뷰를 거친 뒤 dev에 병합합니다.
- 실험성 작업은 spike/* 브랜치에서 검증하고, 실제 반영이 필요하면 feat/* 브랜치에서 정리해 구현합니다.

### 작업 흐름

1. 작업할 내용을 GitHub Issue로 기록합니다.
2. 작업 성격에 맞는 브랜치를 dev에서 생성합니다.
3. 기능 구현 또는 문서 작업을 진행합니다.
4. 로컬에서 빌드(./gradlew build) 및 테스트를 확인합니다.
5. 원격 브랜치에 push 후 Pull Request를 생성합니다. (Discord 알림 자동 발송)
6. CodeRabbit 리뷰 및 팀원 리뷰를 반영한 뒤 dev에 merge합니다.

### 📌 브랜치 명명 규칙 (Branch Naming)
- **`main`** : 항상 빌드 및 실행 가능한 최신 상태 유지 (PR 리뷰 후 병합)
- **`feat/#이슈번호-작업명`** : 새로운 기능 및 화면 개발 (예: `feat/#12-onboarding`)
- **`fix/#이슈번호-작업명`** : 버그 수정 (예: `fix/#21-nav-bug`)
- **`chore/#이슈번호-작업명`** : 설정, 구조, 문서, 패키지 작업 (예: `chore/#8-setup`)
- **`spike/#이슈번호-작업명`** : 기술 가능성 검증 및 R&D (예: `spike/#16-alarm-tts`)
- **`release/demo-2026-08`** : 데모 직전 생성하는 안정화 브랜치 (버그 수정 위주)

<br>

## 🌀 코딩 컨벤션

### 1. 레이아웃 및 포맷팅 (Layout & Formatting)
- **들여쓰기:** 탭(tab) 대신 **2개의 Space**를 사용합니다.
- **최대 줄 길이:** 한 줄은 최대 **99자**를 넘지 않도록 합니다. (Xcode 설정 권장)
- **콜론(`:`):** 콜론의 오른쪽에만 공백을 둡니다. (`let names: [String: String]?`)
- **빈 줄 관리:** 빈 줄에는 공백이 포함되지 않아야 하며, 모든 파일은 빈 줄로 끝납니다.
- **임포트(Import):** 알파벳 순으로 정렬하며, 내장 프레임워크 ➔ (빈 줄) ➔ 서드파티 순으로 작성합니다.

### 2. 네이밍 원칙 (Naming Rules)
- UpperCamelCase: 클래스, 인터페이스, Enum, Record (RoutineGroup, IdempotencyService)
- lowerCamelCase: 메서드, 변수, 필드 (getTotalDurationSecond(), isOwnedBy())
- UPPER_SNAKE_CASE: 상수 (static final) (MAX_RETRY_COUNT)
- 패키지명: 모두 소문자, 도메인 역순 없이 프로젝트 구조 기준 (com.moru.server.routine)
- DTO 네이밍: 요청은 ~Request, 응답은 ~Response로 접미사를 통일합니다.



// ✅ 좋은 예
public class RoutineGroupService { ... }
public record RoutineCreateRequest(String title, int totalDurationSecond) {}
private static final int MAX_RETRY_COUNT = 3;

// ❌ 나쁜 예
public class routine_group_service { ... } // 스네이크 케이스
public class RoutineGroupSvc { ... } // 불명확한 축약어



### 3. Feature Envy 방지 원칙

비즈니스 로직은 가능한 서비스가 아닌 엔티티 메서드로 응집시킵니다.

java
// ✅ 좋은 예: 엔티티가 스스로의 상태를 판단
public class Routine {
    public boolean isOwnedBy(Long userId) {
        return this.userId.equals(userId);
    }

    public void toggleActive() {
        this.active = !this.active;
    }
}

// ❌ 나쁜 예: 서비스가 엔티티 내부 필드를 직접 조작
if (routine.getUserId().equals(userId)) { ... }


### 4. 트랜잭션 및 동시성
쓰기 트랜잭션은 @Transactional로 명시하며, 읽기 전용은 @Transactional(readOnly = true)를 붙입니다.
Dirty checking을 활용하고, 불필요한 save() 명시 호출을 지양합니다.
동시성 제어가 필요한 경우 @Lock(LockModeType.PESSIMISTIC_WRITE)를 사용합니다.
N+1 문제가 예상되는 연관관계 조회는 fetch join 또는 @EntityGraph를 사용합니다.


### 5. 예외 처리
커스텀 예외는 도메인별 Enum(ErrorCode)과 함께 정의합니다. (예: ROUTINE_NOT_FOUND)
소유권이 없는 리소스 접근은 403이 아닌 404로 응답하여 존재 여부를 노출하지 않습니다.
전역 예외 처리는 @RestControllerAdvice로 일원화합니다.


 

<br>

## 📁 PR 컨벤션
* PR 시, 템플릿이 등장한다. 해당 템플릿에서 작성해야할 부분은 아래와 같다
    1. `PR 유형 작성`, 어떤 변경 사항이 있었는지 [] 괄호 사이에 x를 입력하여 체크할 수 있도록 한다.
    2. `작업 내용 작성`, 작업 내용에 대해 자세하게 작성을 한다.
    3. `추후 진행할 작업`, PR 이후 작업할 내용에 대해 작성한다
    4. `리뷰 포인트`, 본인 PR에서 꼭 확인해야 할 부분을 작성한다.
    6. `PR 태그 종류`, PR 제목의 태그는 아래 형식을 따른다.

#### 🌟 태그 종류 (커밋 컨벤션과 동일)

| 태그 | 설명 |
| --- | --- |
| `[Feat]` | 새로운 기능 또는 화면 구현 |
| `[Fix]` | 버그 수정 |
| `[Design]` | UI 디자인, 스타일, 레이아웃 변경 |
| `[Refactor]` | 동작 변경 없는 코드 구조 개선 |
| `[Docs]` | README, 문서 수정 |
| `[Chore]` | 설정, 빌드, 파일 정리 등 기타 작업 |
| `[Test]` | 테스트 코드 추가 및 수정 |

### ✅ PR 예시 모음
>  [Chore] 프로젝트 초기 세팅 <br>
>  [Feat] 프로필 화면 UI 구현 <br>
>  [Fix] iOS 26에서 버튼 클릭 오류 수정 <br>
>  [Design] 로그인 화면 레이아웃 조정 <br>
>  [Docs] README에 프로젝트 소개 추가 <br>

<br>

## 📑 커밋 컨벤션
### 🏷️ 커밋 태그 가이드

커밋 메시지는 아래 형식을 사용합니다.

```text
[태그] 작업 내용
```

예시:

```text
[Feat] 홈 화면 UI 구현
[Fix] 디자인 토큰 중복 선언 제거
[Refactor] 공용 컴포넌트 파일 구조 정리
[Docs] README 컨벤션 추가
```

| 태그 | 설명 |
| --- | --- |
| `[Feat]` | 새로운 기능 또는 화면 구현 |
| `[Fix]` | 버그 수정 |
| `[Design]` | UI 디자인, 스타일, 레이아웃 변경 |
| `[Refactor]` | 동작 변경 없는 코드 구조 개선 |
| `[Docs]` | 문서 추가 및 수정 |
| `[Chore]` | 설정, 빌드, 파일 정리 등 기타 작업 |
| `[Test]` | 테스트 코드 추가 및 수정 |

### ✅ 커밋 예시 모음
>  [Chore] 프로젝트 초기 세팅 <br>
>  [Feat] 프로필 화면 UI 구현 <br>
>  [Fix] iOS 26에서 버튼 클릭 오류 수정 <br>
>  [Design] 로그인 화면 레이아웃 조정 <br>
>  [Docs] README에 프로젝트 소개 추가 <br>

<br>

## 🗂️ 폴더 컨벤션
프로젝트 구조는 MVVM 아키텍처와 도메인(기능) 중심으로 분리하여 관리합니다.

moru-server
├─ src/main/java/com/moru/server
│  ├─ MoruServerApplication.java
│  ├─ global
│  │  ├─ config/              // SecurityConfig, RedisConfig, SwaggerConfig, RestClientConfig
│  │  ├─ exception/           // GlobalExceptionHandler, ErrorCode, CustomException
│  │  ├─ security/            // JwtProvider, JwtAuthFilter, OAuth2 관련
│  │  └─ util/                // 공통 유틸리티
│  ├─ domain
│  │  ├─ auth/                // 로그인/회원가입/소셜로그인/RefreshToken
│  │  │  ├─ controller/
│  │  │  ├─ service/
│  │  │  ├─ dto/
│  │  │  └─ entity/
│  │  ├─ routine/             // 루틴 그룹/아이템 CRUD, orderIndex 관리
│  │  │  ├─ controller/
│  │  │  ├─ service/
│  │  │  ├─ dto/
│  │  │  ├─ entity/
│  │  │  └─ repository/
│  │  ├─ routineexecution/    // 루틴 실행 이력
│  │  ├─ recommendation/      // Gemini AI 기반 온보딩/루틴 추천
│  │  ├─ tts/                 // Google Cloud TTS 연동, 비동기 처리
│  │  ├─ idempotency/         // IdempotencyService, DedupReservation, tombstone
│  │  └─ subscription/        // 구독/결제(영수증 검증)
│  └─ infra
│     ├─ redis/               // Lua Script 실행, RedisTemplate 설정
│     ├─ s3/                  // 파일 업로드
│     └─ external/            // Gemini API, Google Cloud TTS Client
├─ src/main/resources
│  ├─ application.yml
│  ├─ application-dev.yml
│  ├─ application-prod.yml
│  └─ scripts/                // Redis Lua Script (.lua)
├─ src/test/java/com/moru/server  // 도메인별 단위/통합 테스트
├─ docker
│  ├─ Dockerfile
│  └─ docker-compose.yml
├─ .github
│  ├─ workflows/               // CI/CD (Gradle build, Docker push, EC2 배포)
│  └─ ISSUE_TEMPLATE, PULL_REQUEST_TEMPLATE.md
└─ docs
   ├─ DeploymentGate.md
   └─ ServerFoundation.md

   
### 배포 파이프라인
- GitHub Actions를 통한 CI/CD: dev/main push 시 Gradle 빌드 → Docker 이미지 빌드/푸시 → EC2 배포
- 배포 환경변수는 GitHub Secrets로 관리하며, EC2 .env와 충돌 시 Secrets 우선순위를 명확히 문서화합니다.
- 프로덕션 DB는 ddl-auto: validate로 고정하여 스키마 변경 사고를 방지합니다.
- Discord Webhook을 통해 빌드/배포 결과를 팀 채널에 자동 공유합니다.
