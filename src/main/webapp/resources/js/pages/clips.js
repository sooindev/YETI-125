/*
  클립 아카이브 목록 (/clips).

  카드를 누르면 홈과 똑같이 모달에서 재생한다 (components/clip-modal.js).
  1,700개를 훑는 화면에서 한 편 볼 때마다 페이지를 새로 여는 것은 너무 무겁다.

  다만 카드의 href 는 치지직이 아니라 <b>클립의 주소</b>(/clips/{clipId})다.
  ⌘·Ctrl 클릭이나 "링크 주소 복사" 가 그 주소를 집어가고, 크롤러도 그 길로 들어간다 —
  이 화면이 있는 이유가 클립마다 주소를 주는 것이라서다.

  홈과 다른 점은 정렬·검색이 붙는다는 것이다.

  정렬과 검색어는 주소(?sort=&q=)에 남긴다. 새로고침해도 같은 화면이 나오고,
  그 주소를 그대로 남에게 보낼 수 있다. 스크립트가 죽어도 검색 폼이 GET 으로
  같은 주소를 만들어 주므로 첫 화면은 나온다.
*/

const CLIPS_PAGE_SIZE = 24;

let archiveOffset = 0;
let archiveHasMore = false;
let archiveSort = 'popular';
let archiveQuery = '';

/** 목록을 받는 중인가. 더보기 연타로 같은 구간을 두 번 붙이지 않게 막는다 */
let archiveLoading = false;

$(document).ready(function() {
    readStateFromUrl();
    syncControls();
    initControls();
    initClipModal();
    loadArchive(false);
});

/* ===== 주소 ↔ 화면 상태 ===== */

function readStateFromUrl() {
    const params = new URLSearchParams(window.location.search);

    archiveSort = (params.get('sort') === 'latest') ? 'latest' : 'popular';
    archiveQuery = (params.get('q') || '').trim().slice(0, 50);
}

/** 목록을 다시 받을 때마다 주소를 맞춘다. 기록을 쌓지 않으려고 push 가 아니라 replace 다 */
function writeStateToUrl() {
    const params = new URLSearchParams();

    if (archiveSort === 'latest') params.set('sort', 'latest');
    if (archiveQuery) params.set('q', archiveQuery);

    const query = params.toString();
    window.history.replaceState(null, '', query ? '/clips?' + query : '/clips');
}

/** 주소에서 읽은 상태를 버튼과 입력칸에 반영한다 */
function syncControls() {
    $('#clipSearch').val(archiveQuery);
    $('.clips-sort-btn').each(function() {
        $(this).toggleClass('is-active', $(this).attr('data-sort') === archiveSort);
    });
}

function initControls() {
    $('.clips-sort-btn').on('click', function() {
        const sort = $(this).attr('data-sort');
        if (sort === archiveSort) return;

        archiveSort = sort;
        syncControls();
        writeStateToUrl();
        loadArchive(false);
    });

    // 폼 제출을 가로챈다 — 가로채지 못하면 페이지가 통째로 다시 뜨지만 결과는 같다
    $('#clipSearchForm').on('submit', function(e) {
        e.preventDefault();

        const next = ($('#clipSearch').val() || '').trim().slice(0, 50);
        if (next === archiveQuery) return;

        archiveQuery = next;
        writeStateToUrl();
        loadArchive(false);
    });

    $('#loadMoreBtn').on('click', function() {
        loadArchive(true);
    });
}

/* ===== 목록 ===== */

function loadArchive(append) {
    if (archiveLoading) return;
    archiveLoading = true;

    if (!append) {
        archiveOffset = 0;
        $('#clipsContainer').empty();
        $('#clipsCount').prop('hidden', true);
        $('#clipsMore').hide();
        $('#clipsEmpty').hide();
        $('#clipsLoading').show();
    } else {
        $('#loadMoreBtn').prop('disabled', true).text('불러오는 중...');
    }

    $.ajax({
        url: '/live/clips',
        type: 'GET',
        data: {
            limit: CLIPS_PAGE_SIZE,
            offset: append ? archiveOffset : 0,
            sort: archiveSort,
            q: archiveQuery
        },
        dataType: 'json',
        // 검색·최신순은 목록 전량을 받아야 해서 첫 요청이 길다 (캐시가 식었을 때만)
        timeout: 20000,
        success: function(response) {
            if (!response.success || !response.data) {
                showArchiveFailure(append);
                return;
            }

            const clips = response.data.clips || [];
            archiveHasMore = !!response.data.hasMore;
            archiveOffset = response.data.nextOffset || (archiveOffset + clips.length);

            renderArchive(clips, append);

            if (!append && clips.length === 0) {
                $('#clipsEmpty').show();
            }
            showCount(response.data.total);
            $('#clipsMore').toggle(archiveHasMore);
        },
        error: function() {
            showArchiveFailure(append);
        },
        complete: function() {
            archiveLoading = false;
            $('#clipsLoading').hide();
            $('#loadMoreBtn').prop('disabled', false).text('더보기');
        }
    });
}

/** 전량을 놓고 고른 경우에만 개수를 안다 (인기순 기본 화면은 이어 받는 중이라 모른다) */
function showCount(total) {
    if (typeof total !== 'number') {
        $('#clipsCount').prop('hidden', true);
        return;
    }

    const label = archiveQuery
        ? '"' + archiveQuery + '" — 클립 ' + YetiUtil.numberFormat(total) + '개'
        : '클립 ' + YetiUtil.numberFormat(total) + '개';

    $('#clipsCount').text(label).prop('hidden', false);
}

function showArchiveFailure(append) {
    if (append) {
        showToast('더 불러오지 못했습니다', 'error');
        return;
    }
    $('#clipsEmpty').show();
}

function renderArchive(clips, append) {
    const $container = $('#clipsContainer');

    if (!append) {
        $container.empty();
    }

    $.each(clips, function(index, clip) {
        const duration = formatArchiveDuration(clip.duration);
        const viewCount = YetiUtil.numberFormat(clip.viewCount || 0);
        const date = YetiUtil.formatMonthDay(clip.createdAt);

        // 평범한 클릭은 clip-modal.js 가 가로채 모달을 띄운다.
        // href 가 살아 있어야 ⌘ 클릭 · 링크 복사 · 크롤러가 클립 주소로 간다
        const href = '/clips/' + encodeURIComponent(clip.clipId);

        $container.append(
            '<a href="' + YetiUtil.attrUrl(href) + '"' +
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
            '<h2 class="clip-title">' + YetiUtil.escapeHtml(clip.clipTitle) + '</h2>' +
            '<div class="clip-meta">' +
            '<span class="clip-meta-item">👁 ' + viewCount + '</span>' +
            '<span class="clip-meta-item">📅 ' + date + '</span>' +
            '</div>' +
            '</div>' +
            '</a>'
        );
    });

    setTimeout(function() {
        if (typeof window.observeNewElements === 'function') {
            window.observeNewElements();
        }
    }, 50);
}

function formatArchiveDuration(seconds) {
    if (!seconds) return '0:00';

    const hours = Math.floor(seconds / 3600);
    const mins = Math.floor((seconds % 3600) / 60);
    const secs = seconds % 60;

    if (hours > 0) {
        return hours + ':' + String(mins).padStart(2, '0') + ':' + String(secs).padStart(2, '0');
    }
    return mins + ':' + String(secs).padStart(2, '0');
}
