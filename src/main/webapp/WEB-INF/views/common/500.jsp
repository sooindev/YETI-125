<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%--
    500 에러 페이지.

    web.xml 의 <error-page> 가 이 경로를 가리킨다. 이 파일이 없으면
    500 발생 시 없는 페이지로 포워딩되면서 404 빈 응답으로 바뀌어,
    원래 오류가 완전히 가려진다.

    스택 트레이스는 화면에 노출하지 않는다. 대신 발생 시각을 보여줘서
    서버 로그(catalina.out)의 해당 시점과 대조할 수 있게 한다.
--%>
<%
    String occurredAt = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            .format(new java.util.Date());
%>
<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, viewport-fit=cover">
  <title>일시적인 오류 — YETI-125</title>
  <meta name="robots" content="noindex">

  <link rel="icon" type="image/png" href="/resources/images/snowflake.png">
  <script src="/resources/js/theme-init.js"></script>
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Anton&family=JetBrains+Mono:wght@400;500;600;700&family=Noto+Sans+KR:wght@300;400;500;700&display=swap" rel="stylesheet">
  <link rel="stylesheet" href="/resources/css/common.css">
  <link rel="stylesheet" href="/resources/css/error.css">
</head>
<body>

<main class="error-stage">
  <div class="shell error-shell">
    <span class="kicker">Error / 500</span>

    <h1 class="error-code display error-code--warn">500</h1>
    <p class="error-title">일시적인 오류가 발생했습니다</p>

    <p class="error-desc">
      잠시 후 다시 시도해 주세요.
      문제가 계속되면 아래 시각을 알려주시면 확인에 도움이 됩니다.
    </p>

    <div class="error-actions">
      <a href="/" class="btn btn-primary">홈으로 <span class="btn-arrow">→</span></a>
      <a href="javascript:location.reload()" class="btn">다시 시도</a>
    </div>

    <p class="error-meta idx">발생 시각 · <%= occurredAt %></p>
  </div>
</main>

</body>
</html>
