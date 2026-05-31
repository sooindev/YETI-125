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

## 소개

YETI-125는 치지직 스트리머 이리온의 활동을 한곳에 모아 보여주는 팬 아카이브입니다.

chzzk API와 연동해 실시간 방송 상태를 보여주고,
인기 클립과 다시보기를 자동으로 수집하며,
관리자가 직접 등록한 방송 일정을 캘린더로 제공합니다.

> 순수한 팬 활동의 일환으로 제작된 비상업적 프로젝트입니다.
> 원 저작자와 직접적인 관련이 없으며, 정보 제공 및 아카이빙 목적으로만 운영됩니다.

<br>

## 기능

### 홈

실시간 방송 상태(LIVE / OFFLINE), 인기 클립과 다시보기,
채널과 SNS 링크를 한 화면에 정리합니다.

### 방송 일정

월간 캘린더 뷰로 방송 일정을 확인합니다.
저스트 채팅, 종합게임, 노래방송, 합방 — 유형별로 색을 달리해 한눈에 구분됩니다.

### 프로필

캐릭터 설정, 제작 크레딧, 데뷔일과 생일 D-Day,
채널과 SNS 링크를 정돈된 형태로 제공합니다.

### 관리자

방송 일정을 등록·수정·삭제하고, 캘린더에서 드래그로 옮길 수 있습니다.
필터와 인터셉터로 인증을 처리합니다.

<br>

## 기술 스택

| 영역 | |
|------|---|
| 백엔드 | Java 8 · Spring Framework 5.3 · MyBatis |
| 데이터베이스 | MariaDB · HikariCP |
| 프런트엔드 | HTML5 · CSS3 · JavaScript · jQuery 3.7 |
| 라이브러리 | FullCalendar 6.1 |
| 외부 API | [chzzk API](https://chzzk.naver.com/) |
| 빌드 / 서버 | Maven · Apache Tomcat 9 |

<br>

## 디자인

파스텔 라이트 테마.
하늘색을 메인으로, 분홍을 포인트로 사용합니다.

타이포그래피는 디스플레이의 Anton과 본문의 JetBrains Mono를 대비시켜
정보의 위계를 만들었습니다.
화면 전체에 옅은 그레인 텍스처를 더해 무게감을 주고,
미디어 카드는 균일한 3열로 정렬해 가독성에 집중했습니다.

<br>

## 시작하기

JDK 8 이상, Maven 3.6 이상, MariaDB 10 이상, Apache Tomcat 9 이상이 필요합니다.

#### 1. 저장소 클론

```bash
git clone https://github.com/sooindev/YETI-125.git
cd YETI-125
```

#### 2. 데이터베이스 준비

`src/main/resources/sql/schema.sql` 을 실행해 `for_125` 데이터베이스와
`tb_admin`, `tb_schedule` 테이블을 생성합니다.

```bash
mysql -u root -p < src/main/resources/sql/schema.sql
```

#### 3. 접속 정보 설정

`src/main/resources/properties/database.properties` 에서 계정 정보를 수정합니다.

```properties
db.driver=org.mariadb.jdbc.Driver
db.url=jdbc:mariadb://localhost:3306/for_125?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Seoul
DB_USERNAME=your_username
DB_PASSWORD=your_password
```

#### 4. 빌드와 실행

```bash
mvn clean package
cp target/*.war $TOMCAT_HOME/webapps/
$TOMCAT_HOME/bin/startup.sh
```

#### 5. 접속

| | |
|---|---|
| 메인 사이트 | `http://localhost:8080` |
| 관리자 | `http://localhost:8080/admin/admin-login.html` |

<br>

## 프로젝트 구조

```
YETI-125/
├── src/main/
│   ├── java/com/irion/
│   │   ├── common/        라이브 상태, 필터, 인터셉터, 유틸
│   │   ├── schedule/      방송 일정 — controller / service / mapper
│   │   └── admin/         관리자 — 인증, 일정 관리
│   ├── resources/
│   │   ├── spring/        Spring 설정
│   │   ├── mybatis/       MyBatis 설정
│   │   ├── properties/    DB 접속 정보
│   │   └── sql/           스키마와 매퍼 SQL
│   └── webapp/
│       ├── resources/     css · js · images
│       └── *.html         홈, 일정, 프로필, 관리자
└── pom.xml
```

<br>

## 트러블슈팅

#### 일정 시간이 9시간씩 밀려 저장되는 문제

관리자에서 오전 8시로 등록한 일정이 DB에는 오후 5시로 저장되었습니다.
입력한 모든 시각이 정확히 9시간씩 어긋났습니다.

원인은 타임존 변환의 중복이었습니다.
프런트엔드가 오프셋(KST +9h)을 더해 UTC 형식으로 보냈고,
백엔드 `ScheduleVO`가 그 값을 다시 UTC 기준으로 해석하면서
KST 시각에 9시간이 한 번 더 더해진 것입니다.

변환 지점을 하나로 모았습니다.
프런트는 `datetime-local` 입력값을 변환 없이 그대로 보내고,
백엔드의 `@JsonFormat`을 `Asia/Seoul` 기준으로 해석하도록 변경했습니다.
입력·저장·표시 전 구간이 KST로 일관되게 맞춰졌습니다.

#### chzzk 클립 썸네일 JSON 파싱 오류

인기 클립의 썸네일 자리에 이미지 대신 클립 제목이 표시되었습니다.
깨진 `<img>`의 `alt` 텍스트가 노출된 것입니다.

chzzk API 응답의 클립 객체에는 값 자체가 escape된 JSON 문자열인 필드가 있습니다.
직접 구현한 JSON 파서의 괄호 매칭 로직이 escape된 따옴표를 제대로 구분하지 못해
객체 경계를 잘못 인식할 수 있었습니다.

괄호 매칭 함수에 escape 판정 로직을 추가했습니다.
해당 위치 앞의 연속된 백슬래시 개수가 홀수이면 escape된 문자로 판정하도록 하여
파서의 견고성을 높였습니다.

#### 페이지 스크롤이 중간에 멈추는 문제

리뉴얼 직후 일부 페이지에서 본문 중간까지만 스크롤되고 더 내려가지 않았습니다.

`body`에 가로 스크롤을 막으려고 `overflow-x: hidden`을 지정했는데,
CSS 명세상 한 축에 `hidden`을 주면 다른 축의 `overflow`가 자동으로 `auto`로 승격됩니다.
그 결과 `body`가 별도 스크롤 컨테이너가 되어 문서 스크롤과 충돌했습니다.

`overflow-x: hidden`을 `body`에서 `html`로 옮겨,
`body`는 일반 문서 흐름을 유지하도록 했습니다.

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
