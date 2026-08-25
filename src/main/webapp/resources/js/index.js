/* 홈 화면 — 방송 상태, 클립, 다시보기 */

let clipOffset = 0;
let hasMoreClips = false;

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

function checkLiveStatus() {
    $.ajax({
        url: '/live/status',
        type: 'GET',
        dataType: 'json',
        timeout: 10000,
        success: function(response) {
            if (response.success && response.data) {
                if (response.data.isLive) {
                    showLiveHero(response.data);
                } else {
                    showDefaultHero();
                }
            } else {
                showDefaultHero();
            }
        },
        error: function() {
            showDefaultHero();
        }
    });
}

function showLiveHero(data) {
    $('#liveTitle').text(data.liveTitle || '이리온 방송 중!');
    $('#liveLink').attr('href', YetiUtil.safeUrl(data.channelUrl));

    // .attr() 은 값을 그대로 넣는다. 스킴은 여기서도 확인한다.
    const thumbnail = YetiUtil.safeUrl(data.thumbnail);
    if (thumbnail) {
        $('#liveThumbnail').attr('src', thumbnail);
    }

    if (data.viewerCount) {
        $('#liveViewers').text('👤 ' + YetiUtil.numberFormat(data.viewerCount) + '명 시청 중');
    }

    $('#defaultHero').hide();
    $('#liveHero').fadeIn();
}

function showDefaultHero() {
    $('#liveHero').hide();
    $('#defaultHero').fadeIn();
    loadNextSchedule();
}

// 다음 방송 — 오프라인일 때만 쓴다. 방송 중이면 히어로가 통째로 바뀐다.
let nextScheduleTimer = null;

function loadNextSchedule() {
    const today = new Date();
    const until = new Date(today.getTime() + 120 * 24 * 60 * 60 * 1000);

    $.ajax({
        url: '/schedule/list',
        type: 'GET',
        dataType: 'json',
        timeout: 10000,
        data: {
            start: YetiUtil.formatDate(today, 'date'),
            end: YetiUtil.formatDate(until, 'date')
        },
        success: function (events) {
            showNextSchedule(pickNextSchedule(events));
        }
        // 실패하면 기본 문구가 그대로 남는다
    });
}

/** 아직 시작하지 않은 일정 중 가장 이른 것 */
function pickNextSchedule(events) {
    if (!$.isArray(events)) return null;

    const now = Date.now();
    let best = null;
    let bestAt = Infinity;

    $.each(events, function (i, event) {
        const at = YetiUtil.parseDate(event.start);
        if (!at || at.getTime() <= now) return;
        if (at.getTime() < bestAt) {
            bestAt = at.getTime();
            best = event;
        }
    });
    return best;
}

function showNextSchedule(event) {
    if (nextScheduleTimer) {
        clearInterval(nextScheduleTimer);
        nextScheduleTimer = null;
    }
    if (!event) return;

    const at = YetiUtil.parseDate(event.start);
    if (!at) return;

    $('#heroNextTitle').text(event.title || '');
    $('#heroOfflineCopy').hide();
    $('#heroNext').prop('hidden', false);

    const tick = function () {
        const left = at.getTime() - Date.now();
        if (left <= 0) {
            // 시작 시각을 넘겼다 — 그 다음 일정으로 넘어간다
            clearInterval(nextScheduleTimer);
            nextScheduleTimer = null;
            loadNextSchedule();
            return;
        }
        $('#heroNextCountdown').text(formatTimeLeft(left));
    };

    tick();
    // 분 단위로만 보여주므로 30초면 충분하다
    nextScheduleTimer = setInterval(tick, 30000);
}

/** 남은 시간을 "2일 4시간" / "3시간 20분" / "12분" 으로 */
function formatTimeLeft(ms) {
    const totalMin = Math.floor(ms / 60000);
    const days = Math.floor(totalMin / 1440);
    const hours = Math.floor((totalMin % 1440) / 60);
    const mins = totalMin % 60;

    if (days > 0) return hours > 0 ? days + '일 ' + hours + '시간' : days + '일';
    if (hours > 0) return mins > 0 ? hours + '시간 ' + mins + '분' : hours + '시간';
    if (mins > 0) return mins + '분';
    return '곧 시작';
}

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
            '<img src="' + YetiUtil.attrUrl(clip.thumbnailUrl) + '" alt="' + YetiUtil.escapeHtml(clip.clipTitle) + '" loading="lazy">' +
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
            '<img src="' + YetiUtil.attrUrl(video.thumbnailUrl) + '" alt="' + YetiUtil.escapeHtml(video.videoTitle) + '" loading="lazy">' +
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