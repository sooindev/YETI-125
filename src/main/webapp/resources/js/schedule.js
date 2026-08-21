/* ================================================
   Irion Fansite - Schedule Calendar (jQuery)
   ================================================ */

let calendar;

$(document).ready(function() {
    initCalendar();
    loadUpcomingEvents();
});

// 캘린더 초기화
function initCalendar() {
    const calendarEl = document.getElementById('calendar');

    calendar = new FullCalendar.Calendar(calendarEl, {
        initialView: 'dayGridMonth',
        locale: 'ko',
        headerToolbar: {
            left: 'prev,next today',
            center: 'title',
            right: 'dayGridMonth,listMonth'
        },
        buttonText: {
            today: '오늘',
            month: '월간',
            list: '목록'
        },
        height: 'auto',
        dayMaxEvents: 3,
        moreLinkText: '개 더보기',
        // 날짜 표시에서 "일" 제거
        dayCellContent: function(arg) {
            return arg.date.getDate();
        },
        events: function(info, successCallback, failureCallback) {
            loadSchedules(info.startStr, info.endStr, successCallback);
        },
        eventClick: function(info) {
            showScheduleDetail(info.event);
        },
        eventDidMount: function(info) {
            $(info.el).attr('title', info.event.title);

            // 일정 타입에 따라 왼쪽 보더 색상 설정
            const type = info.event.extendedProps.type;
            const colors = {
                'JUSTCHAT': '#7fb58a',
                'GAME': '#8c8fd6',
                'KARAOKE': '#d6c07f',
                'COLLAB': '#d68fb0'
            };

            // 타입이 소문자로 올 수도 있으니 대문자로 변환
            const upperType = type ? type.toUpperCase() : '';
            const color = colors[upperType] || '#8c8fd6';
            info.el.style.borderLeftColor = color;
        }
    });

    calendar.render();
}

// 일정 데이터 로드
function loadSchedules(start, end, callback) {
    $.ajax({
        url: '/schedule/list',
        type: 'GET',
        data: {
            start: start.substring(0, 10),
            end: end.substring(0, 10)
        },
        dataType: 'json',
        success: function(data) {
            const events = [];
            $.each(data, function(index, item) {
                events.push({
                    id: item.id,
                    title: item.title,
                    start: item.start,
                    end: item.end,
                    allDay: item.allDay,
                    color: item.color,
                    extendedProps: {
                        description: item.description,
                        type: item.type
                    }
                });
            });
            callback(events);
        },
        error: function(xhr) {
            callback([]);
        }
    });
}

// 다가오는 일정 로드
function loadUpcomingEvents() {
    const today = new Date();
    const start = formatDateToISO(today);

    // 30일 후
    const futureDate = new Date();
    futureDate.setDate(futureDate.getDate() + 30);
    const end = formatDateToISO(futureDate);

    $.ajax({
        url: '/schedule/list',
        type: 'GET',
        data: {
            start: start,
            end: end
        },
        dataType: 'json',
        success: function(data) {
            if (data && data.length > 0) {
                const now = new Date();

                // 이미 지난 일정 필터링
                const futureEvents = data.filter(function(event) {
                    const startDate = new Date(event.start);
                    const endDate = event.end ? new Date(event.end) : null;

                    // 종료 시간이 있으면 종료 시간 기준, 없으면 시작 시간 기준
                    if (endDate) {
                        return endDate > now;
                    } else if (event.allDay) {
                        // 종일 일정은 오늘이거나 미래면 표시
                        const startOfDay = new Date(startDate);
                        startOfDay.setHours(0, 0, 0, 0);
                        const todayStart = new Date(now);
                        todayStart.setHours(0, 0, 0, 0);
                        return startOfDay >= todayStart;
                    } else {
                        return startDate > now;
                    }
                });

                if (futureEvents.length > 0) {
                    // 시작일 기준 정렬
                    futureEvents.sort(function(a, b) {
                        return new Date(a.start) - new Date(b.start);
                    });

                    // 최대 6개만 표시
                    const upcomingEvents = futureEvents.slice(0, 6);
                    renderUpcomingEvents(upcomingEvents);
                    $('#upcomingEmpty').hide();
                } else {
                    $('#upcomingEvents').empty();
                    $('#upcomingEmpty').show();
                }
            } else {
                $('#upcomingEvents').empty();
                $('#upcomingEmpty').show();
            }
        },
        error: function(xhr) {
            $('#upcomingEvents').empty();
            $('#upcomingEmpty').show();
        }
    });
}

/*
 * 다가오는 일정 렌더링
 *
 * 인라인 onclick 에 값을 박아 넣지 않는다. 속성값은 브라우저가 HTML 을
 * 디코드한 뒤에야 JS 로 파싱되므로, escapeHtml 이 만든 &#039; 가 그 시점에
 * 아포스트로피로 되살아나 문자열 리터럴이 끊긴다. 제목에 ' 가 든 일정은
 * onclick 자체가 SyntaxError 라 눌러도 아무 일도 일어나지 않았다.
 *
 * 원본 이벤트는 배열에 그대로 두고 카드에는 인덱스만 남긴다. 이스케이프는
 * 화면에 글자를 찍는 순간 한 번만 한다.
 */
let upcomingEventsData = [];

