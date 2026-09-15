<%@ page pageEncoding="UTF-8" %>
<%--
  클립 모달 — 홈과 클립 아카이브 공용.
  치지직 공식 임베드로 재생하며 조회수·시청 제한은 치지직 정책을 따른다.

  링크가 둘인 이유 — 모달은 주소가 바뀌지 않아 클립 주소를 집어갈 길이 없다.
  두 주소는 js/components/clip-modal.js 가 채운다
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
