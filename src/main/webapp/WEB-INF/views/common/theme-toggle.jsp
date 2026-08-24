<%@ page pageEncoding="UTF-8" %>
<%--
  테마 토글 버튼. 공개 상단바와 관리자 상단바가 같이 쓴다.

  아이콘 셋은 theme-init.js 가 버튼에 찍는 data-mode 로 고른다
  (시스템 → 라이트 → 다크). 아이콘을 늘리거나 바꿀 곳은 여기 한 군데다.
--%>
<button class="theme-toggle" id="themeToggle" aria-label="테마 전환">
      <svg class="icon-auto" viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="8.2"/><path class="half" d="M12 3.8a8.2 8.2 0 0 1 0 16.4z"/></svg>
      <svg class="icon-moon" viewBox="0 0 24 24" aria-hidden="true"><path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z"/></svg>
      <svg class="icon-sun" viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="4.2"/><path d="M12 2.4v2.6M12 19v2.6M4.2 4.2l1.9 1.9M17.9 17.9l1.9 1.9M2.4 12h2.6M19 12h2.6M4.2 19.8l1.9-1.9M17.9 6.1l1.9-1.9"/></svg>
    </button>
