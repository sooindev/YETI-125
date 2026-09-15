/*
  썸네일 한 칸 — 클립 카드와 다시보기 카드가 함께 쓴다.
  홈(index.jsp)과 클립 아카이브(clips.jsp) 양쪽이 부르므로, 카드를 그리는
  스크립트(clips.js · videos.js)보다 먼저 실려야 한다.
*/

/**
 * 썸네일 한 칸. 19금은 치지직이 이미지 주소를 주지 않는다(null).
 * <img src=""> 는 브라우저가 현재 페이지를 이미지로 받아 깨진 아이콘이 되므로,
 * 주소가 없으면 img 대신 대체 자리를 그린다
 */
function thumbnailHtml(url, alt) {
    const src = YetiUtil.attrUrl(url);
    if (!src) return '';
    return '<img src="' + src + '" alt="' + YetiUtil.escapeHtml(alt) + '" loading="lazy">';
}

/** 썸네일 대체 자리. 19금이면 이유 표시 */
function thumbFallbackHtml(adult) {
    if (adult) {
        return '<div class="thumb-fallback" role="img" aria-label="연령 제한 콘텐츠 — 썸네일 비공개">' +
            '<span class="thumb-fallback-badge">19</span>' +
            '<span class="thumb-fallback-note">연령 제한</span>' +
            '</div>';
    }
    return '<div class="thumb-fallback" role="img" aria-label="썸네일 없음">' +
        '<span class="thumb-fallback-note">No Thumbnail</span>' +
        '</div>';
}

/** img 와 대체 자리 중 하나 */
function thumbnailOrFallback(url, alt, adult) {
    return thumbnailHtml(url, alt) || thumbFallbackHtml(adult);
}
