/* ================================================
   Irion Fansite - Index Page (jQuery)
   ================================================ */

var clipOffset = 0;
var hasMoreClips = false;

$(document).ready(function() {
    checkLiveStatus();
    loadClips();
    setInterval(checkLiveStatus, 60000);

    $('#loadMoreBtn').on('click', function() {
        loadMoreClips();
    });
});

function checkLiveStatus() {
    $.ajax({
        url: '/live/status',
        type: 'GET',
        dataType: 'json',
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

    $.ajax({
        url: '/live/clips',
        type: 'GET',
        data: { limit: 6, offset: 0 },
        dataType: 'json',
        success: function(response) {
            console.log('Clips response:', response);

            $('#clipsLoading').hide();

            if (response.success && response.data) {
                var clips = response.data.clips;
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

    var $btn = $('#loadMoreBtn');
    $btn.prop('disabled', true).text('불러오는 중...');

    $.ajax({
        url: '/live/clips',
        type: 'GET',
        data: {
            limit: 6,
            offset: clipOffset
        },
        dataType: 'json',
        success: function(response) {
            $btn.prop('disabled', false).text('더보기');

            if (response.success && response.data) {
                var clips = response.data.clips;
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
    var $container = $('#clipsContainer');

    if (!append) {
        $container.empty();
    }

    $.each(clips, function(index, clip) {
        var duration = formatDuration(clip.duration);
        var viewCount = numberFormat(clip.viewCount || 0);
        var date = formatDate(clip.createdAt);

        var clipHtml =
            '<a href="' + clip.clipUrl + '" target="_blank" class="clip-card">' +
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
}

function formatDuration(seconds) {
    if (!seconds) return '0:00';
    var mins = Math.floor(seconds / 60);
    var secs = seconds % 60;
    return mins + ':' + String(secs).padStart(2, '0');
}

function formatDate(dateStr) {
    if (!dateStr) return '';
    try {
        var date = new Date(dateStr);
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