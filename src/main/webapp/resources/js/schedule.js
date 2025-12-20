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
                'JUSTCHAT': '#10B981',
                'GAME': '#6366F1',
                'KARAOKE': '#FFC107',
                'COLLAB': '#EC4899'
            };

            // 타입이 소문자로 올 수도 있으니 대문자로 변환
            const upperType = type ? type.toUpperCase() : '';
            const color = colors[upperType] || '#6366F1';
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
            console.error('Failed to load schedules:', xhr);
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
                // 시작일 기준 정렬
                data.sort(function(a, b) {
                    return new Date(a.start) - new Date(b.start);
                });

                // 최대 6개만 표시
                const upcomingEvents = data.slice(0, 6);
                renderUpcomingEvents(upcomingEvents);
                $('#upcomingEmpty').hide();
            } else {
                $('#upcomingEvents').empty();
                $('#upcomingEmpty').show();
            }
        },
        error: function(xhr) {
            console.error('Failed to load upcoming events:', xhr);
            $('#upcomingEvents').empty();
            $('#upcomingEmpty').show();
        }
    });
}

// 다가오는 일정 렌더링
function renderUpcomingEvents(events) {
    const $container = $('#upcomingEvents');
    $container.empty();

    $.each(events, function(index, event) {
        const startDate = new Date(event.start);
        const month = getMonthName(startDate.getMonth());
        const day = startDate.getDate();
        const time = event.allDay ? '종일' : formatTime(startDate);
        const typeClass = getTypeClass(event.type);
        const typeName = getScheduleTypeName(event.type);

        const html = `
            <div class="upcoming-item" onclick="showScheduleDetailById('${event.id}', '${escapeHtml(event.title)}', '${event.start}', '${event.end || ''}', ${event.allDay}, '${event.color || '#6366F1'}', '${event.type || 'STREAM'}', '${escapeHtml(event.description || '')}')">
                <div class="upcoming-date">
                    <span class="upcoming-month">${month}</span>
                    <span class="upcoming-day">${day}</span>
                </div>
                <div class="upcoming-info">
                    <span class="upcoming-type ${typeClass}">${typeName}</span>
                    <h3 class="upcoming-item-title">${escapeHtml(event.title)}</h3>
                    <span class="upcoming-time">🕐 ${time}</span>
                </div>
            </div>
        `;

        $container.append(html);
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

// HTML 이스케이프
function escapeHtml(text) {
    if (!text) return '';
    return text
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

// 일정 상세 보기
function showScheduleDetail(event) {
    const color = event.backgroundColor || '#6366F1';
    const type = event.extendedProps.type || 'STREAM';
    const description = event.extendedProps.description || '';
    const typeClass = getTypeClass(type);
    const typeName = getScheduleTypeName(type);

    let dateText = formatDateTime(event.start);
    if (event.end) {
        dateText += ' ~ ' + formatDateTime(event.end);
    }
    if (event.allDay) {
        dateText = formatDateKorean(event.start) + ' (종일)';
    }

    const html = `
        <div class="detail-header">
            <div class="detail-color" style="background: ${color};"></div>
            <h3 class="detail-title">${escapeHtml(event.title)}</h3>
        </div>
        <div class="detail-row">
            <span class="detail-icon">📅</span>
            <div class="detail-content">
                <div class="detail-label">일시</div>
                <div class="detail-value">${dateText}</div>
            </div>
        </div>
        <div class="detail-row">
            <span class="detail-icon">🏷️</span>
            <div class="detail-content">
                <div class="detail-label">유형</div>
                <div class="detail-value">${typeName}</div>
            </div>
        </div>
        ${description ? `
        <div class="detail-row">
            <span class="detail-icon">📝</span>
            <div class="detail-content">
                <div class="detail-label">설명</div>
                <div class="detail-value">${escapeHtml(description)}</div>
            </div>
        </div>
        ` : ''}
    `;

    $('#scheduleDetail').html(html);
    openModal('scheduleModal');
}

// ID로 일정 상세 보기 (다가오는 일정에서 클릭 시)
function showScheduleDetailById(id, title, start, end, allDay, color, type, description) {
    const typeClass = getTypeClass(type);
    const typeName = getScheduleTypeName(type);

    let dateText = formatDateTime(new Date(start));
    if (end) {
        dateText += ' ~ ' + formatDateTime(new Date(end));
    }
    if (allDay) {
        dateText = formatDateKorean(new Date(start)) + ' (종일)';
    }

    const html = `
        <div class="detail-header">
            <div class="detail-color" style="background: ${color};"></div>
            <h3 class="detail-title">${title}</h3>
        </div>
        <div class="detail-row">
            <span class="detail-icon">📅</span>
            <div class="detail-content">
                <div class="detail-label">일시</div>
                <div class="detail-value">${dateText}</div>
            </div>
        </div>
        <div class="detail-row">
            <span class="detail-icon">🏷️</span>
            <div class="detail-content">
                <div class="detail-label">유형</div>
                <div class="detail-value">${typeName}</div>
            </div>
        </div>
        ${description ? `
        <div class="detail-row">
            <span class="detail-icon">📝</span>
            <div class="detail-content">
                <div class="detail-label">설명</div>
                <div class="detail-value">${description}</div>
            </div>
        </div>
        ` : ''}
    `;

    $('#scheduleDetail').html(html);
    openModal('scheduleModal');
}

// 모달 열기
function openModal(modalId) {
    const $modal = $('#' + modalId);
    $modal.addClass('active');
    $('body').css('overflow', 'hidden');
}

// 모달 닫기
function closeModal(modalId) {
    if (modalId) {
        $('#' + modalId).removeClass('active');
    } else {
        $('.modal').removeClass('active');
    }
    $('body').css('overflow', 'auto');
}

// 모달 외부 클릭 시 닫기
$(document).on('click', '.modal', function(e) {
    if (e.target === this) {
        closeModal();
    }
});

// ESC 키로 모달 닫기
$(document).on('keydown', function(e) {
    if (e.key === 'Escape') {
        closeModal();
    }
});