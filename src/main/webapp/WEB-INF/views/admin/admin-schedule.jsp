<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <meta name="robots" content="noindex, nofollow">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, viewport-fit=cover">
  <title>Schedule Admin — YETI-125</title>
<jsp:include page="/WEB-INF/views/common/head-assets.jsp"/>
  <!-- FullCalendar 6 은 CSS 를 JS 번들 안에서 주입한다. 별도 스타일시트가 없다. -->
  <link rel="stylesheet" href="/resources/css/common.css">
  <link rel="stylesheet" href="/resources/css/admin.css">
</head>
<body>

<!-- ===== Admin Header ===== -->
<header class="header admin-header">
  <div class="header-inner">
    <a href="/admin/schedule" class="logo">
      <span class="logo-text">YETI</span>
      <span class="logo-sub">ADMIN</span>
    </a>
    <nav class="nav admin-nav">
      <a href="/admin/schedule" class="nav-link active">일정 관리</a>
      <a href="/" target="_blank" class="nav-link">사이트 보기</a>
      <a href="#" id="logoutBtn" class="nav-link logout-btn">로그아웃</a>
    </nav>
    <jsp:include page="/WEB-INF/views/common/theme-toggle.jsp"/>
    <button class="mobile-menu-btn" aria-label="메뉴" aria-expanded="false">
      <span></span><span></span><span></span>
    </button>
  </div>
</header>

<main class="admin-main">
  <div class="shell">

    <!-- page header -->
    <div class="admin-pagehead">
      <div class="admin-pagehead-lead">
        <span class="kicker">Admin / Schedule</span>
        <h1 class="admin-pagehead-title display">일정 <em>관리</em></h1>
      </div>
      <button class="btn btn-primary" data-action="add">
        새 일정 추가 <span class="btn-arrow">+</span>
      </button>
    </div>

    <!-- calendar -->
    <div class="admin-cal-wrap">
      <div class="calendar-wrapper">
        <div id="calendar"></div>
      </div>
    </div>

  </div>
</main>

<!-- ===== Add/Edit Modal ===== -->
<div id="scheduleModal" class="modal">
  <div class="modal-content modal-large">
    <div class="modal-header">
      <h2 id="modalTitle" class="display">일정 추가</h2>
      <button class="modal-close">&times;</button>
    </div>
    <form id="scheduleForm" class="modal-body">
      <input type="hidden" id="scheduleId" name="scheduleId">

      <div class="field">
        <label for="title">제목 *</label>
        <input type="text" id="title" name="title" required placeholder="일정 제목">
      </div>

      <div class="field-row">
        <div class="field">
          <label for="scheduleType">유형</label>
          <select id="scheduleType" name="scheduleType">
            <option value="JUSTCHAT">저스트 채팅</option>
            <option value="GAME">종합게임</option>
            <option value="KARAOKE">노래방송</option>
            <option value="COLLAB">합방</option>
          </select>
        </div>
        <div class="field">
          <label for="color">색상</label>
          <input type="color" id="color" name="color" value="#8c8fd6">
        </div>
      </div>

      <div class="field-row">
        <div class="field">
          <label for="startDate">시작 일시 *</label>
          <input type="datetime-local" id="startDate" name="startDate" required>
        </div>
        <div class="field">
          <label for="endDate">종료 일시</label>
          <input type="datetime-local" id="endDate" name="endDate">
        </div>
      </div>

      <div class="check-row">
        <label class="check">
          <input type="checkbox" id="allDayYn" name="allDayYn">
          <span class="check-box"></span>
          <span class="check-text">종일 일정</span>
        </label>
        <label class="check">
          <input type="checkbox" id="displayYn" name="displayYn" checked>
          <span class="check-box"></span>
          <span class="check-text">사용자에게 표시</span>
        </label>
      </div>

      <div class="field">
        <label for="description">설명</label>
        <textarea id="description" name="description" rows="3" placeholder="일정에 대한 설명"></textarea>
      </div>

      <div class="modal-footer">
        <button type="button" class="btn btn-secondary" data-action="cancel">취소</button>
        <button type="button" class="btn btn-danger" id="deleteBtn" style="display: none;">삭제</button>
        <button type="submit" class="btn btn-primary">저장</button>
      </div>
    </form>
  </div>
</div>

<!-- Toast -->
<div id="toast" class="toast" role="status" aria-live="polite"></div>

<jsp:include page="/WEB-INF/views/common/jquery.jsp"/>
<script src="https://cdn.jsdelivr.net/npm/fullcalendar@6.1.10/index.global.min.js"
        integrity="sha384-WfE/vOHqht3KDj6FvpwQUf3UxEPUHoGJ3w1yZ8rhpLWnVigt8HjXL2zXqtcfS7mf"
        crossorigin="anonymous" referrerpolicy="no-referrer"></script>
<script src="/resources/js/common.js"></script>
<script src="/resources/js/admin-schedule.js"></script>
</body>
</html>
