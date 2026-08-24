<%@ page pageEncoding="UTF-8" %>
<%--
  모든 페이지의 <head> 공통 부분 — 파비콘, 폰트, 첫 페인트 스크립트.

  폰트는 두 줄로 나눠 받는다 — Anton·JetBrains Mono(1.1KB)가
  Noto Sans KR(90.8KB) 뒤에 묶여 기다리지 않도록.
  preconnect 는 스크립트 앞, 스타일시트는 스크립트 뒤에 온다.
  (이유는 common.css 의 "대체 폰트 보정" 주석 참고)
--%>
  <link rel="icon" type="image/png" href="/resources/images/snowflake.png">
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <script src="/resources/js/theme-init.js"></script>
  <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Anton&family=JetBrains+Mono:wght@400;500;600;700&display=swap">
  <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Noto+Sans+KR:wght@300;400;500;700&display=swap">
