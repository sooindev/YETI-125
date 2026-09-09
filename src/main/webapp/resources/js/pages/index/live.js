/* 홈 — 방송 상태. 1분마다 다시 물어본다 */

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
    const title = data.liveTitle || '이리온 방송 중!';
    // 제목은 CSS 에서 세 줄에 자른다. 잘린 뒤를 마우스로도 볼 수 있게 원문을 남긴다
    $('#liveTitle').text(title).attr('title', title);
    $('#liveLink').attr('href', YetiUtil.safeUrl(data.channelUrl));

    // .attr() 은 값을 그대로 넣는다. 스킴은 여기서도 확인한다.
    const thumbnail = YetiUtil.safeUrl(data.thumbnail);
    if (thumbnail) {
        $('#liveThumbFallback').empty();
        $('#liveThumbnail').attr('src', thumbnail).show();
    } else {
        // 19금 방송이면 치지직이 주소를 주지 않는다. 빈 src 를 남기면 깨진 이미지가 뜬다.
        $('#liveThumbnail').removeAttr('src').hide();
        $('#liveThumbFallback').html(thumbFallbackHtml(data.adult));
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
