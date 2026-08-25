<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%--
    500 에러 페이지. web.xml 의 <error-page> 가 이 경로를 가리킨다.
    스택 트레이스 대신 발생 시각만 보여줘 서버 로그와 대조하게 한다.
--%>
<%
    String occurredAt = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            .format(new java.util.Date());

    /*
     * "다시 시도" 가 돌아갈 주소.
     *
     * 예전에는 javascript:location.reload() 였는데 CSP 가 막는다 — script-src 에
     * 'unsafe-inline' 이 없어서 javascript: 링크는 실행되지 않는다. 눌러도 아무
     * 일이 없었다. 원래 요청 주소로 가는 평범한 링크로 바꾼다.
     *
     * 이 주소는 바깥에서 들어온 값이라 그대로 심으면 안 된다. 경로 모양인지
     * 확인하고(// 로 시작하면 남의 사이트로 나간다) HTML 특수문자를 바꾼다.
     */
    String retryUrl = "/";
    Object errorUri = request.getAttribute("javax.servlet.error.request_uri");
    if (errorUri instanceof String) {
        String uri = (String) errorUri;
        String query = request.getQueryString();
        if (query != null && !query.isEmpty()) {
            uri = uri + "?" + query;
        }
        if (uri.startsWith("/") && !uri.startsWith("//")) {
            retryUrl = uri;
        }
    }
    retryUrl = retryUrl.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                       .replace("\"", "&quot;").replace("'", "&#39;");
%>
<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, viewport-fit=cover">
  <title>일시적인 오류 — YETI-125</title>
  <meta name="robots" content="noindex">

<jsp:include page="/WEB-INF/views/common/head-assets.jsp"/>
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
      <a href="<%= retryUrl %>" class="btn">다시 시도</a>
    </div>

    <p class="error-meta idx">발생 시각 · <%= occurredAt %></p>
  </div>
</main>

</body>
</html>
