<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%--
  클립 한 건.

  이 화면만 서버가 내용까지 그린다. 제목과 썸네일이 HTML 에 박혀 나가야
  검색엔진이 색인하고 카카오톡·X 가 미리보기를 뽑기 때문이다.

  ${...} 는 JSTL 없이 받은 글자를 그대로 찍는다. 여기 꽂히는 값은 전부
  ClipController.ClipView 에서 이스케이프를 마친 것이다 — 새 값을 넣을 때도
  반드시 거기서 Escape 를 지나게 할 것.
--%>
<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, viewport-fit=cover">
  <title>${title} — 이리온 클립 | YETI-125</title>
  <meta name="description" content="${description}">
  <meta name="author" content="sooindev">
  <meta name="robots" content="${robots}">
  <link rel="canonical" href="${pageUrl}">

  <%-- Open Graph — 공유했을 때 뜨는 카드가 이 화면의 존재 이유 절반이다 --%>
  <meta property="og:type" content="video.other">
  <meta property="og:site_name" content="YETI-125">
  <meta property="og:title" content="${title} — 이리온 클립">
  <meta property="og:description" content="${description}">
  <meta property="og:url" content="${pageUrl}">
  <meta property="og:image" content="${ogImage}">
  <meta property="og:locale" content="ko_KR">

  <%-- Twitter / X --%>
  <meta name="twitter:card" content="summary_large_image">
  <meta name="twitter:title" content="${title} — 이리온 클립">
  <meta name="twitter:description" content="${description}">
  <meta name="twitter:image" content="${ogImage}">

<jsp:include page="/WEB-INF/views/layout/head-assets.jsp"/>
  <%-- 구조화 데이터 (schema.org) --%>
  <script type="application/ld+json">
  {
    "@context": "https://schema.org",
    "@graph": [
      {
        "@type": "VideoObject",
        "@id": "${pageUrl}#clip",
        "name": "${titleJson}",
        "description": "${descriptionJson}",
        "thumbnailUrl": "${ogImage}",
        "uploadDate": "${createdIso}",
        "duration": "${durationIso}",
        "url": "${pageUrl}",
        "contentUrl": "${clipUrl}",
        "interactionStatistic": {
          "@type": "InteractionCounter",
          "interactionType": {
            "@type": "WatchAction"
          },
          "userInteractionCount": ${viewCount}
        },
        "creator": {
          "@id": "https://yeti-125.com/#irion"
        },
        "isPartOf": {
          "@id": "https://yeti-125.com/#website"
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
          },
          {
            "@type": "ListItem",
            "position": 3,
            "name": "${titleJson}",
            "item": "${pageUrl}"
          }
        ]
      }
    ]
  }
  </script>
  <link rel="stylesheet" href="/resources/css/base/common.css">
  <link rel="stylesheet" href="/resources/css/components/media-card.css">
  <link rel="stylesheet" href="/resources/css/pages/clips.css">
</head>
<body>

<%-- ===== Header ===== --%>
<jsp:include page="/WEB-INF/views/layout/header.jsp">
  <jsp:param name="active" value="clips"/>
</jsp:include>

<main>

  <section class="clip-detail">
    <div class="shell">

      <nav class="clip-crumb" aria-label="현재 위치">
        <a href="/clips">클립 아카이브</a>
        <span class="clip-crumb-sep">/</span>
        <%-- 구조화 데이터(BreadcrumbList)의 3번째 항목과 같은 이름이어야 한다.
             clipUID 를 찍으면 방문자에게는 뜻 없는 글자고, 우리가 검색엔진에 알린 것과도 어긋난다 --%>
        <span class="clip-crumb-current">${title}</span>
      </nav>

      <div class="clip-detail-grid">

        <%-- 썸네일. 재생은 치지직에서 한다 — 눌러도 이 자리에서 열리지 않는다 --%>
        <div class="clip-detail-media">
          <img class="clip-detail-img" src="${thumbnailUrl}" alt="${title} 썸네일"${thumbnailHidden}>
          <div class="thumb-fallback" role="img" aria-label="${fallbackLabel}"${fallbackHidden}>
            <span class="thumb-fallback-badge"${fallbackBadgeHidden}>19</span>
            <span class="thumb-fallback-note">${fallbackNote}</span>
          </div>
          <span class="clip-detail-duration">${durationText}</span>
        </div>

        <div class="clip-detail-body">
          <span class="kicker">Clip</span>
          <h1 class="clip-detail-title display">${title}</h1>

          <dl class="clip-detail-meta">
            <div class="clip-detail-meta-row">
              <dt>조회수</dt>
              <dd>${viewCountText}</dd>
            </div>
            <div class="clip-detail-meta-row">
              <dt>길이</dt>
              <dd>${durationText}</dd>
            </div>
            <div class="clip-detail-meta-row">
              <dt>만든 날</dt>
              <dd>${createdText}</dd>
            </div>
          </dl>

          <a class="btn btn-primary clip-detail-go" href="${clipUrl}" target="_blank" rel="noopener">
            치지직에서 보기 <span class="btn-arrow">↗</span>
          </a>

          <p class="clip-detail-note">
            재생은 치지직에서 이루어집니다. 조회수 집계와 시청 제한은 치지직 정책을 그대로 따릅니다.
          </p>
        </div>

      </div>

      <div class="clip-detail-back">
        <a href="/clips" class="btn">← 클립 아카이브로</a>
      </div>

    </div>
  </section>

</main>

<%-- ===== Footer ===== --%>
<jsp:include page="/WEB-INF/views/layout/footer.jsp"/>

<jsp:include page="/WEB-INF/views/layout/jquery.jsp"/>
<script src="/resources/js/core/common.js"></script>
</body>
</html>
