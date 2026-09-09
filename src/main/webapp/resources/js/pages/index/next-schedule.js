/* 홈 — 다음 방송. 오프라인일 때 히어로 아래에 남은 시간을 센다 */

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
