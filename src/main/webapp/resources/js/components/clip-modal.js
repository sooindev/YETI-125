/*
  클립 모달 — 홈과 클립 아카이브가 함께 쓴다.

  카드는 어느 화면에서든 진짜 <a> 다. 평범한 클릭만 가로채 모달을 띄우고,
  ⌘·Ctrl·Shift·휠 클릭은 그대로 보낸다 — 새 탭으로 열려는 사람을 막지 않는다.
  카드가 가리키는 곳은 화면마다 다르다 (홈은 치지직 원본, 아카이브는 클립 페이지).

  모달 안의 두 주소는 href 가 아니라 clipId 에서 만든다.
  그래야 어느 화면에서 열었든 같은 두 곳으로 간다.

  마크업은 layout/clip-modal.jsp, 모양은 css/components/clip-modal.css 다.
*/

function initClipModal() {
    $(document).on('click', '.clip-card', function(e) {
        const clipId = $(this).attr('data-clip-id');
        if (!clipId) return;
        if (e.metaKey || e.ctrlKey || e.shiftKey || e.which === 2) return;

        e.preventDefault();
        openClipModal(clipId, $(this).attr('data-clip-title'));
    });

    // common.js 가 모달을 닫아도 iframe 은 남아 소리가 계속 난다 — 같은 신호로 src 를 거둔다
    $(document).on('click', '#clipModal', function(e) {
        if ($(e.target).is('#clipModal')) clearClipFrame();
    });
    $(document).on('click', '[data-close-modal="clipModal"]', clearClipFrame);
    $(document).on('keydown', function(e) {
        if (e.key === 'Escape') clearClipFrame();
    });
}

function openClipModal(clipId, title) {
    const id = encodeURIComponent(clipId);

    $('#clipModalTitle').text(title || '클립');
    $('#clipModalPage').attr('href', '/clips/' + id);
    $('#clipModalOrigin').attr('href', 'https://chzzk.naver.com/clips/' + id);
    $('#clipModalFrame').attr('src', 'https://chzzk.naver.com/embed/clip/' + id);

    YetiUtil.openModal('clipModal');
}

// src를 비워 플레이어를 완전히 내린다 (재생 중단)
function clearClipFrame() {
    $('#clipModalFrame').attr('src', '');
}
