<%@ page pageEncoding="UTF-8" %>
<%--
  클립 모달 — 홈과 클립 아카이브가 함께 쓴다.
  치지직 공식 임베드로 그 자리에서 재생하고, 조회수 집계·시청 제한은 치지직 정책을 따른다.

  링크가 둘인 이유: 모달로 보면 주소가 바뀌지 않아, 마음에 든 클립의 주소를 집어갈 길이
  없다. "이 클립 페이지" 가 그 길이다 (js/components/clip-modal.js 가 두 주소를 채운다).
--%>
<div id="clipModal" class="modal">
  <div class="modal-content clip-modal-content">
    <button class="modal-close" data-close-modal="clipModal">&times;</button>
    <h3 id="clipModalTitle" class="clip-modal-title"></h3>
    <div class="clip-modal-frame">
      <iframe id="clipModalFrame" src="" title="치지직 클립 플레이어"
              frameborder="0" scrolling="no"
              allow="autoplay; fullscreen; encrypted-media; picture-in-picture"
              allowfullscreen></iframe>
    </div>
    <div class="clip-modal-links">
      <a id="clipModalPage" class="clip-modal-link" href="/clips">
        이 클립 페이지 <span class="btn-arrow">→</span>
      </a>
      <a id="clipModalOrigin" class="clip-modal-origin" href="https://chzzk.naver.com/" target="_blank" rel="noopener">
        치지직에서 원본 보기 <span class="btn-arrow">↗</span>
      </a>
    </div>
  </div>
</div>
