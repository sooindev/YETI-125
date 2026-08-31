<div align="center">

# YETI-125

치지직 스트리머 이리온(IRION)의 비공식 팬사이트.
라이브 상태, 방송 일정, 클립 아카이브를 한곳에서.

<br>

**[yeti-125.com](https://yeti-125.com)**

<br>
<br>

</div>

---

<br>

<div align="center">

<img src="docs/screenshots/home.webp" alt="YETI-125 홈 — 실시간 방송 상태와 이리온 팬사이트 메인 화면" width="900">

</div>

<br>

## 소개

YETI-125는 치지직 스트리머 이리온의 활동을 한곳에 모아 보여주는 팬 아카이브입니다.

chzzk API와 연동해 실시간 방송 상태를 보여주고,
인기 클립과 다시보기를 자동으로 수집하며,
관리자가 직접 등록한 방송 일정을 캘린더로 제공합니다.

> 순수한 팬 활동의 일환으로 제작된 비상업적 프로젝트입니다.
> 원 저작자와 직접적인 관련이 없으며, 정보 제공 및 아카이빙 목적으로만 운영됩니다.

<br>

> **처음 오셨다면 → [YETI-125 설계도](https://claude.ai/code/artifact/c8bf3548-904d-44b6-91d1-e9de9e328aac)**
>
> 폴더 구조와 요청 하나가 화면이 되기까지의 흐름을, 함수 단위까지 그림으로 풀어 쓴 해설서입니다.
> 이 README 는 운영과 의사결정 기록에 집중하고, 구조 설명은 그쪽이 더 자세합니다.

<br>

## 기능

### 홈

실시간 방송 상태(LIVE / OFFLINE), 인기 클립과 다시보기,
채널과 SNS 링크를 한 화면에 정리합니다.

클립은 카드를 누르면 치지직으로 나가지 않고 그 자리에서 재생됩니다.
치지직 공식 임베드 플레이어를 모달에 띄우는 방식이라
조회수 집계와 시청 제한은 치지직 정책을 그대로 따릅니다.
새 탭으로 열고 싶으면 `⌘`(또는 `Ctrl`)를 누른 채 클릭하면 됩니다.

다시보기는 치지직이 임베드 경로를 제공하지 않아 링크 이동입니다.
나가기 전에 어디로 가는지 알리는 확인 모달을 띄웁니다.
매번 묻는 것이 번거로우면 "다시 묻지 않기"로 끌 수 있고,
끈 뒤에는 섹션 머리말에 되돌리는 링크가 나타납니다.
선택은 기기에 기억됩니다.

목록에서 감출 다시보기는 `LiveFeedService` 의 `HIDDEN_VIDEO_NOS` 에
`videoNo` 로 적어둡니다. 제목은 바뀔 수 있지만 번호는 그대로입니다.
지우는 것이 아니라 이 사이트에서만 가리는 것이라 치지직에는 남아 있습니다.

<img src="docs/screenshots/clips.webp" alt="인기 클립 목록 — 조회수 순으로 자동 수집된 클립 카드" width="900">

### 방송 일정

월간 캘린더 뷰로 방송 일정을 확인합니다.
저스트 채팅, 종합게임, 노래방송, 합방 — 유형별로 색을 달리해 한눈에 구분됩니다.

<img src="docs/screenshots/schedule.webp" alt="방송 일정 캘린더 — 유형별 색상과 다가오는 일정 목록" width="900">

### 프로필

캐릭터 설정, 제작 크레딧, 데뷔일과 생일 D-Day,
채널과 SNS 링크를 정돈된 형태로 제공합니다.

<img src="docs/screenshots/profile.webp" alt="프로필 명세 — 기본 정보, 상세 정보, 크레딧, 팬 정보" width="900">

### 관리자

방송 일정을 등록·수정·삭제하고, 캘린더에서 드래그로 옮길 수 있습니다.
좁은 화면에서는 월간 격자 대신 목록 뷰로 시작해 제목과 시간을 그대로 읽을 수 있습니다.

인증은 필터와 인터셉터 두 겹으로 봅니다.
필터가 `DispatcherServlet` 앞에서 정적 페이지까지 막고,
인터셉터가 컨트롤러 진입 직전에 한 번 더 확인합니다.
두 곳 모두 AJAX 요청에는 리다이렉트 대신 401 을 돌려줍니다.

### 인트로

홈에 처음 들어오면 문이 열리는 연출이 재생됩니다.
하루에 한 번만 보여주므로, 같은 날 다시 방문하면 바로 본문이 나옵니다.
(마지막으로 문을 연 날짜를 `localStorage` 에 두고 오늘과 비교합니다)

<img src="docs/screenshots/intro.webp" alt="도어 인트로 — 문이 열리며 사이트로 진입하는 연출" width="900">

### 다크모드

상단바의 토글로 전환합니다.
선택은 기기에 기억되고, 고른 적이 없으면 OS 설정을 따릅니다.

<br>

## 기술 스택

| 영역 | |
|------|---|
| 백엔드 | Java (빌드 타깃 8 · 실행 11 이상) · Spring Framework 5.3 · MyBatis |
| 데이터베이스 | MariaDB · HikariCP |
| 프론트엔드 | HTML5 · CSS3 · JavaScript · jQuery 3.7 |
| 라이브러리 | FullCalendar 6.1 · Jackson · Hibernate Validator |
| 테스트 | JUnit 4 (186개) |
| 외부 API | [chzzk API](https://chzzk.naver.com/) · 클립 임베드 플레이어 |
| 빌드 / 서버 | Maven · Apache Tomcat 9 |

<br>

## 아키텍처

nginx가 HTTPS를 끊고 톰캣에 평문으로 넘깁니다.
방송 일정은 MariaDB에서, 라이브 상태·클립·다시보기는 치지직 API에서 옵니다.

```mermaid
flowchart LR
    subgraph browser["브라우저"]
        UI["HTML · CSS\njQuery · FullCalendar"]
    end

    subgraph prod["운영 서버"]
        NG["nginx\nHTTPS 종료"]
        subgraph tc["Tomcat 9"]
            FC["필터 체인"]
            DS["DispatcherServlet"]
            CT["Controller\nHome · Live · Schedule · Admin"]
            SV["Service\nLiveFeed · Schedule · Admin"]
            MP["MyBatis Mapper"]
        end
    end

    DB[("MariaDB\nfor_125")]
    CZ["치지직 API\napi.chzzk.naver.com"]

    UI -->|HTTPS| NG
    NG -->|"HTTP :8080"| FC
    FC --> DS --> CT --> SV
    SV --> MP --> DB
    SV -->|ChzzkClient| CZ
```

### 요청 처리 파이프라인

필터 여섯 개가 순서대로 지나갑니다. 순서는 `web.xml` 의 `filter-mapping` 선언 순입니다.

`adminLoginFilter` 가 `DispatcherServlet` **앞에서** 도는 것이 중요합니다.
정적 HTML까지 막아주는 대신, 인증 실패 응답을 이 필터가 직접 만들어야 합니다
(트러블슈팅의 "세션이 끊기면 관리자 캘린더가 에러도 없이 비는 문제" 참고).

```mermaid
flowchart TD
    R(["요청"]) --> E["encodingFilter\n/* · UTF-8 강제"]
    E --> Q1{"/resources/* ?"}
    Q1 -->|예| SE["staticResourceEncodingFilter\nContent-Type charset"]
    Q1 -->|아니오| SC
    SE --> SC["staticResourceCacheFilter\n/* · 캐시 재검증 + 보안 헤더\nREQUEST · ERROR"]
    SC --> LR["legacyHtmlRedirectFilter\n/* · 옛 .html → 정규 주소 301"]
    LR --> Q2{"/admin/* ?"}
    Q2 -->|아니오| DS
    Q2 -->|예| AL["adminLoginFilter\n세션 확인"]
    AL --> CF["csrfFilter\nPOST·PUT·DELETE 토큰 검증"]
    CF --> DS["DispatcherServlet"]
    DS --> IC["AdminLoginInterceptor\n/admin/** 재확인"]
    IC --> CT(["Controller"])
```

`staticResourceCacheFilter` 만 `<dispatcher>` 를 두 개 선언합니다.
필터 기본값은 `REQUEST` 뿐이라, 그대로 두면 `<error-page>` 로 넘어가는 404·500 응답에는
이 필터가 아예 돌지 않아 **그 두 페이지만 보안 헤더 없이 나갑니다**
(트러블슈팅의 "404·500 페이지에만 보안 헤더가 붙지 않던 문제" 참고).
`<dispatcher>` 를 하나라도 쓰면 기본값이 사라지므로 `REQUEST` 를 함께 적어야 합니다.

### 데이터 모델

테이블은 둘뿐이고 **서로 외래키로 엮이지 않습니다.**
일정에 작성자를 남기지 않기 때문입니다. 관리자가 한 명이라 지금은 필요가 없고,
여러 명이 되면 `tb_schedule` 에 `admin_id` 를 더하면 됩니다.

삭제는 `del_yn` 으로 표시만 하고 행은 남깁니다.

```mermaid
erDiagram
    tb_admin {
        bigint admin_id PK
        varchar admin_login_id UK
        varchar admin_password "pbkdf2 형식"
        varchar admin_name
        datetime last_login_date
        datetime reg_date
        datetime mod_date
        char del_yn
    }
    tb_schedule {
        bigint schedule_id PK
        varchar title
        text description
        varchar schedule_type
        datetime start_date
        datetime end_date
        char all_day_yn
        char display_yn
        varchar color
        datetime reg_date
        datetime mod_date
        char del_yn
    }
```

### 치지직 연동과 캐시

방송 상태·클립·다시보기는 우리 데이터가 아니라 매번 치지직에 물어야 하는 값입니다.
호출 하나가 최대 5초(`READ_TIMEOUT`)를 쓰기 때문에 그대로 두면
방문자 수만큼 그 시간이 곱해집니다. `LiveFeedService` 가 앞에서 이것을 흡수합니다.

| 상태 | 하는 일 | 기준 |
|---|---|---|
| 캐시가 신선함 | 치지직을 부르지 않고 보관한 값을 준다 | 방송 상태 1분 · 클립/다시보기 10분 |
| 만료됨 | **한 스레드만** 갱신하러 가고 나머지는 그 결과를 나눠 쓴다 | 락 + 이중 확인 |
| 갱신 실패 | 오류 대신 **만료된 값이라도** 돌려준다 | 화면이 죽지 않게 |
| 방금 실패함 | 잠시 동안은 다시 두드리지 않는다 | 30초 (`FAILURE_BACKOFF`) |

마지막 줄이 없으면 치지직이 멈춘 동안 요청마다 락 안에서 5초를 처음부터 다시 기다립니다
(트러블슈팅의 "치지직이 멈추면 요청마다 타임아웃을 다시 기다리는 문제" 참고).

백오프는 가장 짧은 TTL(방송 상태 1분)보다 짧게 둡니다 —
복구를 알아채는 데 걸리는 시간이 정상일 때의 갱신 주기보다 늦어지면 안 되기 때문입니다.
갱신에 성공하면 실패 기록은 지웁니다.

<br>

## 디자인

파스텔 라이트 테마와 다크 테마.
하늘색을 메인으로, 분홍을 포인트로 사용합니다.

색은 전부 CSS 커스텀 프로퍼티로 관리합니다.
다크 테마는 같은 토큰 이름에 야간 값을 덮어쓰는 방식이라,
각 페이지 CSS를 건드리지 않고도 전환됩니다.
어두운 배경에서는 액센트를 한 단계 밝혀 탁해 보이지 않게 했습니다.

헤더의 테마 버튼은 시스템 → 라이트 → 다크 순으로 돕니다.
시스템 상태에서는 OS 설정을 따라가며, 페이지를 켜 둔 채로
OS 모드를 바꿔도 새로고침 없이 그 자리에서 바뀝니다.

타이포그래피는 디스플레이의 Anton과 본문의 JetBrains Mono를 대비시켜
정보의 위계를 만들었습니다.
화면 전체에 옅은 그레인 텍스처를 더해 무게감을 주고,
미디어 카드는 균일한 3열로 정렬해 가독성에 집중했습니다.

모바일에서는 터치 타깃을 44px 이상으로 확보하고,
입력창 글자를 16px로 두어 iOS 사파리의 자동 확대를 막았습니다.

<br>

## 보안

관리자 영역은 팬사이트 규모에 맞는 선에서 기본기를 갖춰두었습니다.

| | |
|---|---|
| 비밀번호 | PBKDF2-HMAC-SHA256 · 210,000회 반복 · 상수 시간 비교 |
| 세션 | 로그인 성공 시 세션 재발급 (세션 고정 방어) |
| 쿠키 | `HttpOnly` · `Secure` · `SameSite=Lax` |
| CSRF | 상태 변경 요청에 토큰 검증, 로그아웃은 POST |
| 무차별 대입 | 계정 기준 실패 횟수 제한 · 자동 해제 · 추적 항목 수 상한 |
| 계정 열거 | 아이디 존재 여부와 무관하게 같은 시간을 들여 응답 |
| 자원 남용 | 일정 조회 기간 상한 · 로그인 아이디 길이 상한 |
| 경로 우회 | 인증 판정 전 경로 정규화 — `..` · 퍼센트 인코딩 · 역슬래시 · 중복 슬래시 · 경로 파라미터 |
| 응답 헤더 | CSP · HSTS · `X-Frame-Options` · `X-Content-Type-Options` · `Referrer-Policy` (오류 페이지 포함) |
| 외부 스크립트 | jQuery · FullCalendar 에 SRI 해시 |

비밀번호 저장 포맷은 알고리즘 이름을 앞에 둡니다.

```
pbkdf2$210000$<salt>$<hash>
```

이 포맷으로 옮기기 전에는 SHA-256 을 한 번만 돌린 값을 저장했습니다.
두 형식을 한동안 같이 받아주면서, 로그인에 성공하는 순간 — 원문을 아는
시점이 거기뿐입니다 — 새 형식으로 다시 저장하는 방식으로 계정을 하나씩
옮겼습니다. DB를 한 번에 갈아엎지 않아도 되는 대신, 옛 형식을 검증하는
경로가 남아 있어야 했습니다.

계정이 모두 옮겨간 것을 확인한 뒤 그 경로는 지웠습니다.
옛 형식은 검증이 1ms도 걸리지 않아서, **그런 계정만 응답이 눈에 띄게
빨리 돌아왔기 때문입니다.** 지금은 알 수 없는 형식의 해시를 만나면
같은 시간을 들여 거절하고, 원인을 `ERROR` 로그로 남깁니다 —
그러지 않으면 화면에는 "비밀번호가 틀렸다"로만 보여 진짜 이유를
알아낼 방법이 없습니다.

반복 횟수를 올릴 때 기존 해시를 다시 만드는 경로는 그대로 있습니다.
`ITERATIONS` 를 높이면 다음 로그인부터 차례로 새 값으로 바뀝니다.

비밀번호를 몰라도 **응답 시간만 재면 아이디의 존재 여부를 알 수 있습니다.**
관리자 계정이 하나뿐이라 아이디가 드러나는 순간 표적이 확정되고,
위의 실패 횟수 제한과 엮이면 관리자를 계속 잠가두기도 쉬워집니다.
그래서 없는 아이디에도 검증에 드는 만큼의 계산을 그대로 씁니다.

| 로그인 실패 경로 | 응답 시간 |
|---|---|
| 있는 아이디 + 틀린 비밀번호 | 197.8 ms |
| 없는 아이디 | 199.2 ms |
| 알 수 없는 형식의 해시 | 196.6 ms |

CSP 의 `script-src` 에는 `'unsafe-inline'` 이 없습니다.
페이지에서 인라인 `onclick` 을 전부 걷어냈기 때문입니다.
닫기 버튼 하나를 인라인으로 되돌리는 순간 이 방어가 통째로 무의미해지므로,
새 핸들러는 `data-*` 속성과 이벤트 위임으로 붙입니다.

이 규칙은 `StaticResourceCacheFilter` 가 **실제로 내보낸 응답 헤더**를 읽어 검사합니다
(`StaticResourceCacheFilterTest`). `script-src` 에 `'unsafe-inline'` 이나 `'unsafe-eval'` 이
들어오면 빌드가 깨집니다. 상수를 직접 읽지 않는 이유는, 그러면 필터가 헤더를 안 붙여도
테스트가 통과하기 때문입니다. 같은 테스트가 `object-src 'none'` · `frame-ancestors 'none'` ·
`base-uri 'self'` · `form-action 'self'` 처럼 빠져도 티가 안 나는 지시자들도 함께 못 박습니다.

보안 헤더는 404·500 오류 페이지에도 붙습니다. 필터 기본 디스패치가 `REQUEST` 뿐이라
예전에는 그 두 페이지만 빠져나갔습니다 (트러블슈팅 참고).

`SameSite` 는 Servlet 4.0 의 `<cookie-config>` 에 항목이 없어
`webapp/META-INF/context.xml` 의 톰캣 쿠키 처리기로 지정합니다.

인증 실패를 돌려주는 방식은 요청 종류에 따라 다릅니다.
AJAX에는 401 JSON을, 브라우저 요청에는 로그인 페이지 리다이렉트를 보냅니다.

```mermaid
sequenceDiagram
    participant B as 브라우저
    participant F as adminLoginFilter
    participant I as Interceptor
    participant C as Controller

    Note over B,F: 세션이 만료된 상태

    B->>F: GET /admin/schedule/list<br/>(X-Requested-With: XMLHttpRequest)
    F->>F: 세션 없음 + AJAX 판정
    F-->>B: 401 + JSON
    Note right of B: 로그인 화면으로 이동

    B->>F: GET /admin/schedule<br/>(Accept: text/html)
    F->>F: 세션 없음 + 일반 요청
    F-->>B: 302 → /admin/admin-login

    Note over B,C: 로그인된 경우
    B->>F: 요청
    F->>I: 통과
    I->>C: 통과
```

> `Secure` 때문에 로컬 `http://localhost:8080` 에서는 관리자 로그인이
> 사파리에서 유지되지 않습니다. 크롬과 파이어폭스는 localhost 를
> 신뢰할 수 있는 출처로 보아 그대로 동작합니다.

### 의존성 취약점 감시

애플리케이션 코드가 아무리 멀쩡해도 가져다 쓰는 라이브러리에 구멍이 나면
같이 뚫립니다. 두 가지를 겁니다.

**자동 (키 없이 동작)** — [.github/dependabot.yml](.github/dependabot.yml)

깃허브가 매주 월요일 `pom.xml` 을 훑어 새 버전이 나오면 PR 을 열어줍니다.
패치 단위 업데이트는 한 PR 로 묶고, 지금 올릴 수 없는 것(Spring 6 계열 —
`jakarta` 네임스페이스가 필요합니다)은 소음이 되므로 제외했습니다.

보안 경고(Dependabot alerts)는 이 파일과 별개입니다. 저장소
`Settings › Code security` 에서 켜며, 공개 저장소는 기본으로 켜져 있습니다.

**수동 (NVD API 키 필요)** — `security` 프로파일

```bash
mvn -Psecurity verify -Dnvd.api.key=발급받은키
```

CVSS 7.0 이상이 나오면 빌드를 실패시키고, 보고서는
`target/dependency-check-report.html` 에 남습니다.

키는 <https://nvd.nist.gov/developers/request-an-api-key> 에서 무료로
받습니다. 2024년부터 익명 접근이 막혀 **키 없이는 아예 돌지 않습니다**.
저장소에 적지 말고 명령줄이나 `NVD_API_KEY` 환경변수로 넘깁니다.

오탐은 [dependency-check-suppress.xml](dependency-check-suppress.xml) 에
**왜 해당되지 않는지 근거를 적어** 예외 처리합니다. 근거 없는 예외는 스캔을
통과시키려고 눈을 가리는 것과 같습니다.

기본 빌드에는 들어가지 않습니다 — NVD 데이터를 받느라 몇 분씩 걸립니다.

<br>

## 시작하기

**JDK 11 이상**, Maven 3.6 이상, MariaDB 10 이상, Apache Tomcat 9 이상이 필요합니다.

바이트코드는 자바 8 타깃으로 컴파일되지만, 실행 환경은 자바 11 이상이어야 합니다.
톰캣이 JSP 를 실행 중에 컴파일할 때 쓰는 ECJ 가 11 이상을 요구하기 때문입니다
(자바 8 서버에 올리면 모든 페이지가 500 이 됩니다 — [트러블슈팅](#자바-8-서버에서-모든-페이지가-500-이-되는-문제) 참고).
`scripts/deploy.sh` 는 배포 전에 서버 자바 버전을 확인하고 11 미만이면 중단합니다.

#### 1. 저장소 클론

```bash
git clone https://github.com/sooindev/YETI-125.git
cd YETI-125
```

#### 2. 데이터베이스 준비

`docs/db/schema.sql` 을 실행해 `for_125` 데이터베이스와
`tb_admin`, `tb_schedule` 테이블을 생성합니다.

```bash
mysql -u root -p < docs/db/schema.sql
```

#### 3. 접속 정보 설정

`src/main/resources/properties/database.properties` 에서 계정 정보를 수정합니다.
이 파일은 `.gitignore` 대상이라 저장소에 포함되지 않습니다.

```properties
db.driver=org.mariadb.jdbc.Driver
db.url=jdbc:mariadb://localhost:3306/for_125?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Seoul
DB_USERNAME=your_username
DB_PASSWORD=your_password
```

#### 4. 빌드와 실행

```bash
mvn clean package
cp target/*.war $TOMCAT_HOME/webapps/ROOT.war
$TOMCAT_HOME/bin/startup.sh
```

HTML이 `/resources/...` 절대경로를 사용하므로 **ROOT 컨텍스트로 배포해야 합니다.**
`yeti-125.war` 그대로 두면 CSS와 JS가 404가 됩니다.

macOS에서는 위 과정을 스크립트 하나로 대신할 수 있습니다.
빌드 → 로컬 톰캣에 ROOT로 배포 → DB 연동까지 확인합니다.

```bash
./scripts/run-local.sh
```

DB 비밀번호는 스크립트에 두지 않습니다.
`~/.my.cnf` 의 `[client]` 섹션을 읽거나 `YETI_DB_PASSWORD` 환경변수를 받습니다.

```ini
[client]
password="비밀번호"
```

`my.cnf` 에서 `#` 는 주석 시작 문자입니다. 비밀번호에 `#` 가 들어 있으면
거기서 잘린 채 읽혀 원인을 짐작하기 어려운 `Access denied` 가 납니다.
값은 큰따옴표로 감싸세요. `user=` 는 넣지 않습니다 —
`[client]` 는 모든 mariadb 클라이언트에 적용되어 소켓 인증까지 끌려갑니다.

테스트는 빌드에 포함되어 있고 따로 돌릴 수도 있습니다.
DB 는 필요 없습니다 — 스프링 컨텍스트를 띄우지 않습니다.

```bash
mvn test
```

`main` 에 올리거나 PR 을 열면 깃허브 액션이 같은 테스트를 자동으로 돌립니다
(`.github/workflows/test.yml`). 실패하면 어느 테스트가 깨졌는지 보고서가 남습니다.

#### 5. 접속

| | |
|---|---|
| 메인 사이트 | `http://localhost:8080` |
| 관리자 | `http://localhost:8080/admin/admin-login` |

<br>

## 배포

`database.properties` 는 classpath 리소스라 **war 안에 그대로 패키징됩니다.**
로컬 설정이 담긴 war를 운영에 올리면 DB 연결에 실패해 사이트가 내려갑니다.

이를 막기 위해 빌드 프로파일을 분리했습니다.
운영 설정은 `src/main/resources-prod/properties/database.properties` 에 두고,
`prod` 프로파일로 빌드할 때만 덮어씁니다. 이 파일 역시 `.gitignore` 대상입니다.

```bash
mvn clean package          # 로컬 개발용
mvn clean package -Pprod   # 운영 배포용
```

운영 설정이 채워지지 않았거나 주소가 로컬을 가리키면 **빌드 단계에서 중단됩니다.**

배포 대상 서버 주소는 저장소에 두지 않습니다 — 공개 저장소이기 때문입니다.
`deploy.env` 에 적거나 환경변수로 넘깁니다. 이 파일은 `.gitignore` 대상입니다.

```bash
cp deploy.env.example deploy.env
# YETI_DEPLOY_SERVER=사용자@서버주소
```

배포는 스크립트 한 줄로 끝납니다.
테스트 → 빌드 → 서버 자바 확인 → 전송 → 교체 → 검증 순으로 진행하고,
검증에 실패하면 직전 백업으로 자동 롤백합니다.

검증은 정상 페이지 네 곳과 DB 연동 API 가 200 인지 보고,
**없는 주소가 404 를 주는지와 그 응답에 보안 헤더가 붙어 있는지**도 함께 봅니다.
상태 코드만 보면 안 됩니다 — 톰캣만 뜨고 앱이 아직 안 붙은 순간에도 404 는 나오기 때문에,
헤더까지 확인해야 "우리 오류 화면이 우리 필터를 타고 나왔다"가 증명됩니다.

```bash
./scripts/deploy.sh
./scripts/deploy.sh --skip-tests   # 급할 때만
```

테스트는 배포 경로에 포함되어 있습니다.
예전에는 `-DskipTests` 로 빌드해서 손으로 `mvn test` 를 칠 때만 확인이 됐고,
깨진 코드가 그대로 운영에 올라갈 수 있었습니다.

```mermaid
flowchart TD
    A(["./scripts/deploy.sh"]) --> T{"배포 대상 확인\ndeploy.env"}
    T -->|없음| Z2(["배포 중단"])
    T -->|있음| B["mvn clean package -Pprod\n(테스트 포함)"]
    B -->|테스트 실패| Z3(["배포 중단"])
    B --> C{"설정 가드\nCHANGE_ME · db.url 검사"}
    C -->|실패| X(["빌드 중단"])
    C -->|통과| D["war 내용 재확인"]
    D --> J{"서버 자바 확인\n11 이상"}
    J -->|미만| Z(["배포 중단"])
    J -->|통과| E["scp → 서버"]
    E --> F["기존 ROOT.war 백업"]
    F --> G["교체 후 톰캣 재기동"]
    G --> H{"헬스체크\nAPI + 페이지 4곳 + 오류 화면"}
    H -->|전부 통과| I(["외부 접근 확인\nhttps://yeti-125.com"])
    H -->|실패| R["백업으로 자동 롤백"]
    R --> Y(["이전 버전으로 복구"])
```

### 데이터베이스 백업

운영 서버에서 `scripts/db-backup.sh` 가 cron 으로 돕니다.
일정은 전부 관리자가 손으로 넣은 것이라 잃으면 되돌릴 방법이 없습니다.

| | |
|---|---|
| 설치 위치 | `/usr/local/sbin/yeti-db-backup.sh` |
| 보관 위치 | `/var/backups/yeti-125` (권한 600) |
| 로그 | `/var/log/yeti-db-backup.log` |
| 보관 개수 | 14개 — 넘으면 오래된 것부터 지웁니다 |

```bash
sudo install -m 700 scripts/db-backup.sh /usr/local/sbin/yeti-db-backup.sh
sudo crontab -e
# 매일 새벽 4시
0 4 * * * /usr/local/sbin/yeti-db-backup.sh >> /var/log/yeti-db-backup.log 2>&1
```

스크립트에 비밀번호가 없습니다.
우분투의 MariaDB `root` 는 `unix_socket` 인증이라 root 로 실행하면 그냥 붙습니다.

덤프는 `--single-transaction` 으로 InnoDB 를 잠그지 않고 일관된 시점을 뜨고,
`--quick` 으로 한 행씩 흘려보냅니다 (2GB 서버라 메모리를 아껴야 합니다).

**중간에 끊긴 덤프가 정상 백업인 척 남지 않도록** 먼저 `.part` 로 쓰고,
`gzip -t` 로 압축이 온전한지 확인한 뒤에만 정식 이름으로 바꿉니다.

복구는 이렇게 합니다.

```bash
gunzip -c /var/backups/yeti-125/for_125-20260826-040001.sql.gz | mysql for_125
```

> 옛 SHA-256 해시가 담긴 백업을 되살리면 그 관리자 계정은 로그인할 수 없습니다.
> `PasswordUtil.main` 으로 해시를 새로 넣어야 합니다.

<br>

## 프로젝트 구조

```
YETI-125/
├── src/
│   ├── main/
│   │   ├── java/com/irion/
│   │   │   ├── common/
│   │   │   │   ├── controller/  HTTP 처리 (요청 파라미터 · 응답 모양)
│   │   │   │   ├── service/     ChzzkClient(호출·파싱) · LiveFeedService(캐시)
│   │   │   │   ├── filter/      인증 · CSRF · 보안 헤더 · 캐시 재검증
│   │   │   │   ├── interceptor/ 관리자 인증
│   │   │   │   └── util/        비밀번호 · CSRF 토큰 · 로그인 시도 제한 · 조회 기간
│   │   │   ├── schedule/      방송 일정 — controller / service / mapper
│   │   │   └── admin/         관리자 — 인증, 일정 관리
│   │   ├── resources/
│   │   │   ├── logback.xml    로그 설정 (클래스패스 최상단이어야 logback 이 찾는다)
│   │   │   ├── spring/        Spring 설정
│   │   │   ├── mybatis/       MyBatis 설정
│   │   │   ├── properties/    DB 접속 정보 (로컬)
│   │   │   └── sql/           매퍼 SQL — MyBatis 가 읽는 *_SQL.xml 만 둔다
│   │   ├── resources-prod/    DB 접속 정보 (운영) — mvn -Pprod 전용
│   │   └── webapp/
│   │       ├── META-INF/      톰캣 쿠키 처리기 (SameSite)
│   │       ├── WEB-INF/
│   │       │   ├── web.xml
│   │       │   └── views/     페이지 JSP — 홈, 일정, 프로필, 관리자
│   │       │       └── common/  상단바 · 바닥글 · 테마 토글 · head 공통 조각
│   │       └── resources/     css · js · images
│   └── test/
│       ├── java/com/irion/
│       │   ├── testsupport/   FakeHttp — 필터·인터셉터 테스트가 함께 쓰는 서블릿 대역
│       │   └── (본 코드와 같은 패키지 구조)
│       │                      비밀번호 · 로그인 검증 · 시도 제한 · 인증 두 겹
│       │                      일정 저장·조회 · 치지직 파싱 · 페이지네이션
│       │                      입력 검증 · 조회 기간 · JSON 날짜 형식
│       │                      치지직 캐시 · 장애 폴백 · 백오프 · 커서 페이징
│       │                      보안 헤더 · CSP 지시자 · web.xml 매핑
│       └── resources/         logback-test.xml — 테스트는 파일 로그를 남기지 않는다
├── .github/
│   ├── workflows/test.yml 푸시·PR 마다 mvn test
│   └── dependabot.yml     주간 의존성 감시
├── scripts/
│   ├── run-local.sh       로컬 빌드 · 배포 · 기동 확인 (맥)
│   ├── deploy.sh          테스트 · 빌드 · 전송 · 배포 · 롤백 (맥 → 서버)
│   └── db-backup.sh       DB 백업 (서버에서 cron 으로)
├── docs/
│   ├── db/schema.sql      최초 1회 실행하는 DDL — 실행 자원이 아니라 war 에 넣지 않는다
│   └── screenshots/       README 용 화면 캡처
├── deploy.env.example     배포 대상 서버 설정 예시 (실제 값은 deploy.env)
└── pom.xml
```

<br>

## 트러블슈팅

#### 일정 시간이 9시간씩 밀려 저장되는 문제

관리자에서 오전 8시로 등록한 일정이 DB에는 오후 5시로 저장되었습니다.
입력한 모든 시각이 정확히 9시간씩 어긋났습니다.

원인은 타임존 변환의 중복이었습니다.
프론트엔드가 오프셋(KST +9h)을 더해 UTC 형식으로 보냈고,
백엔드 `ScheduleVO`가 그 값을 다시 UTC 기준으로 해석하면서
KST 시각에 9시간이 한 번 더 더해진 것입니다.

변환 지점을 하나로 모았습니다.
프론트는 `datetime-local` 입력값을 변환 없이 그대로 보내고,
백엔드의 `@JsonFormat`을 `Asia/Seoul` 기준으로 해석하도록 변경했습니다.
입력·저장·표시 전 구간이 KST로 일관되게 맞춰졌습니다.

#### 직접 만든 JSON 파서가 제목을 잘라먹는 문제

인기 클립의 썸네일 자리에 이미지 대신 제목이 나오거나,
제목이 중간에서 끊겨 표시되었습니다.

chzzk 응답을 `indexOf` 로 잘라 읽는 파서를 직접 두고 있었습니다.
문자열의 끝을 다음 `"` 로 찾는 방식이라 JSON 이스케이프를 모릅니다.

```
입력   {"clipTitle":"이리온 \"레전드\" 순간"}
결과   이리온 \
```

괄호 매칭에 escape 판정을 덧대며 한동안 버텼지만,
`\n` · `\\` · `\/` 가 디코드되지 않는 등 같은 종류의 문제가 계속 나왔습니다.
JSON 을 직접 파싱하려 한 것 자체가 원인이었습니다.

이미 의존성에 있던 Jackson 으로 교체하고 손으로 만든 파서 120줄을 걷어냈습니다.
같은 작업에서 치지직 호출·파싱을 `ChzzkClient` 로, 캐시를 `LiveFeedService` 로
분리해 컨트롤러에는 HTTP 처리만 남겼습니다 (602줄 → 92줄).
회귀를 막으려고 위 입력을 그대로 테스트로 고정해두었습니다.

#### 페이지 스크롤이 중간에 멈추는 문제

리뉴얼 직후 일부 페이지에서 본문 중간까지만 스크롤되고 더 내려가지 않았습니다.

`body`에 가로 스크롤을 막으려고 `overflow-x: hidden`을 지정했는데,
CSS 명세상 한 축에 `hidden`을 주면 다른 축의 `overflow`가 자동으로 `auto`로 승격됩니다.
그 결과 `body`가 별도 스크롤 컨테이너가 되어 문서 스크롤과 충돌했습니다.

`overflow-x: hidden`을 `body`에서 `html`로 옮겨,
`body`는 일반 문서 흐름을 유지하도록 했습니다.

#### 배포 직후 사이트 전체가 응답하지 않는 문제

새 war를 올린 뒤 정적 페이지는 열리는데 DB를 읽는 요청마다 실패했습니다.
데이터가 지워진 것처럼 보였지만, 실제로는 연결 대상이 잘못돼 있었습니다.

`database.properties` 는 classpath 리소스라 빌드 시 war 안에 함께 들어갑니다.
로컬 개발 설정이 담긴 채로 패키징된 war가 배포되면서,
운영 서버가 존재하지 않는 로컬 주소로 접속을 시도한 것입니다.

빌드 프로파일을 분리해 운영 설정을 별도 디렉터리에서 주입하도록 했습니다.
설정이 비어 있거나 주소가 로컬이면 빌드 자체가 실패하므로,
잘못된 war가 만들어지지 않습니다.

#### 500 오류가 404 빈 화면으로 표시되는 문제

위 장애를 추적할 때 원인 파악이 늦어진 이유입니다.
`web.xml` 이 오류 페이지를 `/WEB-INF/views/common/500.jsp` 로 지정하고 있었는데
해당 파일이 없었습니다.

500이 발생하면 없는 페이지로 포워딩되고, 그 과정에서 404가 반환되며
본문이 비어 있어 원래 오류가 완전히 가려집니다.

404 · 500 페이지를 실제로 만들어 채웠습니다.

#### 자바 8 서버에서 모든 페이지가 500 이 되는 문제

war 는 정상이고 API 도 응답하는데, 브라우저로 여는 페이지마다 500 이 났습니다.

JSP 는 미리 컴파일되지 않고 첫 요청 때 톰캣이 컴파일합니다.
톰캣 9 가 그 일에 쓰는 ECJ 가 자바 11 이상을 요구해서,
자바 8 서버에서는 JSP 가 하나도 컴파일되지 않았습니다.
`/schedule/list` 같은 JSON API 는 JSP 를 거치지 않아 멀쩡히 200 을 돌려줬고,
그래서 헬스체크는 통과하는데 사이트는 죽어 있는 상태가 됐습니다.

배포 스크립트에 두 가지를 넣었습니다.
전송 전에 서버 자바 버전을 읽어 11 미만이면 배포를 중단하고,
헬스체크는 API 하나가 아니라 실제 페이지(`/` · `/schedule` · `/info` · `/admin/admin-login`)까지
함께 확인합니다. 하나라도 200 이 아니면 직전 백업으로 롤백합니다.

#### 수정한 CSS·JS가 반영되지 않는 문제

파일을 고치고 새로고침해도 예전 화면이 그대로 보였습니다.
강제 새로고침(`⌘⇧R`)을 해야만 반영되었습니다.

톰캣이 정적 파일에 `Last-Modified` 만 보내고 `Cache-Control` 을 보내지 않아,
브라우저가 자체 휴리스틱으로 캐시 유효기간을 정한 탓입니다.
배포 후 기존 방문자가 새 HTML과 예전 CSS를 섞어 받는 문제로도 이어집니다.

`StaticResourceCacheFilter` 를 추가해 `.html` · `.css` · `.js` 에
`Cache-Control: no-cache` 를 지정했습니다.
변경이 없으면 304만 오가므로 대역폭 부담은 거의 없습니다.

확장자로만 거르면 `/schedule` 처럼 확장자 없는 페이지 주소가 빠져나가
이 필터가 무의미해집니다. 마지막 `/` 뒤에 `.` 이 없으면 페이지 주소로 봅니다.

#### 404·500 페이지에만 보안 헤더가 붙지 않던 문제

응답 헤더를 훑어보다 발견했습니다. 모든 페이지에 CSP·`X-Frame-Options`·HSTS 가
붙는데 오류 페이지 둘만 하나도 없었습니다. 필터 코드에는 조건 분기가 없었고
`web.xml` 의 매핑도 `/*` 라 안 걸릴 이유가 없어 보였습니다.

서블릿 명세 때문입니다. `filter-mapping` 에 `<dispatcher>` 를 적지 않으면
기본값은 `REQUEST` **하나뿐**입니다. `<error-page>` 로 넘어가는 응답은
`ERROR` 디스패치라서 필터 체인을 다시 타지 않습니다.
매핑이 `/*` 든 아니든 상관이 없었던 것입니다.

하필 `500.jsp` 는 바깥에서 온 주소(`javax.servlet.error.request_uri`)를
"다시 시도" 링크로 되심는 페이지입니다. 그쪽에서도 `//` 를 막고 이스케이프하지만,
겹겹이 두는 것이 이 필터의 존재 이유입니다.

```xml
<filter-mapping>
  <filter-name>staticResourceCacheFilter</filter-name>
  <url-pattern>/*</url-pattern>
  <dispatcher>REQUEST</dispatcher>   <!-- 적는 순간 기본값이 사라진다 -->
  <dispatcher>ERROR</dispatcher>
</filter-mapping>
```

`REQUEST` 를 같이 적는 것이 중요합니다. `ERROR` 만 적으면 정반대로
**일반 요청에서** 헤더가 사라집니다.

설정 파일의 문제라 코드로는 드러나지 않습니다.
`StaticResourceCacheFilterTest` 가 `web.xml` 을 직접 읽어 두 선언이 남아 있는지 확인합니다
(`servlet-context.xml` 과 VO 를 대조하는 `ScheduleVOJsonFormatTest` 와 같은 방식입니다).

#### 다시보기는 임베드되지 않는 문제

클립과 같은 방식으로 다시보기도 사이트 안에서 재생하려 했으나 되지 않았습니다.
`/embed/video/{videoNo}` 를 `iframe` 에 넣으면 플레이어 대신
치지직의 "존재하지 않는 채널입니다" 화면이 나옵니다.

치지직 프론트엔드 번들의 라우터를 확인해보니
임베드 경로는 `/embed/clip/:clipUID` 와 `/embed/clip-donation/:clipUID` 둘뿐입니다.
VOD용 경로 자체가 없어서, 라우터에 걸리지 않고 SPA의 404로 떨어진 것입니다.
`X-Frame-Options` 나 CSP `frame-ancestors` 로 막는 방식이 아니라
(치지직은 이 헤더들을 아예 보내지 않습니다) 경로가 존재하지 않는 쪽입니다.

치지직이 임베드를 열어둔 범위가 클립까지라고 보고,
다시보기는 기존의 링크 이동을 유지했습니다.

#### 19금 방송·다시보기의 썸네일이 깨지는 문제

연령 제한이 걸린 다시보기 카드와 방송 중 히어로에서
썸네일 자리에 깨진 이미지 아이콘이 떴습니다.

치지직 응답을 직접 받아보니 그 항목만 `"adult": true` 이면서
`thumbnailImageUrl` 이 `null` 입니다 (생방송은 `liveImageUrl` 이 `null`).
로그인 쿠키 없이 부르는 공개 API 라 이미지 주소를 아예 내려주지 않습니다.

`ChzzkClient.text()` 는 없는 값을 빈 문자열로 바꾸므로
화면에는 `<img src="">` 가 그려집니다. 빈 `src` 는 "이미지 없음" 이 아니라
현재 페이지 주소로 해석되어, 브라우저가 HTML 을 이미지로 받아보려다 실패합니다.
깨진 아이콘은 그 실패의 표시였습니다.

주소를 되찾을 방법은 없으니 (성인 인증된 세션이라야 내려옵니다)
없는 것을 없다고 보여주기로 했습니다. `adult` 플래그를 화면까지 내려보내고,
주소가 없으면 `img` 자체를 만들지 않은 채 `.thumb-fallback` 자리를 그립니다.
19금이면 배지로 이유까지 밝힙니다.

#### 관리자 인증 필터가 주소의 세미콜론 하나로 열리던 문제

`/admin;x=1/schedule` 로 요청하면 `AdminLoginFilter` 가 로그인 검사를
건너뛰고 그대로 통과시켰습니다. `/admin` 정확히 일치도 마찬가지였습니다.

톰캣은 필터를 고를 때 경로 파라미터(`;` 부터 다음 `/` 까지)를 떼고
`/admin/schedule` 로 봅니다. 스프링도 `UrlPathHelper` 가 같은 일을 해서
관리자 컨트롤러로 보냅니다. 그런데 `getRequestURI()` 만 `;x=1` 을 그대로
들고 있어서, `RequestUtil.normalizedPath()` 의 결과가 `/admin/` 으로
시작하지 않는 것처럼 보였습니다. 조건이 거짓이 되니 검사 자체를 건너뜁니다.

`..` · `%2F` · `\` · `//` 는 이미 정규화하고 테스트도 있었는데,
경로 파라미터만 빠져 있었습니다.

실제로 뚫리지는 않았습니다. `AdminLoginInterceptor` 가 경로와 무관하게
세션을 확인하고, 그쪽 `/admin/**` 매핑은 스프링이 떼어낸 경로를 쓰기
때문에 거기서 막힙니다. 그래도 일부러 두 겹으로 둔 방어 중 한 겹이
죽어 있는 상태였습니다 — 나중에 인터셉터를 중복이라 여겨 걷어내거나,
인터셉터가 닿지 않는 관리자 주소가 하나 생기면 그때 진짜로 뚫립니다.

`normalizedPath()` 가 세그먼트마다 경로 파라미터를 떼도록 고쳤습니다.
**떼는 시점은 디코딩보다 앞입니다** — 톰캣이 그 순서라서, 뒤에 떼면
`%3B` 로 보낸 진짜 세미콜론까지 잘려 이번엔 반대 방향으로 어긋납니다.
`/admin` 정확히 일치도 관리자 영역으로 함께 묶었습니다.

#### 클립 더보기가 20개에서 멈추는 문제

더보기를 눌러도 클립이 스무 개를 넘지 않았습니다.
코드는 열 페이지를 돌며 최대 100개를 모으도록 짜여 있었는데도 그랬습니다.

chzzk 의 클립 페이징은 offset 이 아니라 커서입니다.
응답의 `page.next` 에 `clipUID` 와 `readCount` 가 함께 담겨 오고,
**두 값을 같은 이름의 파라미터로 되돌려줘야** 다음 묶음이 옵니다.
기존 구현은 `next` 라는 이름으로 `clipUID` 만 보냈습니다.
chzzk 은 모르는 파라미터를 조용히 무시하고 1페이지를 다시 줬고,
중복을 걸러내고 나면 늘 스무 개였습니다.
열 번 호출해 같은 스무 개를 열 번 받고 있었던 셈입니다.

커서를 `clipUID` + `readCount` 로 바로잡고, 미리 다 받는 대신
더보기가 요구하는 만큼만 이어 받아 캐시 뒤에 붙이도록 바꿨습니다.
목록이 자랄 때 적재 시각을 새로 찍어, 한참 넘겨보는 중에 TTL 이
끝나 목록이 처음부터 다시 쌓이는 일도 막았습니다.
메모리 상한과 요청당 외부 호출 횟수, 커서가 돌지 않을 때의 중단 조건을 함께 뒀습니다.

같은 자리에서 `paginate()` 의 경계 버그도 고쳤습니다.
`offset` 이 목록 길이를 넘으면 `subList` 가 예외를 던져
`/live/clips?offset=99999` 같은 요청이 500 이 됩니다.

정리된 흐름은 다음과 같습니다.
외부 호출은 락 밖에서 하고, 합치는 순간에만 락을 잡습니다.
한 스레드가 최대 10회 × 5초 동안 락을 쥔 채 나머지 요청을 세워두는 일을 막기 위해서입니다.

```mermaid
flowchart TD
    Q(["GET /live/clips"]) --> CL["limit 을 1~50 으로 클램프"]
    CL --> C1{"캐시가 신선한가\nTTL 10분"}
    C1 -->|예| FD["ClipFeed"]
    C1 -->|아니오| LD["첫 2페이지 적재"]
    LD --> FD
    FD --> C2{"offset+limit 만큼\n모였는가"}
    C2 -->|예| PG["paginate → 응답"]
    C2 -->|아니오| C3{"다른 요청이\n이미 확장 중인가"}
    C3 -->|예| PG
    C3 -->|아니오| GT["락 밖에서 이어 받기\n커서 = clipUID + readCount"]
    GT --> MG["락 안에서 병합 · 캐시 교체"]
    MG --> PG
```

#### 치지직이 멈추면 요청마다 타임아웃을 다시 기다리는 문제

치지직이 응답하지 않는 동안 사이트 전체가 느려졌습니다.
캐시는 만료된 값으로 물러나도록 돼 있어서 화면이 죽지는 않았는데,
응답이 오기까지 몇 초씩 걸렸습니다.

`LiveFeedService.cached()` 가 **실패를 기억하지 않는 것**이 원인이었습니다.
성공하면 값과 적재 시각을 남기지만, 실패하면 만료된 값을 돌려주고 아무것도 적지 않습니다.
그래서 다음 요청은 여전히 "만료됨" 상태를 보고 락을 잡은 뒤
5초 타임아웃을 처음부터 다시 기다립니다. 요청이 몰리면 그 5초가 줄줄이 직렬로 쌓입니다.

`Snapshot` 에 `failedAt` 을 더해 값·적재 시각·실패 시각을 한 객체에 담았습니다.
"값과 시각을 따로 두면 새 값 + 옛 시각 조합이 보인다"는 이유가 실패 시각에도 그대로 적용됩니다.

**락 안에서도 백오프를 보는 것이 핵심입니다.** 이미 락 앞에 줄 서 있던 스레드들이
첫 스레드의 실패를 보고 그 자리에서 물러나야 합니다. 락 밖에서만 확인하면
줄 서 있던 것들은 그대로 통과해 각자 타임아웃을 기다립니다.

값은 버리지 않습니다. 버리면 백오프 30초 동안 화면이 빕니다.

같은 조건(동시 20건 · 타임아웃 대역 200ms)에서 3회씩 측정한 결과입니다.

| | 외부 호출 | 벽시계 시간 |
|---|---|---|
| 고치기 전 | 20회 | 4073 · 4063 · 4080 ms |
| 고친 뒤 | **1회** | **206 · 206 · 207 ms** |

이 클래스는 테스트가 없었기 때문에 백오프를 넣기 **전에** 먼저 붙였습니다.
TTL 이 1~10분이라 기다릴 수 없어, `Snapshot` 의 적재 시각만 과거로 돌리는
리플렉션 헬퍼로 만료를 만듭니다.

한 가지가 함께 바뀌었습니다. 치지직이 죽어 목록이 비면 예전에는 다음 요청이
곧바로 다시 시도했는데, 이제 30초 동안은 두드리지 않습니다.
**빈 목록이 TTL 10분 내내 굳지 않는다는 성질은 그대로입니다** — 기다리는 시간이 30초로 바뀐 것뿐입니다.

#### hidden 속성을 붙였는데 계속 보이는 문제

"이동 확인 다시 켜기" 링크를 `hidden` 으로 감췄는데도 화면에 남았습니다.
JS 로 속성을 붙이고 떼는 것은 정상이었습니다.

`hidden` 은 브라우저 기본 스타일시트의 `[hidden] { display: none }` 으로
동작합니다. 기본 스타일시트는 작성자 CSS 보다 우선순위가 낮아서,
그 요소에 `display` 를 지정하는 순간 숨김이 풀립니다.

`.aside-restore[hidden] { display: none; }` 로 명시적으로 다시 눌렀습니다.

#### 모달을 닫아도 클립 소리가 계속 나는 문제

클립 모달을 닫았는데 재생 중이던 소리가 멈추지 않았습니다.

`common.js` 의 공용 모달 로직은 배경 클릭과 ESC에 `.active` 클래스만 떼어냅니다.
화면에서 사라질 뿐 `iframe` 은 DOM에 그대로 남아 안쪽 플레이어가 계속 돕니다.
닫기 버튼만 따로 처리해서는 나머지 두 경로가 새어 나갑니다.

세 경로 모두에서 `iframe` 의 `src` 를 비우도록 했습니다.
`src` 를 지우면 문서 자체가 내려가므로 재생이 확실히 멈춥니다.

#### 모달 안에서 날짜 입력창이 밖으로 삐져나가는 문제

관리자 일정 등록 모달의 `datetime-local` 입력창이 모달 폭을 넘어
가로 스크롤이 생겼습니다. 입력창에 `width: 100%` 가 이미 지정돼 있었는데도
줄어들지 않았습니다.

입력창이 아니라 그리드 트랙이 원인이었습니다.
`datetime-local` 은 내부 UI 때문에 최소 폭이 크고,
`1fr` 트랙은 내용의 max-content 폭까지 늘어납니다.

`grid-template-columns` 를 `minmax(0, 1fr)` 로 바꿔 트랙을 묶었습니다.

#### 제목에 `'` 가 든 일정을 눌러도 아무 반응이 없는 문제

"다가오는 일정" 카드 중 일부만 클릭이 먹지 않았습니다.
콘솔에는 `SyntaxError` 만 찍혔습니다.

카드를 인라인 `onclick` 으로 만들면서 값에 `escapeHtml()` 을 통과시킨 것이
원인이었습니다. `escapeHtml` 은 `'` 를 `&#039;` 로 바꾸지만,
브라우저는 속성값을 **HTML 디코드한 뒤에** JS 로 파싱합니다.
그 시점에 아포스트로피가 되살아나 문자열 리터럴이 끊깁니다.

```
생성한 HTML    onclick="show('1', '이리온&#039;s 첫 합방')"
JS 가 보는 것   show('1', '이리온's 첫 합방')     ← SyntaxError
```

반대편도 문제였습니다. 상세 모달은 이미 이스케이프된 값을 다시 이스케이프하지
않고 `.html()` 에 넣어서, `<` 가 든 제목이 태그로 해석됐습니다.
한쪽은 너무 많이, 다른 쪽은 너무 적게 이스케이프하고 있었던 셈입니다.

인라인 핸들러를 없애고, 원본 객체는 배열에 둔 채 인덱스만 `data-*` 로 넘기도록
바꿨습니다. 이스케이프는 화면에 글자를 찍는 그 한 곳에서만 합니다.
이 정리 덕분에 CSP 의 `script-src` 에서 `'unsafe-inline'` 도 뺄 수 있었습니다.

#### 세션이 끊기면 관리자 캘린더가 에러도 없이 비는 문제

로그인이 만료된 뒤 관리자 페이지를 열면 일정이 하나도 없는 것처럼 보였습니다.
로그인 화면으로 넘어가지도, 오류를 띄우지도 않았습니다.

인증을 필터와 인터셉터 두 곳에서 보고 있었는데,
"AJAX 면 401" 분기는 인터셉터에만 있었습니다.
필터는 `DispatcherServlet` 앞에서 돌기 때문에 요청이 인터셉터까지 닿지 못하고
그 자리에서 로그인 페이지로 리다이렉트됩니다.

jQuery 는 302 를 그대로 따라가 로그인 HTML 을 200 으로 받습니다.
JSON 파싱에 실패해 `error` 콜백으로 오지만, 그때 `xhr.status` 는 200 이라
`401` 분기에 걸리지 않고 조용히 빈 배열로 끝납니다.
실패가 성공처럼 보이는 경로였습니다.

필터에도 같은 판정을 넣어 AJAX 요청에는 401 JSON 을 돌려주도록 했습니다.
판정 로직은 `RequestUtil` 한 곳에 모아 두 곳이 어긋나지 않게 했습니다.

<br>

## 라이선스

소스 코드는 [MIT License](LICENSE) 하에 배포됩니다.

방송 콘텐츠의 저작권은 원 저작자(이리온)에게 있으며,
chzzk, YouTube, X 등의 로고와 브랜드는 각 사의 상표입니다.

<br>

---

<br>

<div align="center">

**sooindev**

[github.com/sooindev](https://github.com/sooindev)

<br>
<br>

</div>
