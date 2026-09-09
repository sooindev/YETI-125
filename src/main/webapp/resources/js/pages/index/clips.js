/* 홈 — 인기 클립. 목록·더보기·모달 */

let clipOffset = 0;
let hasMoreClips = false;

function loadClips() {
    clipOffset = 0;

    $('#clipsLoading').show();
    $('#clipsEmpty').hide();

    $.ajax({
        url: '/live/clips',
        type: 'GET',
        data: { limit: 6, offset: 0 },
        dataType: 'json',
        timeout: 10000,
        success: function(response) {
            $('#clipsLoading').hide();

            if (response.success && response.data) {
                const clips = response.data.clips;
                hasMoreClips = response.data.hasMore;
                clipOffset = response.data.nextOffset || 6;

                if (clips && clips.length > 0) {
                    renderClips(clips, false);

                    if (hasMoreClips) {
                        $('#clipsMore').show();
                    } else {
                        $('#clipsMore').hide();
                    }
                } else {
                    $('#clipsEmpty').show();
                }
            } else {
                $('#clipsEmpty').show();
            }
        },
        error: function() {
            $('#clipsLoading').hide();
            $('#clipsEmpty').show();
        }
    });
}

function loadMoreClips() {
    if (!hasMoreClips) return;

    const $btn = $('#loadMoreBtn');
    $btn.prop('disabled', true).text('불러오는 중...');

    $.ajax({
        url: '/live/clips',
        type: 'GET',
        data: {
            limit: 6,
            offset: clipOffset
        },
        dataType: 'json',
        timeout: 10000,
        success: function(response) {
            $btn.prop('disabled', false).text('더보기');

            if (response.success && response.data) {
                const clips = response.data.clips;
                hasMoreClips = response.data.hasMore;
                clipOffset = response.data.nextOffset;

                if (clips && clips.length > 0) {
                    renderClips(clips, true);

                    if (!hasMoreClips) {
                        $('#clipsMore').hide();
                    }
                } else {
                    $('#clipsMore').hide();
                }
            }
        },
        error: function() {
            $btn.prop('disabled', false).text('더보기');
        }
    });
}

// 클립 모달 — 치지직 공식 임베드로 사이트 안에서 재생
function initClipModal() {
    // 카드 클릭은 모달로 가로채고, 새 탭 열기는 원본으로 보낸다
    $(document).on('click', '.clip-card', function(e) {
        const clipId = $(this).attr('data-clip-id');
        if (!clipId) return;
        if (e.metaKey || e.ctrlKey || e.shiftKey || e.which === 2) return;

        e.preventDefault();
        openClipModal(clipId, $(this).attr('data-clip-title'), $(this).attr('href'));
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

function openClipModal(clipId, title, originUrl) {
    $('#clipModalTitle').text(title || '클립');
    $('#clipModalOrigin').attr('href', YetiUtil.safeUrl(originUrl) || 'https://chzzk.naver.com/clips/' + encodeURIComponent(clipId));
    $('#clipModalFrame').attr('src', 'https://chzzk.naver.com/embed/clip/' + encodeURIComponent(clipId));
    YetiUtil.openModal('clipModal');
}

function closeClipModal() {
    clearClipFrame();
    YetiUtil.closeModal('clipModal');
}

// src를 비워 플레이어를 완전히 내린다 (재생 중단)
function clearClipFrame() {
    $('#clipModalFrame').attr('src', '');
}

function renderClips(clips, append) {
    const $container = $('#clipsContainer');

    if (!append) {
        $container.empty();
    }

    $.each(clips, function(index, clip) {
        const duration = formatDuration(clip.duration);
        const viewCount = YetiUtil.numberFormat(clip.viewCount || 0);
        const date = YetiUtil.formatMonthDay(clip.createdAt);

        const clipHtml =
            '<a href="' + YetiUtil.attrUrl(clip.clipUrl) + '" target="_blank" rel="noopener"' +
            ' class="clip-card scroll-animate scale-in"' +
            ' data-clip-id="' + YetiUtil.escapeHtml(clip.clipId) + '"' +
            ' data-clip-title="' + YetiUtil.escapeHtml(clip.clipTitle) + '">' +
            '<div class="clip-thumbnail">' +
            thumbnailOrFallback(clip.thumbnailUrl, clip.clipTitle, clip.adult) +
            '<span class="clip-duration">' + duration + '</span>' +
            '<div class="clip-play-overlay">' +
            '<div class="clip-play-icon">▶</div>' +
            '</div>' +
            '</div>' +
            '<div class="clip-info">' +
            '<h3 class="clip-title">' + YetiUtil.escapeHtml(clip.clipTitle) + '</h3>' +
            '<div class="clip-meta">' +
            '<span class="clip-meta-item">👁 ' + viewCount + '</span>' +
            '<span class="clip-meta-item">📅 ' + date + '</span>' +
            '</div>' +
            '</div>' +
            '</a>';

        $container.append(clipHtml);
    });

    setTimeout(function() {
        if (typeof window.observeNewElements === 'function') {
            window.observeNewElements();
        }
    }, 50);
}

function formatDuration(seconds) {
    if (!seconds) return '0:00';
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return mins + ':' + String(secs).padStart(2, '0');
}
