<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, viewport-fit=cover">
  <title>이리온 프로필 — 치지직 설녀 버튜버 | YETI-125</title>
  <meta name="description" content="치지직 버추얼 스트리머(버튜버) 이리온(IRION)의 프로필. 설녀 캐릭터 설정, 데뷔일, 신장, MBTI, 제작 크레딧과 채널·SNS 링크를 한눈에 확인하세요.">
  <meta name="keywords" content="이리온 프로필, IRION 프로필, 설녀 버튜버, 치지직 버튜버, 버추얼 스트리머, 이리온 데뷔일, 이리온 MBTI, 버튜버 팬사이트, YETI-125">
  <meta name="author" content="sooindev">
  <link rel="canonical" href="https://yeti-125.com/info">

  <!-- Open Graph -->
  <meta property="og:type" content="profile">
  <meta property="og:site_name" content="YETI-125">
  <meta property="og:title" content="프로필 — 이리온(IRION)">
  <meta property="og:description" content="설녀 VTuber 이리온의 프로필. 캐릭터 설정, 데뷔일, 채널·SNS 링크를 한눈에.">
  <meta property="og:url" content="https://yeti-125.com/info">
  <meta property="og:image" content="https://yeti-125.com/resources/images/Irion-profile.jpg">
  <meta property="og:locale" content="ko_KR">

  <!-- Twitter / X -->
  <meta name="twitter:card" content="summary_large_image">
  <meta name="twitter:title" content="프로필 — 이리온(IRION)">
  <meta name="twitter:description" content="설녀 VTuber 이리온의 프로필. 캐릭터 설정, 데뷔일, 채널·SNS 링크.">
  <meta name="twitter:image" content="https://yeti-125.com/resources/images/Irion-profile.jpg">

<jsp:include page="/WEB-INF/views/common/head-assets.jsp"/>
  <!-- 구조화 데이터 (schema.org) -->
  <script type="application/ld+json">
  {
    "@context": "https://schema.org",
    "@graph": [
      {
        "@type": "ProfilePage",
        "@id": "https://yeti-125.com/info#webpage",
        "url": "https://yeti-125.com/info",
        "name": "이리온(IRION) 프로필 — 치지직 설녀 버튜버",
        "description": "버추얼 스트리머 이리온의 캐릭터 설정과 채널 링크.",
        "isPartOf": {
          "@id": "https://yeti-125.com/#website"
        },
        "mainEntity": {
          "@id": "https://yeti-125.com/#irion"
        },
        "primaryImageOfPage": {
          "@type": "ImageObject",
          "url": "https://yeti-125.com/resources/images/Irion-profile.jpg"
        },
        "inLanguage": "ko-KR"
      },
      {
        "@type": "Person",
        "@id": "https://yeti-125.com/#irion",
        "name": "이리온",
        "alternateName": [
          "IRION",
          "이리온 IRION"
        ],
        "description": "치지직에서 활동하는 설녀 콘셉트의 버추얼 스트리머(버튜버). 2023년 9월 12일 데뷔.",
        "jobTitle": "버추얼 스트리머",
        "image": "https://yeti-125.com/resources/images/Irion-profile.jpg",
        "url": "https://yeti-125.com/info",
        "height": "165 cm",
        "knowsLanguage": "ko",
        "sameAs": [
          "https://chzzk.naver.com/63368ec9081dc85e61d0e4310b7e1602",
          "https://www.youtube.com/@2leon0809",
          "https://www.youtube.com/@2reon",
          "https://x.com/lrion_125",
          "https://cafe.naver.com/dlfldhs0809"
        ]
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
            "name": "프로필",
            "item": "https://yeti-125.com/info"
          }
        ]
      }
    ]
  }
  </script>
  <link rel="stylesheet" href="/resources/css/common.css">
  <link rel="stylesheet" href="/resources/css/info.css">
</head>
<body>

<!-- ===== Header ===== -->
<jsp:include page="/WEB-INF/views/common/header.jsp">
  <jsp:param name="active" value="info"/>
</jsp:include>

