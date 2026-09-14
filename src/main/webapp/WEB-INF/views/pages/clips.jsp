<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, viewport-fit=cover">
  <title>이리온 클립 아카이브 — 치지직 버튜버 클립 모음 | YETI-125</title>
  <meta name="description" content="치지직 버추얼 스트리머(버튜버) 이리온(IRION)의 클립 아카이브. 인기순·최신순으로 정렬하고 제목으로 검색해 원하는 장면을 찾으세요.">
  <meta name="keywords" content="이리온 클립, 이리온 하이라이트, 치지직 클립, 버튜버 클립, 이리온 명장면, 치지직 팬사이트, 버튜버 팬사이트, YETI-125">
  <meta name="author" content="sooindev">
  <link rel="canonical" href="https://yeti-125.com/clips">

  <%-- Open Graph --%>
  <meta property="og:type" content="website">
  <meta property="og:site_name" content="YETI-125">
  <meta property="og:title" content="클립 아카이브 — YETI-125 이리온 팬사이트">
  <meta property="og:description" content="이리온의 클립을 인기순·최신순으로 모아 둔 아카이브. 제목으로 검색할 수 있습니다.">
  <meta property="og:url" content="https://yeti-125.com/clips">
  <meta property="og:image" content="https://yeti-125.com/resources/images/profile/Irion-profile.jpg">
  <meta property="og:locale" content="ko_KR">

  <%-- Twitter / X --%>
  <meta name="twitter:card" content="summary_large_image">
  <meta name="twitter:title" content="클립 아카이브 — YETI-125 이리온 팬사이트">
  <meta name="twitter:description" content="이리온의 클립을 인기순·최신순으로 모아 둔 아카이브.">
  <meta name="twitter:image" content="https://yeti-125.com/resources/images/profile/Irion-profile.jpg">

<jsp:include page="/WEB-INF/views/layout/head-assets.jsp"/>
  <%-- 구조화 데이터 (schema.org) --%>
  <script type="application/ld+json">
  {
    "@context": "https://schema.org",
    "@graph": [
      {
        "@type": "CollectionPage",
        "@id": "https://yeti-125.com/clips#webpage",
        "url": "https://yeti-125.com/clips",
        "name": "이리온 클립 아카이브 — 치지직 버튜버 클립 모음",
        "description": "치지직 버추얼 스트리머 이리온의 클립 아카이브. 인기순·최신순 정렬과 제목 검색.",
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
            "name": "클립 아카이브",
            "item": "https://yeti-125.com/clips"
          }
        ]
      }
    ]
  }
  </script>
  <link rel="stylesheet" href="/resources/css/base/common.css">
  <link rel="stylesheet" href="/resources/css/components/media-card.css">
  <link rel="stylesheet" href="/resources/css/components/clip-modal.css">
  <link rel="stylesheet" href="/resources/css/pages/clips.css">
  <link rel="stylesheet" href="/resources/css/base/scroll-animations.css">
</head>
<body>

<%-- ===== Header ===== --%>
<jsp:include page="/WEB-INF/views/layout/header.jsp">
  <jsp:param name="active" value="clips"/>
</jsp:include>

<main>

  <%-- ===== Page masthead ===== --%>
  <section class="page-masthead">
    <div class="shell">
      <div class="masthead-grid">
        <div class="masthead-lead">
          <span class="kicker">Archive / No.125</span>
          <h1 class="masthead-title display">클립<br><em>아카이브</em></h1>
        </div>
        <div class="masthead-aside">
          <p class="masthead-copy">
            시청자들이 잘라 남긴 순간들. 치지직에서 자동으로 모으고,
            클립을 누르면 그 클립만의 주소로 넘어갑니다.
          </p>
          <span class="idx">003 — Clips</span>
        </div>
      </div>
    </div>
  </section>

  <%-- ===== Controls + grid ===== --%>
  <section class="section clips-section">
    <div class="shell">

      <div class="clips-controls">
        <%-- 검색은 form 이다 — 엔터로 제출되고, 스크립트가 죽어도 입력칸이 남는다 --%>
        <form id="clipSearchForm" class="clips-search" role="search" action="/clips" method="get">
          <label class="sr-only" for="clipSearch">클립 제목 검색</label>
          <input type="search" id="clipSearch" name="q" class="clips-search-input"
                 placeholder="제목으로 검색" autocomplete="off" maxlength="50">
          <button type="submit" class="clips-search-btn" aria-label="검색">↵</button>
        </form>

        <div class="clips-sort" role="group" aria-label="정렬">
          <button type="button" class="clips-sort-btn is-active" data-sort="popular">인기순</button>
          <button type="button" class="clips-sort-btn" data-sort="latest">최신순</button>
        </div>
      </div>

      <p id="clipsCount" class="clips-count" hidden></p>

      <div id="clipsContainer" class="card-grid"></div>

      <div id="clipsLoading" class="feed-state" style="display: none;">
        <span class="feed-state-mark">↻</span> 클립을 불러오는 중
      </div>

      <div id="clipsEmpty" class="feed-state" style="display: none;">
        <span class="feed-state-mark">∅</span> 클립을 찾지 못했습니다
      </div>

      <div id="clipsMore" class="feed-more" style="display: none;">
        <button type="button" id="loadMoreBtn" class="btn">더보기</button>
      </div>

    </div>
  </section>

</main>

<%-- ===== Clip Modal (홈 · 아카이브 공용) ===== --%>
<jsp:include page="/WEB-INF/views/layout/clip-modal.jsp"/>

<%-- ===== Footer ===== --%>
<jsp:include page="/WEB-INF/views/layout/footer.jsp"/>

<div id="toast" class="toast"></div>

<jsp:include page="/WEB-INF/views/layout/jquery.jsp"/>
<script src="/resources/js/core/common.js"></script>
<script src="/resources/js/core/scroll-animations.js"></script>
<script src="/resources/js/components/media-card.js"></script>
<script src="/resources/js/components/clip-modal.js"></script>
<script src="/resources/js/pages/clips.js"></script>
</body>
</html>
