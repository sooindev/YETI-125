/* 홈 — 배선. 화면이 준비되면 각 구역을 불러온다.
   함수는 다른 파일에 있다 — ready 는 스크립트가 모두 실린 뒤에 돈다 */

$(document).ready(function() {
    checkLiveStatus();
    loadClips();
    loadVideos();
    setInterval(checkLiveStatus, 60000);

    $('#loadMoreBtn').on('click', function() {
        loadMoreClips();
    });

    $('#loadMoreVideosBtn').on('click', function() {
        loadMoreVideos();
    });

    initClipModal();
    initVideoModal();
});
