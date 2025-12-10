/* ================================================
   Irion Fansite - Index Page (jQuery)
   ================================================ */

$(document).ready(function() {
    // 방송 상태 체크
    checkLiveStatus();

    // 60초마다 방송 상태 갱신
    setInterval(checkLiveStatus, 60000);
});

// 방송 상태 체크
function checkLiveStatus() {
    $.ajax({
        url: '/live/status',
        type: 'GET',
        dataType: 'json',
        success: function(response) {
            console.log('Live status:', response);

            if (response.success && response.data) {
                const data = response.data;

                if (data.isLive) {
                    showLiveHero(data);
                } else {
                    showDefaultHero();
                }
            } else {
                showDefaultHero();
            }
        },
        error: function(xhr, status, error) {
            console.log('Live status check error:', error);
            showDefaultHero();
        }
    });
}

// 방송 중 - 라이브 히어로 표시
function showLiveHero(data) {
    // 제목
    $('#liveTitle').text(data.liveTitle || '이리온 방송 중!');

    // 링크
    $('#liveLink').attr('href', data.channelUrl);

    // 썸네일
    if (data.thumbnail) {
        $('#liveThumbnail').attr('src', data.thumbnail);
    }

    // 시청자 수
    if (data.viewerCount) {
        $('#liveViewers').text('👤 ' + numberFormat(data.viewerCount) + '명 시청 중');
    }

    // 히어로 전환
    $('#defaultHero').hide();
    $('#liveHero').fadeIn();
}

// 방송 안 함 - 기본 히어로 표시
function showDefaultHero() {
    $('#liveHero').hide();
    $('#defaultHero').fadeIn();
}

// 숫자 포맷 (1000 -> 1,000)
function numberFormat(num) {
    return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',');
}