<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <meta name="robots" content="noindex, nofollow">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, viewport-fit=cover">
  <title>Admin Login — YETI-125</title>
<jsp:include page="/WEB-INF/views/common/head-assets.jsp"/>
  <link rel="stylesheet" href="/resources/css/common.css">
  <link rel="stylesheet" href="/resources/css/admin.css">
</head>
<body class="login-page">

<main class="login-stage">
  <!-- left: oversized identity -->
  <section class="login-aside">
    <div class="login-aside-top">
      <span class="logo-text">YETI</span>
      <span class="logo-sub">125</span>
    </div>
    <h1 class="login-aside-title display">CONTROL<br><em>ROOM</em></h1>
    <div class="login-aside-foot idx">RESTRICTED ACCESS · ADMIN ONLY</div>
  </section>

  <!-- right: form -->
  <section class="login-panel">
    <div class="login-box">
      <span class="kicker">Authentication</span>
      <h2 class="login-heading display">관리자<br>로그인</h2>

      <div id="errorMsg" class="alert alert-error" style="display: none;"></div>

      <form id="loginForm" class="login-form">
        <div class="field">
          <label for="adminLoginId">아이디 / ID</label>
          <input type="text" id="adminLoginId" name="adminLoginId"
                 placeholder="관리자 아이디" required autofocus autocomplete="username">
        </div>
        <div class="field">
          <label for="password">비밀번호 / Password</label>
          <input type="password" id="password" name="password"
                 placeholder="비밀번호" required autocomplete="current-password">
        </div>
        <button type="submit" class="btn btn-primary btn-block">
          로그인 <span class="btn-arrow">→</span>
        </button>
      </form>

      <a href="/" class="login-back">← 사이트로 돌아가기</a>
    </div>
  </section>
</main>

<jsp:include page="/WEB-INF/views/common/jquery.jsp"/>
<script src="/resources/js/admin-login.js"></script>
</body>
</html>
