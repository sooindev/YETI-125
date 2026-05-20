<div align="center">

# ❄️ YETI-125

**치지직 스트리머 이리온(IRION)의 비공식 팬사이트**

라이브 상태 · 방송 일정 · 클립 아카이브를 한곳에서.

<br>

![Java](https://img.shields.io/badge/Java-8-007396?style=flat-square&logo=openjdk&logoColor=white)
![Spring](https://img.shields.io/badge/Spring-5.3-6DB33F?style=flat-square&logo=spring&logoColor=white)
![MyBatis](https://img.shields.io/badge/MyBatis-3.x-DC382D?style=flat-square)
![MariaDB](https://img.shields.io/badge/MariaDB-10-003545?style=flat-square&logo=mariadb&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-4ea8d8?style=flat-square)

</div>

<br>

> [!NOTE]
> **비상업적 팬 프로젝트** — 순수한 팬 활동의 일환으로 제작된 비상업적 웹사이트입니다.
> 어떠한 상업적 목적이나 수익 창출도 의도하지 않으며, 정보 제공 및 아카이빙 목적으로만 운영됩니다.
> 원 저작자(이리온)와 직접적인 관련이 없습니다.

<br>

## 목차

- [소개](#소개)
- [주요 기능](#주요-기능)
- [기술 스택](#기술-스택)
- [디자인](#디자인)
- [시작하기](#시작하기)
- [프로젝트 구조](#프로젝트-구조)
- [라이선스](#라이선스)

<br>

## 소개

**YETI-125** 는 치지직 스트리머 이리온의 활동을 한곳에 모아 보여주는 팬 아카이브입니다.
chzzk API와 연동해 실시간 방송 상태를 보여주고, 인기 클립·다시보기를 자동으로 수집하며,
관리자가 직접 등록한 방송 일정을 캘린더로 제공합니다.

<br>

## 주요 기능

### 🏠 홈
- chzzk API 연동 **실시간 방송 상태** (LIVE / OFFLINE)
- **인기 클립** 자동 수집 — 조회수순, 더보기 페이지네이션
- **다시보기(VOD)** 목록 — 최신순
- 채널 & 링크 모음 (chzzk · YouTube · X · 네이버 카페)

### 📅 방송 일정
- **월간 캘린더** 뷰 ([FullCalendar](https://fullcalendar.io/) 기반)
- 일정 유형별 색상 구분
  - 🟢 저스트 채팅 &nbsp; 🔵 종합게임 &nbsp; 🟡 노래방송 &nbsp; 🩷 합방
- 다가오는 일정 미리보기 · 상세 정보 모달

### 👤 프로필
- 스트리머 소개 및 캐릭터 명세
- 데뷔 / 생일 **D-Day 카운터**
- 채널 & SNS 링크

### 🔧 관리자
- 방송 일정 **등록 · 수정 · 삭제** (CRUD)
- 캘린더에서 **드래그 앤 드롭**으로 일정 이동
- 일정 유형 · 색상 · 노출 여부 설정
- 로그인 인증 (필터 + 인터셉터)

<br>

## 기술 스택

| 영역 | 사용 기술 |
|------|-----------|
| **Backend** | Java 8, Spring Framework 5.3 (Spring MVC), MyBatis |
| **Database** | MariaDB, HikariCP (커넥션 풀) |
| **Frontend** | HTML5, CSS3, JavaScript (ES6+), jQuery 3.7 |
| **Library** | FullCalendar 6.1 |
| **External API** | [chzzk API](https://chzzk.naver.com/) — 방송 상태 · 클립 · 다시보기 |
| **Build / Server** | Maven, Apache Tomcat 9 |

<br>

## 디자인

에디토리얼 감성의 **파스텔 라이트 테마**.

- 🎨 **하늘색 메인 + 분홍 포인트** 파스텔 팔레트
- 🔤 **Anton**(디스플레이) × **JetBrains Mono**(본문) 타이포그래피 대비
- 🌫️ 화면 전체 **그레인 노이즈** 텍스처 오버레이
- 📐 **비대칭 그리드** 레이아웃 · 균일 3열 미디어 카드
- 📱 모바일까지 고려한 **반응형** 디자인

<br>

## 시작하기

### 사전 요구사항

- JDK **8** 이상
- Maven **3.6+**
- MariaDB **10+**
- Apache Tomcat **9+**

### 1. 저장소 클론

```bash
git clone https://github.com/sooindev/YETI-125.git
cd YETI-125
```

### 2. 데이터베이스 준비

`src/main/resources/sql/schema.sql` 을 실행해 데이터베이스와 테이블을 생성합니다.

```bash
mysql -u root -p < src/main/resources/sql/schema.sql
```

> `for_125` 데이터베이스와 `tb_admin`, `tb_schedule` 테이블이 생성됩니다.

### 3. DB 접속 정보 설정

`src/main/resources/properties/database.properties` 에서 계정 정보를 수정합니다.

```properties
db.driver=org.mariadb.jdbc.Driver
db.url=jdbc:mariadb://localhost:3306/for_125?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Seoul
DB_USERNAME=your_username
DB_PASSWORD=your_password
```

### 4. 빌드 & 실행

```bash
# WAR 빌드
mvn clean package

# Tomcat에 배포
cp target/*.war $TOMCAT_HOME/webapps/
$TOMCAT_HOME/bin/startup.sh
```

### 5. 접속

| 페이지 | 주소 |
|--------|------|
| 메인 사이트 | `http://localhost:8080` |
| 관리자 | `http://localhost:8080/admin/admin-login.html` |

<br>

## 프로젝트 구조

```
YETI-125/
├── src/main/
│   ├── java/com/irion/
│   │   ├── common/        # 공통 — 라이브 상태, 필터, 인터셉터, 유틸
│   │   ├── schedule/      # 방송 일정 — controller / service / mapper / vo
│   │   └── admin/         # 관리자 — 인증, 일정 관리
│   ├── resources/
│   │   ├── spring/        # Spring 설정 (root / servlet context)
│   │   ├── mybatis/       # MyBatis 설정
│   │   ├── properties/    # DB 접속 정보
│   │   └── sql/           # 스키마 · 매퍼 SQL
│   └── webapp/
│       ├── resources/     # css · js · images
│       ├── WEB-INF/       # web.xml
│       └── *.html         # 정적 페이지 (홈 / 일정 / 프로필 / 관리자)
└── pom.xml
```

<br>

## 라이선스

이 프로젝트의 **소스 코드**는 [MIT License](LICENSE) 하에 배포됩니다.

> [!IMPORTANT]
> - 모든 방송 콘텐츠의 저작권은 원 저작자(**이리온**)에게 있습니다.
> - 본 사이트는 **비공식 팬사이트**이며 원 저작자와 직접적인 관련이 없습니다.
> - chzzk · YouTube · X 등의 로고와 브랜드는 각 사의 상표입니다.

<br>

## 개발자

**sooindev** &nbsp;·&nbsp; [GitHub](https://github.com/sooindev)

<br>

<div align="center">

⭐ 이 프로젝트가 마음에 드셨다면 Star를 눌러주세요!

<sub>Made with ❄️ for IRION</sub>

</div>
