/* ================================================
   Irion Fansite - Admin Schedule Management (jQuery)
   ================================================ */

let calendar;
let currentScheduleId = null;

/*
 * /admin/** 요청에 AJAX 표시와 CSRF 토큰을 붙인다.
 *
 * 표시가 없으면 서버가 인증 실패를 401 이 아니라 로그인 페이지 리다이렉트로
 * 돌려주는데, jQuery 가 그걸 따라가 200 + HTML 을 받아버려 401 분기에 걸리지
 * 않는다. jQuery 도 X-Requested-With 를 붙여주지만 인증 판정이 걸린 헤더를
 * 라이브러리 기본값에 맡기지 않는다.
 */
let csrfToken = null;

$.ajaxPrefilter(function(options) {
    if (!options.url || options.url.indexOf('/admin/') !== 0) return;

    const headers = {
        'X-Requested-With': 'XMLHttpRequest',
        'Accept': 'application/json'
    };

    const method = (options.type || 'GET').toUpperCase();
    if (csrfToken && method !== 'GET' && method !== 'HEAD') {
        headers['X-CSRF-Token'] = csrfToken;
    }

    options.headers = $.extend(headers, options.headers);
});

$(document).ready(function() {
    // 실패해도 화면은 띄운다. 조회에는 토큰이 필요 없고,
    // 저장 시점에 서버가 403 으로 알려준다
    $.ajax({
        url: '/admin/csrf-token',
        type: 'GET',
        dataType: 'json'
    }).done(function(res) {
        csrfToken = res && res.token;
    }).always(function() {
        initCalendar();
        initEventHandlers();
    });
});

// 캘린더 초기화
function initCalendar() {
    const calendarEl = document.getElementById('calendar');

    // 좁은 화면에서 월간 격자는 칸 폭이 40px 남짓이라 제목이 들어가지 않는다.
    // 일정을 확인하고 수정하는 화면이므로 모바일에서는 목록으로 시작한다.
    // (상단 월간/목록 토글로 언제든 바꿀 수 있다)
    const isNarrow = window.matchMedia('(max-width: 720px)').matches;

    calendar = new FullCalendar.Calendar(calendarEl, {
        initialView: isNarrow ? 'listMonth' : 'dayGridMonth',
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
        editable: true,
        selectable: true,
        // ko 로케일은 날짜를 "26일"로 렌더링한다. 좁은 화면에서는 칸 안에서
        // "26 / 일"로 줄바꿈되어 행 높이가 늘어나므로 숫자만 표시한다.
        // (공개 페이지 schedule.js 와 동일한 처리)
        dayCellContent: function(arg) {
            return arg.date.getDate();
        },
        events: function(info, successCallback, failureCallback) {
            loadSchedules(info.startStr, info.endStr, successCallback);
        },
        eventClick: function(info) {
            openEditModal(info.event);
        },
        select: function(info) {
            openAddModalWithDate(info.startStr);
        },
        eventDrop: function(info) {
            updateScheduleDate(info.event);
        },
        eventResize: function(info) {
            updateScheduleDate(info.event);
        }
    });

    calendar.render();
}

// 이벤트 핸들러 초기화
function initEventHandlers() {
    // 폼 제출
    $('#scheduleForm').on('submit', function(e) {
        e.preventDefault();
        saveSchedule();
    });

    // 로그아웃
    $('#logoutBtn').on('click', function(e) {
        e.preventDefault();
        doLogout();
    });

    // 유형 변경 시 색상 자동 변경
    $('#scheduleType').on('change', function() {
        const type = $(this).val();
        const colors = {
            'COLLAB': '#d68fb0',
            'JUSTCHAT': '#7fb58a',
            'GAME': '#8c8fd6',
            'KARAOKE': '#d6c07f'
        };
        $('#color').val(colors[type] || '#8c8fd6');
    });

    // 새 일정 추가 버튼
    $('.btn-primary[data-action="add"]').on('click', function() {
        openAddModal();
    });

    // 모달 닫기 버튼
    $('.modal-close, .btn-secondary[data-action="cancel"]').on('click', function() {
        YetiUtil.closeModal('scheduleModal');
    });

    // 삭제 버튼
    $('#deleteBtn').on('click', function() {
        deleteSchedule();
    });
}

