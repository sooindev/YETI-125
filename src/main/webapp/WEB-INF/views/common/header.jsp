<%@ page pageEncoding="UTF-8" %>
<%-- 공개 페이지 상단바. 지금 페이지는 active 파라미터로 알린다 — index / schedule / info --%>
<header class="header">
  <div class="header-inner">
    <a href="/" class="logo">
      <span class="logo-text">YETI</span>
      <span class="logo-sub">125</span>
    </a>
    <nav class="nav">
      <a href="/" class="nav-link${param.active == 'index' ? ' active' : ''}">Index</a>
      <a href="/schedule" class="nav-link${param.active == 'schedule' ? ' active' : ''}">Schedule</a>
      <a href="/info" class="nav-link${param.active == 'info' ? ' active' : ''}">Profile</a>
      <%-- 3주년 기념 방명록. 기념이 끝나 내릴 때는 이 줄과 guestbook.jsp 를 지운다 --%>
      <a href="/guestbook" class="nav-link${param.active == 'guestbook' ? ' active' : ''}">Guestbook</a>
    </nav>
    <jsp:include page="/WEB-INF/views/common/theme-toggle.jsp"/>
    <button class="mobile-menu-btn" aria-label="메뉴" aria-expanded="false">
      <span></span><span></span><span></span>
    </button>
  </div>
</header>