function renderUpcomingEvents(events) {
    const $container = $('#upcomingEvents');
    $container.empty();
    upcomingEventsData = events.slice();

    $.each(events, function(index, event) {
        const startDate = new Date(event.start);
        const month = getMonthName(startDate.getMonth());
        const day = startDate.getDate();
        const time = event.allDay ? '종일' : formatTime(startDate);
        const typeClass = getTypeClass(event.type);
        const typeName = getScheduleTypeName(event.type);

        const html = `
            <div class="upcoming-item" role="button" tabindex="0" data-upcoming-index="${index}">
                <div class="upcoming-date">
                    <span class="upcoming-month">${month}</span>
                    <span class="upcoming-day">${day}</span>
                </div>
                <div class="upcoming-info">
                    <span class="upcoming-type ${typeClass}">${typeName}</span>
                    <h3 class="upcoming-item-title">${YetiUtil.escapeHtml(event.title)}</h3>
                    <span class="upcoming-time">🕐 ${time}</span>
                </div>
            </div>
        `;

        $container.append(html);
    });
}

// 카드 클릭 / 키보드(Enter, Space) — 인덱스로 원본을 되찾아 상세를 연다
$(document).on('click', '.upcoming-item', function() {
    openUpcomingDetail($(this).attr('data-upcoming-index'));
});

$(document).on('keydown', '.upcoming-item', function(e) {
    if (e.key === 'Enter' || e.key === ' ') {
        e.preventDefault();
        openUpcomingDetail($(this).attr('data-upcoming-index'));
    }
});

function openUpcomingDetail(index) {
    const event = upcomingEventsData[Number(index)];
    if (!event) return;

    showScheduleDetailFrom({
        title: event.title,
        start: event.start,
        end: event.end,
        allDay: event.allDay,
        color: event.color,
        type: event.type,
        description: event.description
    });
}

// 유형별 클래스
function getTypeClass(type) {
    switch (type) {
        case 'COLLAB': return 'type-collab';
        case 'JUSTCHAT': return 'type-justchat';
        case 'GAME': return 'type-game';
        case 'KARAOKE': return 'type-karaoke';
        default: return 'type-game';
    }
}

// 유형 이름
function getScheduleTypeName(type) {
    switch (type) {
        case 'COLLAB': return '합방';
        case 'JUSTCHAT': return '저스트 채팅';
        case 'GAME': return '종합게임';
        case 'KARAOKE': return '노래방송';
        default: return '종합게임';
    }
}

// 월 이름 (짧은 형식)
function getMonthName(month) {
    const months = ['JAN', 'FEB', 'MAR', 'APR', 'MAY', 'JUN', 'JUL', 'AUG', 'SEP', 'OCT', 'NOV', 'DEC'];
    return months[month];
}

// 날짜를 ISO 형식으로 변환
function formatDateToISO(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
}

// 시간 포맷
function formatTime(date) {
    const hours = date.getHours();
    const minutes = String(date.getMinutes()).padStart(2, '0');
    const ampm = hours >= 12 ? '오후' : '오전';
    const displayHours = hours % 12 || 12;
    return `${ampm} ${displayHours}:${minutes}`;
}

// 날짜/시간 포맷
function formatDateTime(date) {
    if (!(date instanceof Date)) {
        date = new Date(date);
    }
    const year = date.getFullYear();
    const month = date.getMonth() + 1;
    const day = date.getDate();
    const time = formatTime(date);
    return `${year}년 ${month}월 ${day}일 ${time}`;
}

// 날짜 포맷 (한국어)
function formatDateKorean(date) {
    if (!(date instanceof Date)) {
        date = new Date(date);
    }
    const year = date.getFullYear();
    const month = date.getMonth() + 1;
    const day = date.getDate();
    return `${year}년 ${month}월 ${day}일`;
}

/*
 * 일정 상세 보기
 *
 * 상세 HTML 을 만드는 곳은 여기 하나뿐이다. 들어오는 값은 항상 원본이고,
 * 이스케이프는 이 함수 안에서만 한다. 이스케이프된 값을 다시 넘기지 말 것.
 */
function showScheduleDetailFrom(data) {
    const typeName = getScheduleTypeName(data.type || 'STREAM');
    const description = data.description || '';

    let dateText = formatDateTime(new Date(data.start));
    if (data.end) {
        dateText += ' ~ ' + formatDateTime(new Date(data.end));
    }
    if (data.allDay) {
        dateText = formatDateKorean(new Date(data.start)) + ' (종일)';
    }

    const html = `
        <div class="detail-header">
            <div class="detail-color" style="background: ${YetiUtil.safeColor(data.color)};"></div>
            <h3 class="detail-title">${YetiUtil.escapeHtml(data.title)}</h3>
        </div>
        <div class="detail-row">
            <span class="detail-icon">📅</span>
            <div class="detail-content">
                <div class="detail-label">일시</div>
                <div class="detail-value">${YetiUtil.escapeHtml(dateText)}</div>
            </div>
        </div>
        <div class="detail-row">
            <span class="detail-icon">🏷️</span>
            <div class="detail-content">
                <div class="detail-label">유형</div>
                <div class="detail-value">${YetiUtil.escapeHtml(typeName)}</div>
            </div>
        </div>
        ${description ? `
        <div class="detail-row">
            <span class="detail-icon">📝</span>
            <div class="detail-content">
                <div class="detail-label">설명</div>
                <div class="detail-value">${YetiUtil.escapeHtml(description)}</div>
            </div>
        </div>
        ` : ''}
    `;

    $('#scheduleDetail').html(html);
    YetiUtil.openModal('scheduleModal');
}

// 캘린더 이벤트 클릭
function showScheduleDetail(event) {
    showScheduleDetailFrom({
        title: event.title,
        start: event.start,
        end: event.end,
        allDay: event.allDay,
        color: event.backgroundColor,
        type: event.extendedProps.type,
        description: event.extendedProps.description
    });
}

// 모달을 열고 닫는 일, 바깥 클릭과 ESC 처리는 common.js (YetiUtil) 가 맡는다.
// 예전에는 여기서 같은 이름의 전역 함수를 다시 정의해 common.js 것을
// 덮어썼다. overflow 복원값도 '' 와 'auto' 로 서로 달랐다.
