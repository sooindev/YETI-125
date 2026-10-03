<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%--
    400·405 에러 페이지. web.xml 의 <error-page> 가 가리킨다.

    없으면 톰캣 기본 화면이 나간다 — 톰캣 버전과 빠진 파라미터 이름까지 찍힌다.
    상태 코드는 정수라 그대로 찍어도 된다
--%>
<%
    Object status = request.getAttribute("javax.servlet.error.status_code");
    int code = (status instanceof Integer) ? (Integer) status : 400;
%>
<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, viewport-fit=cover">
  <title>잘못된 요청입니다 — YETI-125</title>
  <meta name="robots" content="noindex">

<jsp:include page="/WEB-INF/views/layout/head-assets.jsp"/>
  <link rel="stylesheet" href="/resources/css/base/common.css">
  <link rel="stylesheet" href="/resources/css/pages/error.css">
</head>
<body>

<main class="error-stage">
  <div class="shell error-shell">
    <span class="kicker">Error / <%= code %></span>

    <h1 class="error-code display"><%= code %></h1>
    <p class="error-title">잘못된 요청입니다</p>

    <p class="error-desc">
      주소나 요청 형식이 올바르지 않습니다.
      아래에서 원하는 곳으로 이동해 주세요.
    </p>

    <div class="error-actions">
      <a href="/" class="btn btn-primary">홈으로 <span class="btn-arrow">→</span></a>
      <a href="/schedule" class="btn">방송 일정</a>
      <a href="/info" class="btn">프로필</a>
    </div>

    <p class="error-meta idx">YETI-125 · IRION ARCHIVE</p>
  </div>
</main>

</body>
</html>