// 일정 데이터 로드
function loadSchedules(start, end, callback) {
    $.ajax({
        url: '/admin/schedule/list',
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
                    className: item.displayYn === 'N' ? 'hidden-schedule' : '',
                    extendedProps: {
                        description: item.description,
                        type: item.type,
                        displayYn: item.displayYn
                    }
                });
            });
            callback(events);
        },
        error: function(xhr) {
            if (xhr.status === 401) {
                window.location.href = '/admin/admin-login';
            }
            callback([]);
        }
    });
}

// 새 일정 추가 모달 열기
function openAddModal() {
    currentScheduleId = null;
    resetForm();
    $('#modalTitle').text('일정 추가');
    $('#deleteBtn').hide();
    YetiUtil.openModal('scheduleModal');
}

// 날짜 선택 시 추가 모달 열기
function openAddModalWithDate(dateStr) {
    currentScheduleId = null;
    resetForm();
    $('#modalTitle').text('일정 추가');
    $('#deleteBtn').hide();
    $('#startDate').val(dateStr + 'T00:00');
    YetiUtil.openModal('scheduleModal');
}

// 수정 모달 열기
function openEditModal(event) {
    currentScheduleId = event.id;

    $('#modalTitle').text('일정 수정');
    $('#scheduleId').val(event.id);
    $('#title').val(event.title);
    $('#scheduleType').val(event.extendedProps.type);
    $('#color').val(event.backgroundColor || '#6366F1');
    $('#description').val(event.extendedProps.description || '');
    $('#allDayYn').prop('checked', event.allDay);
    $('#displayYn').prop('checked', event.extendedProps.displayYn === 'Y');

    // 날짜 설정
    if (event.start) {
        $('#startDate').val(YetiUtil.formatDate(event.start, 'datetime-local'));
    }
    if (event.end) {
        $('#endDate').val(YetiUtil.formatDate(event.end, 'datetime-local'));
    }

    $('#deleteBtn').show();
    YetiUtil.openModal('scheduleModal');
}

// 폼 초기화
function resetForm() {
    $('#scheduleForm')[0].reset();
    $('#scheduleId').val('');
    $('#scheduleType').val('JUSTCHAT');  // 기본값을 저스트채팅으로 설정
    $('#color').val('#7fb58a');  // 저스트채팅
    $('#displayYn').prop('checked', true);
}

// 일정 저장 (추가/수정)
function saveSchedule() {
    const title = $('#title').val().trim();
    const startDate = $('#startDate').val();

    // 유효성 검사
    if (!title) {
        showToast('제목을 입력해주세요.', 'error');
        $('#title').focus();
        return;
    }

    if (!startDate) {
        showToast('시작 일시를 선택해주세요.', 'error');
        $('#startDate').focus();
        return;
    }

    const data = {
        title: title,
        description: $('#description').val().trim(),
        scheduleType: $('#scheduleType').val(),
        startDate: formatDateForServer($('#startDate').val()),
        endDate: $('#endDate').val() ? formatDateForServer($('#endDate').val()) : null,
        allDayYn: $('#allDayYn').is(':checked') ? 'Y' : 'N',
        displayYn: $('#displayYn').is(':checked') ? 'Y' : 'N',
        color: $('#color').val()
    };

    let url = '/admin/schedule';
    let method = 'POST';

    if (currentScheduleId) {
        url = '/admin/schedule/' + currentScheduleId;
        method = 'PUT';
    }

    $.ajax({
        url: url,
        type: method,
        contentType: 'application/json',
        data: JSON.stringify(data),
        success: function(response) {
            if (response.success) {
                showToast(response.message, 'success');
                YetiUtil.closeModal('scheduleModal');
                calendar.refetchEvents();
            } else {
                showToast(response.message || '저장에 실패했습니다.', 'error');
            }
        },
        error: function(xhr) {
            if (xhr.status === 401) {
                window.location.href = '/admin/admin-login';
                return;
            }
            showToast('저장 중 오류가 발생했습니다.', 'error');
        }
    });
}

