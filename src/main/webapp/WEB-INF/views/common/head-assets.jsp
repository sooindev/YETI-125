<%@ page pageEncoding="UTF-8" %>
<%--
  모든 페이지의 <head> 공통 — 파비콘, 폰트, 첫 페인트 스크립트.
  폰트를 두 줄로 나눈 것은 Anton·JetBrains Mono(1.1KB)가 Noto Sans KR(90.8KB) 뒤에
  묶이지 않게 하려는 것이다. preconnect 는 스크립트 앞, 스타일시트는 뒤에 온다.
--%>
  <link rel="icon" type="image/png" href="/resources/images/snowflake.png">
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <script src="/resources/js/theme-init.js"></script>
  <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Anton&family=JetBrains+Mono:wght@400;500;600;700&display=swap">
  <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Noto+Sans+KR:wght@300;400;500;700&display=swap">
