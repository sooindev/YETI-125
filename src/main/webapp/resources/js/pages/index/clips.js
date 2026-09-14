/* 홈 — 인기 클립. 목록과 더보기.
   카드를 누르면 뜨는 모달은 components/clip-modal.js 가 맡는다 (아카이브와 공용) */

/*
  홈에서 더보기로 늘릴 수 있는 최대치. 3열 그리드라 여섯 줄이고, 처음 6개에 더보기 두 번이다.

  상한이 필요한 이유는 아카이브(/clips)가 생겼기 때문이다. 전에는 여기서 3,000개까지
  끝없이 늘어났다 — 홈이 목록 화면 노릇까지 하면 둘 다 어중간해진다.
  상한에 닿으면 버튼이 "아카이브에서 전체 보기" 로 바뀐다.
*/
const HOME_CLIP_MAX = 18;

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
                    syncClipsMore();
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
                    syncClipsMore();
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

/**
 * 더보기 자리에 무엇을 세울지 정한다.
 *
 * 더 볼 것이 없으면 아무것도, 상한 전이면 더보기, 상한에 닿으면 아카이브 버튼.
 * 상한에 닿았는데 더 볼 것도 없는 경우는 둘 다 감춘다 — 아카이브에 가도 같은 목록이다.
 */
function syncClipsMore() {
    if (!hasMoreClips) {
        $('#clipsMore').hide();
        return;
    }

    const capped = clipOffset >= HOME_CLIP_MAX;

    $('#clipsMore').show();
    $('#loadMoreBtn').toggle(!capped);
    $('#clipsArchiveLink').prop('hidden', !capped);
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
