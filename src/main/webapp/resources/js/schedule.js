/* ================================================
   Irion Fansite - Schedule Calendar (jQuery)
   ================================================ */

let calendar;

$(document).ready(function() {
  initCalendar();
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
    events: function(info, successCallback, failureCallback) {
      loadSchedules(info.startStr, info.endStr, successCallback);
    },
    eventClick: function(info) {
      showScheduleDetail(info.event);
    },
    eventDidMount: function(info) {
      // 툴팁 추가
      $(info.el).attr('title', info.event.title);
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

// 일정 상세 보기
function showScheduleDetail(event) {
  $('#modalTitle').text(event.title);

  // 일시 포맷
  let dateText = formatDate(event.start, 'display');
  if (event.end) {
    dateText += ' ~ ' + formatDate(event.end, 'display');
  }
  if (event.allDay) {
    dateText = formatDate(event.start, 'date').replace(/-/g, '.') + ' (종일)';
  }
  $('#modalDate').text(dateText);

  // 유형
  $('#modalType').text(getScheduleTypeName(event.extendedProps.type));

  // 설명
  const description = event.extendedProps.description;
  if (description) {
    $('#descriptionRow').show();
    $('#modalDescription').text(description);
  } else {
    $('#descriptionRow').hide();
  }

  openModal('scheduleModal');
}