<main>

  <!-- ===== Profile hero ===== -->
  <section class="profile-hero">
    <div class="shell">
      <div class="profile-hero-grid">

        <div class="profile-portrait">
          <!-- 이 이미지만 width/height 를 붙이지 않는다.
               .profile-portrait img 가 aspect-ratio: 4/5 로 자리를 이미 잡아두는데,
               속성으로 height 를 주면 그 값이 이겨서 882px 짜리 세로로 늘어난다.
               (원본은 882x882 정사각이고 object-fit: cover 로 잘라 쓴다) -->
          <img src="/resources/images/Irion-profile.jpg" alt="이리온">
          <span class="portrait-tag idx">FILE / IRION-125</span>
        </div>

        <div class="profile-id">
          <span class="kicker">Profile / 설녀 VTuber</span>
          <h1 class="profile-name display">이리온<span class="profile-name-en">IRION</span></h1>
          <p class="profile-tagline">설녀 VTuber · 치지직 스트리머</p>

          <ul class="profile-vitals">
            <li><span class="vital-k">Birthday</span><span class="vital-v">04 / 10</span></li>
            <li><span class="vital-k">Height</span><span class="vital-v">165 cm</span></li>
            <li><span class="vital-k">MBTI</span><span class="vital-v">INFJ</span></li>
            <li><span class="vital-k">Mark</span><span class="vital-v">❄ / 🌸</span></li>
          </ul>
        </div>

      </div>

      <!-- D-Day counters -->
      <div class="dday-row">
        <div class="dday-cell">
          <span class="dday-label idx">데뷔 — Debut</span>
          <span class="dday-count display" id="debutDday">D+000</span>
        </div>
        <div class="dday-cell">
          <span class="dday-label idx">생일 — Birthday</span>
          <span class="dday-count display" id="birthdayDday">D-000</span>
        </div>
        <div class="dday-cell dday-cell--social">
          <span class="dday-label idx">Channels</span>
          <div class="quick-social">
            <a href="https://chzzk.naver.com/live/63368ec9081dc85e61d0e4310b7e1602" target="_blank" class="quick-social-btn" title="치지직">
              <img src="/resources/images/chzzk.png" alt="치지직" width="22" height="22">
            </a>
            <a href="https://www.youtube.com/@2leon0809" target="_blank" class="quick-social-btn" title="유튜브">
              <img src="/resources/images/youtube.webp" alt="유튜브" width="22" height="22">
            </a>
            <a href="https://x.com/lrion_125" target="_blank" class="quick-social-btn" title="X">
              <img src="/resources/images/x.webp" alt="X" width="22" height="22">
            </a>
            <a href="https://cafe.naver.com/dlfldhs0809" target="_blank" class="quick-social-btn" title="팬카페">
              <img src="/resources/images/naver-cafe.png" alt="네이버 카페" width="22" height="22">
            </a>
          </div>
        </div>
      </div>
    </div>
  </section>

  <!-- ===== Detail spec sheet ===== -->
  <section class="section spec-section">
    <div class="shell">
      <div class="sec-head scroll-animate fade-in">
        <h2 class="sec-head__title">프로필<br><em>명세</em></h2>
        <p class="sec-head__aside aside--nowrap">
          버추얼 설정과 제작 크레딧. 이리온이라는 설녀에 대한 기록.
        </p>
      </div>

      <div class="spec-grid">

        <article class="spec-card">
          <header class="spec-card-head">
            <span class="spec-card-idx">01</span>
            <h3>기본 정보</h3>
          </header>
          <dl class="spec-rows">
            <div class="spec-row"><dt>이름</dt><dd>이리온</dd></div>
            <div class="spec-row"><dt>성별</dt><dd>여성</dd></div>
            <div class="spec-row"><dt>종족</dt><dd>설녀</dd></div>
            <div class="spec-row"><dt>나이</dt><dd>500살</dd></div>
          </dl>
        </article>

        <article class="spec-card">
          <header class="spec-card-head">
            <span class="spec-card-idx">02</span>
            <h3>상세 정보</h3>
          </header>
          <dl class="spec-rows">
            <div class="spec-row"><dt>생일</dt><dd>4월 10일</dd></div>
            <div class="spec-row"><dt>신장</dt><dd>165cm</dd></div>
            <div class="spec-row"><dt>MBTI</dt><dd class="spec-hl">INFJ</dd></div>
            <div class="spec-row"><dt>데뷔일</dt><dd>2023. 09. 12</dd></div>
            <div class="spec-row"><dt>오시마크</dt><dd>❄️ 🌸</dd></div>
          </dl>
        </article>

        <article class="spec-card">
          <header class="spec-card-head">
            <span class="spec-card-idx">03</span>
            <h3>크레딧</h3>
          </header>
          <dl class="spec-rows">
            <div class="spec-row"><dt>캐릭터 디자인</dt><dd>Hisiya</dd></div>
            <div class="spec-row"><dt>Live2D</dt><dd>Doha</dd></div>
          </dl>
        </article>

        <article class="spec-card spec-card--accent">
          <header class="spec-card-head">
            <span class="spec-card-idx">04</span>
            <h3>팬 정보</h3>
          </header>
          <dl class="spec-rows">
            <div class="spec-row"><dt>팬네임</dt><dd class="spec-hl">예티</dd></div>
            <div class="spec-row">
              <dt>팬카페</dt>
              <dd><a href="https://cafe.naver.com/dlfldhs0809" target="_blank" class="spec-link">이리온 공식 팬카페 ↗</a></dd>
            </div>
          </dl>
        </article>

      </div>
    </div>
  </section>

  <!-- ===== Channels ===== -->
  <section class="section channel-section">
    <div class="shell">
      <div class="sec-head sec-head--flip scroll-animate fade-in">
        <h2 class="sec-head__title">채널 <em>&amp;</em><br>SNS</h2>
        <p class="sec-head__aside">방송과 소통이 이루어지는 모든 채널.</p>
      </div>

      <ul class="channel-list stagger-container">
        <li>
          <a href="https://chzzk.naver.com/live/63368ec9081dc85e61d0e4310b7e1602" target="_blank" class="channel-row">
            <span class="channel-idx">01</span>
            <img src="https://ssl.pstatic.net/static/nng/glive/icon/favicon.png" alt="치지직" class="channel-ico" width="28" height="28">
            <span class="channel-name">치지직</span>
            <span class="channel-desc">메인 방송 플랫폼</span>
            <span class="channel-arrow">↗</span>
          </a>
        </li>
        <li>
          <a href="https://www.youtube.com/@2leon0809" target="_blank" class="channel-row">
            <span class="channel-idx">02</span>
            <img src="/resources/images/youtube.webp" alt="유튜브" class="channel-ico" width="28" height="28">
            <span class="channel-name">유튜브</span>
            <span class="channel-desc">메인 유튜브 채널</span>
            <span class="channel-arrow">↗</span>
          </a>
        </li>
        <li>
          <a href="https://www.youtube.com/@2reon" target="_blank" class="channel-row">
            <span class="channel-idx">03</span>
            <img src="/resources/images/youtube.webp" alt="유튜브" class="channel-ico" width="28" height="28">
            <span class="channel-name">다시보기</span>
            <span class="channel-desc">방송 다시보기 채널</span>
            <span class="channel-arrow">↗</span>
          </a>
        </li>
        <li>
          <a href="https://x.com/lrion_125" target="_blank" class="channel-row">
            <span class="channel-idx">04</span>
            <img src="/resources/images/x.webp" alt="X" class="channel-ico" width="28" height="28">
            <span class="channel-name">X / Twitter</span>
            <span class="channel-desc">공지 및 소통</span>
            <span class="channel-arrow">↗</span>
          </a>
        </li>
        <li>
          <a href="https://cafe.naver.com/dlfldhs0809" target="_blank" class="channel-row">
            <span class="channel-idx">05</span>
            <img src="/resources/images/naver-cafe.png" alt="네이버 카페" class="channel-ico" width="28" height="28">
            <span class="channel-name">팬카페</span>
            <span class="channel-desc">네이버 팬카페</span>
            <span class="channel-arrow">↗</span>
          </a>
        </li>
      </ul>
    </div>
  </section>

  <!-- ===== About note ===== -->
  <section class="section about-section">
    <div class="shell">
      <div class="about-slab scroll-animate fade-in">
        <span class="about-quote">“</span>
        <p class="about-text">
          이 사이트는 이리온을 응원하는 예티가 만든 <strong>비공식 팬사이트</strong>입니다.
        </p>
        <span class="about-sign idx">— Yeti / Fan Archive</span>
      </div>
    </div>
  </section>

</main>

<!-- ===== Footer ===== -->
<jsp:include page="/WEB-INF/views/common/footer.jsp"/>

<jsp:include page="/WEB-INF/views/common/jquery.jsp"/>
<script src="/resources/js/common.js"></script>
<script src="/resources/js/scroll-animations.js"></script>
<script src="/resources/js/info.js"></script>
</body>
</html>
