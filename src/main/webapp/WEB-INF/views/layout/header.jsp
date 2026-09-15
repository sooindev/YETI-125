<%@ page pageEncoding="UTF-8" %>
<%-- 공개 상단바. 현재 페이지는 active 파라미터 — index / clips / schedule / info --%>
<header class="header">
  <div class="header-inner">
    <a href="/" class="logo">
      <span class="logo-text">YETI</span>
      <span class="logo-sub">125</span>
    </a>
    <nav class="nav">
      <a href="/" class="nav-link${param.active == 'index' ? ' active' : ''}">Index</a>
      <a href="/clips" class="nav-link${param.active == 'clips' ? ' active' : ''}">Clips</a>
      <a href="/schedule" class="nav-link${param.active == 'schedule' ? ' active' : ''}">Schedule</a>
      <a href="/info" class="nav-link${param.active == 'info' ? ' active' : ''}">Profile</a>
    </nav>
    <jsp:include page="/WEB-INF/views/layout/theme-toggle.jsp"/>
    <button class="mobile-menu-btn" aria-label="메뉴" aria-expanded="false">
      <span></span><span></span><span></span>
    </button>
  </div>
</header>
