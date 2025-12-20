# YETI-125 🎮

치지직 스트리머 이리온(Irion)의 비공식 팬사이트입니다.

> ⚠️ **비상업적 프로젝트**
> 이 프로젝트는 순수한 팬 활동의 일환으로 제작된 비상업적 웹사이트입니다.
> 어떠한 상업적 목적이나 수익 창출을 의도하지 않으며, 오직 정보 제공 및 아카이빙 목적으로 운영됩니다.

## 📋 주요 기능

### 🏠 홈
- 실시간 방송 상태 확인
- 최근 클립 및 동영상
- 다가오는 방송 일정
- 커뮤니티 링크 (chzzk, YouTube, Discord)

### 📅 방송 일정
- 월간 캘린더 뷰로 방송 일정 확인
- 일정 유형별 색상 구분
  - 🟢 저스트채팅
  - 🟣 종합게임
  - 🟡 노래방송
  - 🩷 합방
- 다가오는 일정 미리보기
- 일정 상세 정보 모달

### 👤 프로필
- 스트리머 소개
- 방송 스타일 및 특징
- 팬아트 갤러리

### 🔧 관리자
- 방송 일정 관리 (CRUD)
- 실시간 일정 추가/수정/삭제
- chzzk API 연동으로 자동 방송 상태 확인

## 🛠 기술 스택

### Backend
- **Java 8**
- **Spring Framework 5.3.x**
- **Spring MVC**
- **MyBatis**
- **MySQL**
- **Maven**

### Frontend
- **HTML5 / CSS3**
- **JavaScript (ES6+)**
- **jQuery 3.7.1**
- **FullCalendar 6.1.10**

### API
- **chzzk API** - 실시간 방송 상태 확인

### 디자인
- 반응형 웹 디자인 (모바일 최적화)
- 모던 UI/UX with 글래스모피즘

## 🚀 설치 및 실행

### 사전 요구사항
- JDK 8 이상
- Maven 3.6+
- MySQL 8.0+
- Tomcat 9.0+

### 1. 저장소 클론
```bash
git clone https://github.com/SooinDev/YETI-125.git
cd YETI-125
```

### 2. 데이터베이스 설정
```sql
CREATE DATABASE for_125;
USE for_125;

-- 테이블 생성 스크립트 실행
-- (schema.sql 파일 참조)
```

### 3. 설정 파일 수정
`src/main/resources/application.properties` 파일에서 데이터베이스 연결 정보를 수정하세요:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/for_125
spring.datasource.username=your_username
spring.datasource.password=your_password
```

### 4. 빌드 및 실행
```bash
# Maven 빌드
mvn clean package

# Tomcat에 배포
cp target/yeti-125.war $TOMCAT_HOME/webapps/

# Tomcat 시작
$TOMCAT_HOME/bin/startup.sh
```

또는 Maven Tomcat 플러그인 사용:
```bash
mvn tomcat7:run
```

### 6. 접속
브라우저에서 `http://localhost:8080` 접속

## 📱 모바일 최적화

- 터치 디바이스 감지 및 최적화
- 모바일에서 무거운 애니메이션 제거
- 반응형 그리드 레이아웃
- 터치 제스처 지원

## 🎨 주요 디자인 특징

- **글래스모피즘** 디자인
- **부드러운 애니메이션** (모바일에서는 단순화)
- **접근성** 고려한 색상 대비
- **모던한 UI 컴포넌트**

## 📂 프로젝트 구조

```
YETI-125/
├── src/main/
│   ├── java/
│   │   └── com/irion/
│   │       ├── common/         # 공통 유틸리티
│   │       ├── schedule/       # 일정 관리
│   │       ├── admin/          # 관리자 기능
│   │       └── live/           # 실시간 방송 상태
│   ├── resources/
│   │   └── application.properties
│   └── webapp/
│       ├── resources/
│       │   ├── css/           # 스타일시트
│       │   ├── js/            # JavaScript
│       │   └── images/        # 이미지
│       ├── WEB-INF/
│       │   ├── spring/        # Spring 설정
│       │   └── views/         # JSP 뷰
│       └── *.html             # 정적 페이지
└── pom.xml
```

## 🔐 관리자 기능

관리자 페이지 접속: `/admin/admin-login.html`

**주요 기능:**
- 방송 일정 추가/수정/삭제
- 일정 유형 설정 (저스트채팅, 종합게임, 노래방송, 합방)
- 일정 색상 커스터마이징
- 드래그 앤 드롭으로 일정 이동

## 🤝 기여

이 프로젝트는 개인 학습 및 팬 활동 목적으로 제작되었습니다.
버그 제보나 개선 사항은 [Issues](https://github.com/yourusername/YETI-125/issues)에 등록해주세요.

## 📜 라이선스 및 저작권

### 저작권 고지
- 모든 방송 콘텐츠의 저작권은 원 저작자(이리온)에게 있습니다.
- 이 프로젝트는 비공식 팬사이트이며, 원 저작자와 직접적인 관련이 없습니다.
- chzzk, YouTube, Discord 등의 로고 및 브랜드는 각 회사의 상표입니다.

### 프로젝트 라이선스
이 프로젝트의 코드는 MIT 라이선스 하에 배포됩니다.

## 👨‍💻 개발자

**SooinDev**
- GitHub: [@SooinDev](https://github.com/SooinDev)

---

⭐ 이 프로젝트가 도움이 되었다면 Star를 눌러주세요!
