<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, viewport-fit=cover">
  <title>데뷔 3주년 축하 방명록 — 이리온 팬사이트 | YETI-125</title>
  <meta name="description" content="치지직 버추얼 스트리머 이리온(IRION)의 데뷔 3주년을 축하하는 팬 방명록. 축하 메시지와 응원의 한마디를 남겨 주세요.">
  <meta name="author" content="sooindev">
  <link rel="canonical" href="https://yeti-125.com/guestbook">

  <%-- Open Graph --%>
  <meta property="og:type" content="website">
  <meta property="og:site_name" content="YETI-125">
  <meta property="og:title" content="이리온 데뷔 3주년 축하 방명록">
  <meta property="og:description" content="이리온의 데뷔 3주년을 함께 축하해 주세요. 축하 메시지와 응원의 한마디를 남길 수 있습니다.">
  <meta property="og:url" content="https://yeti-125.com/guestbook">
  <meta property="og:image" content="https://yeti-125.com/resources/images/Irion-profile.jpg">
  <meta property="og:locale" content="ko_KR">

  <%-- Twitter / X --%>
  <meta name="twitter:card" content="summary_large_image">
  <meta name="twitter:title" content="이리온 데뷔 3주년 축하 방명록">
  <meta name="twitter:description" content="이리온의 데뷔 3주년을 함께 축하해 주세요.">
  <meta name="twitter:image" content="https://yeti-125.com/resources/images/Irion-profile.jpg">

<jsp:include page="/WEB-INF/views/common/head-assets.jsp"/>
  <link rel="stylesheet" href="/resources/css/common.css">
  <link rel="stylesheet" href="/resources/css/guestbook.css">
  <%-- 카드가 순서대로 떠오르는 등장 효과. guestbook.js 가 observeNewElements() 로 다시 태운다 --%>
  <link rel="stylesheet" href="/resources/css/scroll-animations.css">
  <%-- 데뷔 3주년 축하 연출 (기념 주간 한정) --%>
  <link rel="stylesheet" href="/resources/css/anniversary.css">
</head>
<%--
  data-admin 은 화면 표시용일 뿐이다. 삭제 버튼을 보이느냐만 정하고,
  실제 권한은 /admin/guestbook/* 의 필터·인터셉터가 서버에서 판단한다.
  브라우저에서 이 값을 Y 로 고쳐도 삭제 요청은 401 로 막힌다.
--%>
<body class="gb-page" data-admin="${not empty sessionScope.adminUser ? 'Y' : 'N'}">

<jsp:include page="/WEB-INF/views/common/header.jsp">
  <jsp:param name="active" value="guestbook"/>
</jsp:include>

<main>

  <%-- ===== 히어로 ===== --%>
  <section class="gb-hero">
    <div class="shell">
      <div class="gb-hero-inner">
        <div>
          <span class="kicker">Archive / Anniversary</span>
          <h1 class="gb-hero-title">
            이리온의 세 번째 겨울,<br>
            <em>축하 한마디</em>를 남겨 주세요
          </h1>
          <p class="gb-hero-copy">
            2023년 9월 12일 첫 방송으로부터 3년.
            예티들이 남긴 마음이 이곳에 차곡차곡 쌓입니다.
          </p>
        </div>

        <div class="gb-hero-meta">
          <span class="gb-hero-meta-label">Entries</span>
          <span class="gb-hero-meta-value" id="gbTotal">0</span>
        </div>
      </div>
    </div>
  </section>

  <%-- ===== 작성 패널 ===== --%>
  <section class="gb-compose">
    <div class="shell">
      <form id="gbForm" class="gb-form" autocomplete="off">
        <div class="gb-form-head">
          <h2 class="gb-form-title">축하 메시지 남기기</h2>
          <p class="gb-form-sub">모두 익명으로 남겨집니다. 이름 없이 마음만 적어 주세요.</p>
        </div>

        <div class="gb-field">
          <div class="gb-field-label">
            <label for="gbContent">축하 메시지<span class="gb-required">*</span></label>
            <%-- maxlength 로 이미 막히지만, 얼마나 남았는지 보여야 글을 다듬는다 --%>
            <span class="gb-counter"><span id="gbContentCount">0</span> / 500</span>
          </div>
          <textarea id="gbContent" name="content" maxlength="500" rows="4"
                    placeholder="이리온에게 전하고 싶은 축하 인사를 적어 주세요" required></textarea>
        </div>

        <div class="gb-field">
          <div class="gb-field-label">
            <label for="gbCheer">응원의 한마디 <span class="gb-optional">선택</span></label>
          </div>
          <input type="text" id="gbCheer" name="cheer" maxlength="100"
                 placeholder="예) 앞으로도 눈처럼 반짝이길!">
        </div>

        <button type="submit" id="gbSubmit" class="btn btn-primary">
          축하 남기기 <span class="btn-arrow">→</span>
        </button>
      </form>
    </div>
  </section>

  <%-- ===== 방명록 목록 ===== --%>
  <section class="section gb-list-section">
    <div class="shell">
      <div class="sec-head">
        <h2 class="sec-head__title">예티들의<br><em>축하</em></h2>
        <p class="sec-head__aside">
          남겨진 축하가 최신순으로 쌓입니다. 번호는 몇 번째 축하인지를 뜻합니다.
        </p>
      </div>

      <div id="gbList" class="gb-grid"></div>

      <div id="gbEmpty" class="gb-state" style="display: none;">
        <span class="gb-state-mark">∅</span>
        아직 남겨진 축하가 없습니다
      </div>

      <div class="gb-more" id="gbMore" style="display: none;">
        <button type="button" id="gbMoreBtn" class="btn">
          더 보기 <span class="btn-arrow">↓</span>
        </button>
      </div>
    </div>
  </section>

</main>

<div id="toast" class="toast" role="status" aria-live="polite"></div>

<jsp:include page="/WEB-INF/views/common/footer.jsp"/>

<jsp:include page="/WEB-INF/views/common/jquery.jsp"/>
<script src="/resources/js/common.js"></script>
<script src="/resources/js/scroll-animations.js"></script>
<script src="/resources/js/guestbook.js"></script>
<script src="/resources/js/anniversary.js"></script>
</body>
</html>
