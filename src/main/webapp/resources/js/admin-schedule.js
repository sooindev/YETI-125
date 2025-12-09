/* ================================================
   Irion Fansite - Admin Schedule Management (jQuery)
   ================================================ */

let calendar;
let currentScheduleId = null;

$(document).ready(function() {
    initCalendar();
    initEventHandlers();
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
        editable: true,
        selectable: true,
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
            'STREAM': '#6366F1',
            'EVENT': '#EC4899',
            'OTHER': '#10B981'
        };
        $('#color').val(colors[type] || '#6366F1');
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
            console.error('Failed to load schedules:', xhr);
            if (xhr.status === 401) {
                window.location.href = '/admin/login.html';
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
    openModal('scheduleModal');
}

// 날짜 선택 시 추가 모달 열기
function openAddModalWithDate(dateStr) {
    currentScheduleId = null;
    resetForm();
    $('#modalTitle').text('일정 추가');
    $('#deleteBtn').hide();
    $('#startDate').val(dateStr + 'T00:00');
    openModal('scheduleModal');
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
        $('#startDate').val(formatDate(event.start, 'datetime-local'));
    }
    if (event.end) {
        $('#endDate').val(formatDate(event.end, 'datetime-local'));
    }

    $('#deleteBtn').show();
    openModal('scheduleModal');
}

// 폼 초기화
function resetForm() {
    $('#scheduleForm')[0].reset();
    $('#scheduleId').val('');
    $('#color').val('#6366F1');
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
        startDate: new Date($('#startDate').val()).toISOString(),
        endDate: $('#endDate').val() ? new Date($('#endDate').val()).toISOString() : null,
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
                closeModal('scheduleModal');
                calendar.refetchEvents();
            } else {
                showToast(response.message || '저장에 실패했습니다.', 'error');
            }
        },
        error: function(xhr) {
            if (xhr.status === 401) {
                window.location.href = '/admin/login.html';
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
                closeModal('scheduleModal');
                calendar.refetchEvents();
            } else {
                showToast(response.message || '삭제에 실패했습니다.', 'error');
            }
        },
        error: function(xhr) {
            if (xhr.status === 401) {
                window.location.href = '/admin/login.html';
                return;
            }
            showToast('삭제 중 오류가 발생했습니다.', 'error');
        }
    });
}

// 드래그로 일정 날짜 변경
function updateScheduleDate(event) {
    const data = {
        startDate: event.start.toISOString(),
        endDate: event.end ? event.end.toISOString() : null,
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

// 로그아웃
function doLogout() {
    $.ajax({
        url: '/admin/logout',
        type: 'GET',
        success: function() {
            window.location.href = '/admin/login.html';
        },
        error: function() {
            window.location.href = '/admin/login.html';
        }
    });
}