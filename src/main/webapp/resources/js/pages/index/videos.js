/* 홈 — 다시보기. 월별로 묶어 보여준다 */

let videoOffset = 0;
let hasMoreVideos = false;

function loadVideos() {
    videoOffset = 0;

    $('#videosLoading').show();
    $('#videosEmpty').hide();

    $.ajax({
        url: '/live/videos',
        type: 'GET',
        data: { limit: 6, offset: 0 },
        dataType: 'json',
        timeout: 10000,
        success: function(response) {
            $('#videosLoading').hide();

            if (response.success && response.data) {
                const videos = response.data.videos;
                hasMoreVideos = response.data.hasMore;
                videoOffset = response.data.nextOffset || 6;

                if (videos && videos.length > 0) {
                    renderVideos(videos, false);

                    if (hasMoreVideos) {
                        $('#videosMore').show();
                    } else {
                        $('#videosMore').hide();
                    }
                } else {
                    $('#videosEmpty').show();
                }
            } else {
                $('#videosEmpty').show();
            }
        },
        error: function() {
            $('#videosLoading').hide();
            $('#videosEmpty').show();
        }
    });
}

function loadMoreVideos() {
    if (!hasMoreVideos) return;

    const $btn = $('#loadMoreVideosBtn');
    $btn.prop('disabled', true).text('불러오는 중...');

    $.ajax({
        url: '/live/videos',
        type: 'GET',
        data: {
            limit: 6,
            offset: videoOffset
        },
        dataType: 'json',
        timeout: 10000,
        success: function(response) {
            $btn.prop('disabled', false).text('더보기');

            if (response.success && response.data) {
                const videos = response.data.videos;
                hasMoreVideos = response.data.hasMore;
                videoOffset = response.data.nextOffset;

                if (videos && videos.length > 0) {
                    renderVideos(videos, true);

                    if (!hasMoreVideos) {
                        $('#videosMore').hide();
                    }
                } else {
                    $('#videosMore').hide();
                }
            }
        },
        error: function() {
            $btn.prop('disabled', false).text('더보기');
        }
    });
}

// 치지직이 VOD 임베드를 지원하지 않아 나가기 전에 한 번 알린다
const VIDEO_SKIP_KEY = 'yeti-video-leave';

// localStorage 가 막히면 읽기는 "묻는다"로, 쓰기는 조용히 넘어간다
function skipVideoConfirm() {
    try {
        return localStorage.getItem(VIDEO_SKIP_KEY) === 'skip';
    } catch (e) {
        return false;
    }
}

function rememberVideoSkip() {
    try {
        localStorage.setItem(VIDEO_SKIP_KEY, 'skip');
    } catch (e) {}
}

function forgetVideoSkip() {
    try {
        localStorage.removeItem(VIDEO_SKIP_KEY);
    } catch (e) {}
}

// 되돌릴 길은 꺼져 있을 때만 보여준다
function syncVideoRestoreLink() {
    $('#videoConfirmRestore').prop('hidden', !skipVideoConfirm());
}

function initVideoModal() {
    $(document).on('click', '.video-card', function(e) {
        const url = $(this).attr('href');
        if (!url) return;
        // 새 탭 열기(⌘/Ctrl/Shift/휠 클릭)는 묻지 않고 그대로 보낸다
        if (e.metaKey || e.ctrlKey || e.shiftKey || e.which === 2) return;
        // 다시 묻지 않기를 고른 사람도 그대로 보낸다
        if (skipVideoConfirm()) return;

        e.preventDefault();
        $('#videoModalTitle').text($(this).attr('data-video-title') || '다시보기');
        $('#videoModalGo').attr('href', url);
        // 체크는 매번 풀린 상태로 시작한다 — 켜져 있었다면 여기까지 오지 않는다
        $('#videoModalSkip').prop('checked', false);
        YetiUtil.openModal('videoModal');
    });

    // 기억은 실제로 이동할 때만 한다 — 체크하고 취소한 것은 "묻지 말라"가 아니다
    $(document).on('click', '#videoModalGo', function() {
        if ($('#videoModalSkip').prop('checked')) {
            rememberVideoSkip();
            syncVideoRestoreLink();
        }
        YetiUtil.closeModal('videoModal');
    });

    $(document).on('click', '#videoConfirmRestore', function() {
        forgetVideoSkip();
        syncVideoRestoreLink();
        if (typeof showToast === 'function') {
            showToast('이동 확인을 다시 켰습니다', 'success');
        }
    });

    syncVideoRestoreLink();
}

// 더보기로 이어 받을 때 같은 달 머리말을 다시 찍지 않도록 마지막 값을 들고 있는다
let lastVideoGroup = '';

/** "2026-08" — 정렬·비교용 키 */
function videoGroupKey(publishDate) {
    const at = YetiUtil.parseDate(publishDate);
    if (!at) return '';
    return at.getFullYear() + '-' + ('0' + (at.getMonth() + 1)).slice(-2);
}

/** "2026년 8월" — 화면에 찍는 이름 */
function videoGroupLabel(key) {
    const parts = key.split('-');
    return parts[0] + '년 ' + parseInt(parts[1], 10) + '월';
}

function renderVideos(videos, append) {
    const $container = $('#videosContainer');

    if (!append) {
        $container.empty();
        lastVideoGroup = '';
    }

    $.each(videos, function(index, video) {
        // 달이 바뀌는 자리에 머리말을 끼운다. 날짜를 못 읽은 항목은 앞 묶음에 남긴다
        const group = videoGroupKey(video.publishDate);
        if (group && group !== lastVideoGroup) {
            lastVideoGroup = group;
            $container.append(
                '<h3 class="video-group">' + YetiUtil.escapeHtml(videoGroupLabel(group)) + '</h3>'
            );
        }

        const duration = formatVideoDuration(video.duration);
        const viewCount = YetiUtil.numberFormat(video.readCount || 0);
        const date = YetiUtil.formatMonthDay(video.publishDate);

        const videoHtml =
            '<a href="' + YetiUtil.attrUrl(video.videoUrl) + '" target="_blank" rel="noopener"' +
            ' class="video-card scroll-animate scale-in"' +
            ' data-video-title="' + YetiUtil.escapeHtml(video.videoTitle) + '">' +
            '<div class="video-thumbnail">' +
            thumbnailOrFallback(video.thumbnailUrl, video.videoTitle, video.adult) +
            '<span class="video-duration">' + duration + '</span>' +
            '<div class="video-play-overlay">' +
            '<div class="video-play-icon">▶</div>' +
            '</div>' +
            '</div>' +
            '<div class="video-info">' +
            '<h3 class="video-title">' + YetiUtil.escapeHtml(video.videoTitle) + '</h3>' +
            '<div class="video-meta">' +
            '<span class="video-meta-item">👁 ' + viewCount + '</span>' +
            '<span class="video-meta-item">📅 ' + date + '</span>' +
            '</div>' +
            '</div>' +
            '</a>';

        $container.append(videoHtml);
    });

    setTimeout(function() {
        if (typeof window.observeNewElements === 'function') {
            window.observeNewElements();
        }
    }, 50);
}

function formatVideoDuration(seconds) {
    if (!seconds) return '0:00';

    const hours = Math.floor(seconds / 3600);
    const mins = Math.floor((seconds % 3600) / 60);
    const secs = seconds % 60;

    if (hours > 0) {
        return hours + ':' + String(mins).padStart(2, '0') + ':' + String(secs).padStart(2, '0');
    }
    return mins + ':' + String(secs).padStart(2, '0');
}
