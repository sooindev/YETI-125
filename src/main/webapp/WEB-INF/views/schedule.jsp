<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, viewport-fit=cover">
  <title>이리온 방송 일정 — 치지직 버튜버 캘린더 | YETI-125</title>
  <meta name="description" content="치지직 버추얼 스트리머(버튜버) 이리온의 방송 일정을 월간 캘린더로 확인하세요. 저스트 채팅, 종합게임, 노래방송, 합방 일정을 유형별로 정리한 팬사이트입니다.">
  <meta name="keywords" content="이리온 일정, 이리온 방송일정, 치지직 방송 일정, 버튜버 일정, 치지직 팬사이트, 버튜버 팬사이트, 스트리머 팬사이트, YETI-125">
  <meta name="author" content="sooindev">
  <link rel="canonical" href="https://yeti-125.com/schedule">

  <!-- Open Graph -->
  <meta property="og:type" content="website">
  <meta property="og:site_name" content="YETI-125">
  <meta property="og:title" content="방송 일정 — YETI-125 이리온 팬사이트">
  <meta property="og:description" content="이리온의 방송 일정을 캘린더로 확인하세요. 유형별로 정리된 다가오는 일정과 지난 방송 기록.">
  <meta property="og:url" content="https://yeti-125.com/schedule">
  <meta property="og:image" content="https://yeti-125.com/resources/images/Irion-profile.jpg">
  <meta property="og:locale" content="ko_KR">

  <!-- Twitter / X -->
  <meta name="twitter:card" content="summary_large_image">
  <meta name="twitter:title" content="방송 일정 — YETI-125 이리온 팬사이트">
  <meta name="twitter:description" content="이리온의 방송 일정을 캘린더로 확인하세요.">
  <meta name="twitter:image" content="https://yeti-125.com/resources/images/Irion-profile.jpg">

<jsp:include page="/WEB-INF/views/common/head-assets.jsp"/>
  <!-- FullCalendar 6 은 CSS 를 JS 번들 안에서 주입한다. 별도 스타일시트가 없다. -->
  <!-- 구조화 데이터 (schema.org) -->
  <script type="application/ld+json">
  {
    "@context": "https://schema.org",
    "@graph": [
      {
        "@type": "WebPage",
        "@id": "https://yeti-125.com/schedule#webpage",
        "url": "https://yeti-125.com/schedule",
        "name": "이리온 방송 일정 — 치지직 버튜버 스트리밍 캘린더",
        "description": "치지직 버추얼 스트리머 이리온의 월간 방송 일정 캘린더.",
        "isPartOf": {
          "@id": "https://yeti-125.com/#website"
        },
        "about": {
          "@id": "https://yeti-125.com/#irion"
        },
        "inLanguage": "ko-KR"
      },
      {
        "@type": "BreadcrumbList",
        "itemListElement": [
          {
            "@type": "ListItem",
            "position": 1,
            "name": "홈",
            "item": "https://yeti-125.com/"
          },
          {
            "@type": "ListItem",
            "position": 2,
            "name": "방송 일정",
            "item": "https://yeti-125.com/schedule"
          }
        ]
      }
    ]
  }
  </script>
  <link rel="stylesheet" href="/resources/css/common.css">
  <link rel="stylesheet" href="/resources/css/schedule.css">
</head>
<body>

<!-- ===== Header ===== -->
<jsp:include page="/WEB-INF/views/common/header.jsp">
  <jsp:param name="active" value="schedule"/>
</jsp:include>

<main>

  <!-- ===== Page masthead ===== -->
  <section class="page-masthead">
    <div class="shell">
      <div class="masthead-grid">
        <div class="masthead-lead">
          <span class="kicker">Archive / No.125</span>
          <h1 class="masthead-title display">방송<br><em>일정</em></h1>
        </div>
        <div class="masthead-aside">
          <p class="masthead-copy">
            이리온의 라이브 스케줄. 캘린더에서 날짜를 선택하면 상세 정보를 볼 수 있습니다.
          </p>
          <span class="idx">002 — Schedule</span>
        </div>
      </div>
    </div>
  </section>

  <!-- ===== Calendar + upcoming, asymmetric split ===== -->
  <section class="section sched-section">
    <div class="shell">
      <div class="sched-grid">

        <!-- calendar column -->
        <div class="sched-cal">
          <div class="legend">
            <span class="legend-head idx">일정 유형</span>
            <div class="legend-items">
              <span class="legend-item">
                <span class="legend-swatch" style="background:#7fb58a;"></span>저스트 채팅
              </span>
              <span class="legend-item">
                <span class="legend-swatch" style="background:#8c8fd6;"></span>종합게임
              </span>
              <span class="legend-item">
                <span class="legend-swatch" style="background:#d6c07f;"></span>노래방송
              </span>
              <span class="legend-item">
                <span class="legend-swatch" style="background:#d68fb0;"></span>합방
              </span>
            </div>
          </div>
          <div class="calendar-wrapper">
            <div id="calendar"></div>
          </div>
        </div>

        <!-- upcoming column -->
        <aside class="sched-upcoming">
          <div class="upcoming-head">
            <span class="kicker">Next Up</span>
            <h2 class="upcoming-title display">다가오는<br>일정</h2>
          </div>
          <div id="upcomingEvents" class="upcoming-list"></div>
          <div id="upcomingEmpty" class="upcoming-empty" style="display: none;">
            <span class="upcoming-empty-mark">∅</span>
            <p>예정된 일정이 없습니다</p>
          </div>
        </aside>

      </div>
    </div>
  </section>

</main>

<!-- ===== Detail Modal ===== -->
<div id="scheduleModal" class="modal">
  <div class="modal-content">
    <button class="modal-close" data-close-modal="scheduleModal">&times;</button>
    <div id="scheduleDetail"></div>
  </div>
</div>

<!-- ===== Footer ===== -->
<jsp:include page="/WEB-INF/views/common/footer.jsp"/>

<jsp:include page="/WEB-INF/views/common/jquery.jsp"/>
<script src="https://cdn.jsdelivr.net/npm/fullcalendar@6.1.10/index.global.min.js"
        integrity="sha384-WfE/vOHqht3KDj6FvpwQUf3UxEPUHoGJ3w1yZ8rhpLWnVigt8HjXL2zXqtcfS7mf"
        crossorigin="anonymous" referrerpolicy="no-referrer"></script>
<script src="/resources/js/common.js"></script>
<script src="/resources/js/schedule.js"></script>
</body>
</html>