// 일정 삭제
function deleteSchedule() {
    if (!currentScheduleId) return;

    if (!confirm('정말 이 일정을 삭제하시겠습니까?')) {
        return;
    }

    $.ajax({
        url: '/admin/schedule/' + currentScheduleId,
        type: 'DELETE',
        success: function(response) {
            if (response.success) {
                showToast(response.message, 'success');
                YetiUtil.closeModal('scheduleModal');
                calendar.refetchEvents();
            } else {
                showToast(response.message || '삭제에 실패했습니다.', 'error');
            }
        },
        error: function(xhr) {
            if (xhr.status === 401) {
                window.location.href = '/admin/admin-login';
                return;
            }
            showToast('삭제 중 오류가 발생했습니다.', 'error');
        }
    });
}

// 드래그로 일정 날짜 변경
function updateScheduleDate(event) {
    // Date 객체의 로컬 시각을 "yyyy-MM-dd'T'HH:mm:ss" 형식으로 추출.
    // 서버(ScheduleVO)가 Asia/Seoul 기준으로 해석하므로 화면의 시각이 그대로 저장된다.
    const formatEventDate = (dateObj) => {
        if (!dateObj) return null;
        const p = (n) => String(n).padStart(2, '0');
        return dateObj.getFullYear() + '-' + p(dateObj.getMonth() + 1) + '-' + p(dateObj.getDate())
            + 'T' + p(dateObj.getHours()) + ':' + p(dateObj.getMinutes()) + ':' + p(dateObj.getSeconds());
    };

    const data = {
        startDate: formatEventDate(event.start),
        endDate: event.end ? formatEventDate(event.end) : null,
        allDayYn: event.allDay ? 'Y' : 'N'
    };

    // 기존 데이터 유지
    data.title = event.title;
    data.description = event.extendedProps.description;
    data.scheduleType = event.extendedProps.type;
    data.displayYn = event.extendedProps.displayYn;
    data.color = event.backgroundColor;

    $.ajax({
        url: '/admin/schedule/' + event.id,
        type: 'PUT',
        contentType: 'application/json',
        data: JSON.stringify(data),
        success: function(response) {
            if (response.success) {
                showToast('일정이 이동되었습니다.', 'success');
            } else {
                showToast('이동에 실패했습니다.', 'error');
                calendar.refetchEvents();
            }
        },
        error: function(xhr) {
            showToast('오류가 발생했습니다.', 'error');
            calendar.refetchEvents();
        }
    });
}

// 날짜를 서버로 전송할 형식으로 변환
// datetime-local 입력값("2026-05-21T08:00")을 타임존 변환 없이 그대로 전송한다.
// 서버(ScheduleVO)가 이 값을 Asia/Seoul 기준으로 해석하므로 입력 시각이 그대로 저장된다.
function formatDateForServer(dateTimeLocalString) {
    if (!dateTimeLocalString) return null;

    // 초가 없으면 ":00"을 붙여 "yyyy-MM-dd'T'HH:mm:ss" 형식을 맞춘다.
    return dateTimeLocalString.length === 16
        ? dateTimeLocalString + ':00'
        : dateTimeLocalString;
}

/* GET 이면 img 태그 하나로도 남의 세션을 끊을 수 있어 POST 로 둔다 */
function doLogout() {
    $.ajax({
        url: '/admin/logout',
        type: 'POST',
        dataType: 'json'
    }).always(function() {
        window.location.href = '/admin/admin-login';
    });
}