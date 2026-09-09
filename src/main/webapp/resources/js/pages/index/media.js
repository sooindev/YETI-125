/* 홈 — 썸네일 한 칸. 클립과 다시보기가 함께 쓴다 (clips.js 보다 먼저 실려야 한다) */

/**
 * 썸네일 한 칸. 19금 방송·다시보기는 치지직이 이미지 주소를 아예 주지 않는다(null).
 * 그대로 <img src=""> 를 내면 브라우저가 현재 페이지를 이미지로 받아 깨진 아이콘을 그리므로,
 * 주소가 없으면 img 자체를 내지 않고 대체 자리를 그린다.
 */
function thumbnailHtml(url, alt) {
    const src = YetiUtil.attrUrl(url);
    if (!src) return '';
    return '<img src="' + src + '" alt="' + YetiUtil.escapeHtml(alt) + '" loading="lazy">';
}

/** 썸네일이 없을 때 채우는 자리. 19금이면 이유를 밝혀 준다 */
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

/** img 와 대체 자리 중 맞는 쪽 하나 */
function thumbnailOrFallback(url, alt, adult) {
    return thumbnailHtml(url, alt) || thumbFallbackHtml(adult);
}
