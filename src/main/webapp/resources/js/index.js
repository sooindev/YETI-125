/* ================================================
   Irion Fansite - Index Page (jQuery)
   ================================================ */

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
    $('#liveLink').attr('href', data.channelUrl);

    if (data.thumbnail) {
        $('#liveThumbnail').attr('src', data.thumbnail);
    }

    if (data.viewerCount) {
        $('#liveViewers').text('👤 ' + numberFormat(data.viewerCount) + '명 시청 중');
    }

    $('#defaultHero').hide();
    $('#liveHero').fadeIn();
}

function showDefaultHero() {
    $('#liveHero').hide();
    $('#defaultHero').fadeIn();
}

// 클립 로드 (초기)
function loadClips() {
    clipOffset = 0;

    // 로딩 표시
    $('#clipsLoading').show();
    $('#clipsEmpty').hide();

    $.ajax({
        url: '/live/clips',
        type: 'GET',
        data: { limit: 6, offset: 0 },
        dataType: 'json',
        timeout: 10000,
        success: function(response) {
            console.log('Clips response:', response);

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

// 클립 더보기
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

function renderClips(clips, append) {
    const $container = $('#clipsContainer');

    if (!append) {
        $container.empty();
    }

    $.each(clips, function(index, clip) {
        const duration = formatDuration(clip.duration);
        const viewCount = numberFormat(clip.viewCount || 0);
        const date = formatDate(clip.createdAt);

        const clipHtml =
            '<a href="' + clip.clipUrl + '" target="_blank" class="clip-card scroll-animate scale-in">' +
            '<div class="clip-thumbnail">' +
            '<img src="' + (clip.thumbnailUrl || '') + '" alt="' + escapeHtml(clip.clipTitle) + '">' +
            '<span class="clip-duration">' + duration + '</span>' +
            '<div class="clip-play-overlay">' +
            '<div class="clip-play-icon">▶</div>' +
            '</div>' +
            '</div>' +
            '<div class="clip-info">' +
            '<h3 class="clip-title">' + escapeHtml(clip.clipTitle) + '</h3>' +
            '<div class="clip-meta">' +
            '<span class="clip-meta-item">👁 ' + viewCount + '</span>' +
            '<span class="clip-meta-item">📅 ' + date + '</span>' +
            '</div>' +
            '</div>' +
            '</a>';

        $container.append(clipHtml);
    });

    // DOM 렌더링 완료 후 애니메이션 적용
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

function formatDate(dateStr) {
    if (!dateStr) return '';
    try {
        const date = new Date(dateStr);
        return (date.getMonth() + 1) + '월 ' + date.getDate() + '일';
    } catch (e) {
        return '';
    }
}

function numberFormat(num) {
    return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',');
}

function escapeHtml(text) {
    if (!text) return '';
    return text
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

let videoOffset = 0;
let hasMoreVideos = false;

// 다시보기 로드 (초기)
function loadVideos() {
    videoOffset = 0;

    // 로딩 표시
    $('#videosLoading').show();
    $('#videosEmpty').hide();

    $.ajax({
        url: '/live/videos',
        type: 'GET',
        data: { limit: 6, offset: 0 },
        dataType: 'json',
        timeout: 10000,
        success: function(response) {
            console.log('Videos response:', response);

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

// 다시보기 더보기
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

// 다시보기 렌더링
function renderVideos(videos, append) {
    const $container = $('#videosContainer');

    if (!append) {
        $container.empty();
    }

    $.each(videos, function(index, video) {
        const duration = formatVideoDuration(video.duration);
        const viewCount = numberFormat(video.readCount || 0);
        const date = formatDate(video.publishDate);

        const videoHtml =
            '<a href="' + video.videoUrl + '" target="_blank" class="video-card scroll-animate scale-in">' +
            '<div class="video-thumbnail">' +
            '<img src="' + (video.thumbnailUrl || '') + '" alt="' + escapeHtml(video.videoTitle) + '">' +
            '<span class="video-duration">' + duration + '</span>' +
            '<div class="video-play-overlay">' +
            '<div class="video-play-icon">▶</div>' +
            '</div>' +
            '</div>' +
            '<div class="video-info">' +
            '<h3 class="video-title">' + escapeHtml(video.videoTitle) + '</h3>' +
            '<div class="video-meta">' +
            '<span class="video-meta-item">👁 ' + viewCount + '</span>' +
            '<span class="video-meta-item">📅 ' + date + '</span>' +
            '</div>' +
            '</div>' +
            '</a>';

        $container.append(videoHtml);
    });

    // DOM 렌더링 완료 후 애니메이션 적용
    setTimeout(function() {
        if (typeof window.observeNewElements === 'function') {
            window.observeNewElements();
        }
    }, 50);
}

// 영상 시간 포맷 (초 -> HH:MM:SS 또는 MM:SS)
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