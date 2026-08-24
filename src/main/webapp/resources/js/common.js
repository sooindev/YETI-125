/* ================================================
   Irion Fansite - Common JavaScript (jQuery)
   ================================================ */

$(document).ready(function() {
    // Mobile Menu Toggle
    //
    // aria-expanded 를 같이 갱신한다. 버튼 모양(햄버거 ↔ X)만으로는
    // 스크린 리더 사용자가 메뉴가 열렸는지 알 수 없다.
    $('.mobile-menu-btn').on('click', function() {
        var opened = $('.nav').toggleClass('active').hasClass('active');
        $(this).toggleClass('active').attr('aria-expanded', opened ? 'true' : 'false');
    });

    initHeaderScrollState();
});

/**
 * 상단바 스크롤 상태
 *
 * 상단바는 항상 고정되어 있다. 최상단에서는 배경 없이 페이지에 묻혀
 * 있다가, 스크롤이 시작되면 흐린 배경과 hairline 이 배어 나온다.
 *
 * 스크롤 방향에 따라 접었다 폈다 하는 동작은 쓰지 않는다. iOS 사파리에서
 * 흐림 레이어가 남거나 움직임이 끊기는 문제가 반복돼, 예측 가능한 고정
 * 방식을 택했다. 되살리려면 스크롤 방향을 비교해 .is-hidden 을 토글하면
 * 된다 (CSS 에 규칙은 남겨두었다).
 */
function initHeaderScrollState() {
    var header = document.querySelector('.header');
    if (!header) return;

    var ticking = false;

    function update() {
        ticking = false;
        header.classList.toggle('is-stuck', window.scrollY > 4);
    }

    window.addEventListener('scroll', function () {
        if (!ticking) {
            ticking = true;
            window.requestAnimationFrame(update);
        }
    }, { passive: true });

    update();
}

/* ================================================
   YetiUtil — 공용 유틸리티 네임스페이스

   전에는 이 함수들이 전역에 흩어져 있었고, 페이지마다 뒤에 로드되는
   스크립트가 같은 이름으로 덮어썼다.

     formatDate    common.js  ← index.js   (시그니처가 아예 달랐다)
     openModal     common.js  ← schedule.js
     closeModal    common.js  ← schedule.js (overflow 복원값도 '' vs 'auto')
     escapeHtml    index.js   / schedule.js  (같은 코드 두 벌)
     numberFormat  index.js   / info.js      (같은 코드 두 벌)

   window.YetiTheme 와 같은 방식으로 한 곳에 모은다. 새 유틸은 여기에
   추가하고, 페이지 스크립트에서 전역 함수를 새로 만들지 말 것.
   ================================================ */
window.YetiUtil = (function () {
    'use strict';

    /**
     * 서버가 준 날짜 문자열을 Date 로.
     *
     * "2024-02-29 12:00:00" 처럼 공백으로 이어 붙인 형식은 ES 표준이
     * 아니라 구버전 사파리에서 Invalid Date 가 된다. 그건 예외가 아니라
     * 값이라서 try/catch 로도 잡히지 않고, 그대로 "NaN월 NaN일" 이 렌더된다.
     * 형식을 직접 읽어 로컬 시각으로 만든다.
     *
     * 읽을 수 없으면 null 이다.
     */
    function parseDate(value) {
        if (value instanceof Date) {
            return isNaN(value.getTime()) ? null : value;
        }
        if (typeof value === 'number') {
            var fromNumber = new Date(value);
            return isNaN(fromNumber.getTime()) ? null : fromNumber;
        }
        if (typeof value !== 'string' || !value) {
            return null;
        }

        var m = /^(\d{4})-(\d{2})-(\d{2})(?:[ T](\d{2}):(\d{2})(?::(\d{2}))?)?/.exec(value);
        if (m) {
            return new Date(+m[1], +m[2] - 1, +m[3],
                    +(m[4] || 0), +(m[5] || 0), +(m[6] || 0));
        }

        var parsed = new Date(value);
        return isNaN(parsed.getTime()) ? null : parsed;
    }

    return {
        parseDate: parseDate,

        /** HTML 이스케이프 — 화면에 글자를 찍기 직전 한 번만 통과시킨다 */
        escapeHtml: function (text) {
            if (text === null || text === undefined || text === '') return '';
            return String(text)
                .replace(/&/g, '&amp;')
                .replace(/</g, '&lt;')
                .replace(/>/g, '&gt;')
                .replace(/"/g, '&quot;')
                .replace(/'/g, '&#039;');
        },

        /**
         * 속성에 넣어도 되는 URL 인가.
         *
         * href / src 에 그대로 들어가는 값이다. javascript: 같은 스킴이
         * 섞여 들어오면 클릭 한 번으로 스크립트가 된다. http(s) 와
         * 상대경로만 통과시킨다.
         */
        safeUrl: function (url) {
            if (!url) return '';
            var value = String(url).trim();
            return /^(https?:\/\/|\/|\.\/)/i.test(value) ? value : '';
        },

        /**
         * 속성에 바로 넣을 수 있는 URL 문자열.
         *
         * safeUrl 로 스킴을 거르고 escapeHtml 로 따옴표를 막는다. href / src
         * 를 문자열로 이어 붙일 때는 반드시 이걸 쓴다. 제목만 이스케이프하고
         * URL 은 그냥 넣는 식으로 규칙이 갈리면 어디가 안전한지 알 수 없다.
         */
        attrUrl: function (url) {
            return this.escapeHtml(this.safeUrl(url));
        },

        /** style 속성에 넣어도 되는 색인가 — #rgb / #rrggbb 만 */
        safeColor: function (color) {
            return /^#[0-9a-fA-F]{3}([0-9a-fA-F]{3})?$/.test(color || '') ? color : '#6366F1';
        },

        /** 1000 → 1,000 */
        numberFormat: function (num) {
            if (num === null || num === undefined) return '0';
            return String(num).replace(/\B(?=(\d{3})+(?!\d))/g, ',');
        },

        /** 날짜 포맷 (date / datetime / datetime-local / display) */
        formatDate: function (date, format) {
            var d = parseDate(date);
            if (!d) return '';

            var year = d.getFullYear();
            var month = String(d.getMonth() + 1).padStart(2, '0');
            var day = String(d.getDate()).padStart(2, '0');
            var hours = String(d.getHours()).padStart(2, '0');
            var minutes = String(d.getMinutes()).padStart(2, '0');

            if (format === 'date') {
                return year + '-' + month + '-' + day;
            } else if (format === 'datetime') {
                return year + '-' + month + '-' + day + ' ' + hours + ':' + minutes;
            } else if (format === 'datetime-local') {
                return year + '-' + month + '-' + day + 'T' + hours + ':' + minutes;
            } else if (format === 'display') {
                return year + '년 ' + month + '월 ' + day + '일 ' + hours + ':' + minutes;
            }
            return d.toString();
        },

        /** "3월 5일" — 목록 카드에 쓰는 짧은 형식 */
        formatMonthDay: function (value) {
            var d = parseDate(value);
            return d ? (d.getMonth() + 1) + '월 ' + d.getDate() + '일' : '';
        },

        openModal: function (modalId) {
            $('#' + modalId).addClass('active');
            $('body').css('overflow', 'hidden');
        },

        closeModal: function (modalId) {
            if (modalId) {
                $('#' + modalId).removeClass('active');
            } else {
                $('.modal').removeClass('active');
            }
            $('body').css('overflow', '');
        }
    };
})();

/* ------------------------------------------------
   모달 닫기 신호
   ------------------------------------------------ */

/*
 * 닫기 버튼
 *
 * 인라인 onclick 대신 data-close-modal 로 표시한다. 페이지 안에 인라인
 * 핸들러가 하나도 없어야 CSP 의 script-src 에서 'unsafe-inline' 을 뺄 수 있다.
 */
$(document).on('click', '[data-close-modal]', function() {
    YetiUtil.closeModal($(this).attr('data-close-modal'));
});

// 모달 바깥을 클릭하면 닫는다
$(document).on('click', '.modal', function(e) {
    if ($(e.target).hasClass('modal')) {
        YetiUtil.closeModal();
    }
});

// ESC 로 닫는다
$(document).on('keydown', function(e) {
    if (e.key === 'Escape') {
        YetiUtil.closeModal();
    }
});

// API Request Helper
function apiRequest(url, method, data, callback) {
    const options = {
        url: url,
        type: method || 'GET',
        contentType: 'application/json',
        dataType: 'json',
        headers: {
            'X-Requested-With': 'XMLHttpRequest'
        },
        success: function(response) {
            if (callback) callback(response);
        },
        error: function(xhr) {
            // 인증 실패 시
            if (xhr.status === 401) {
                window.location.href = '/admin/admin-login';
                return;
            }
            if (callback) callback(null);
        }
    };

    if (data && (method === 'POST' || method === 'PUT' || method === 'PATCH')) {
        options.data = JSON.stringify(data);
    }

    $.ajax(options);
}

// Toast Message
/*
 * 토스트
 *
 * 이전 타이머를 지우고 다시 건다. 그러지 않으면 두 번째 토스트가
 * 첫 번째의 타이머에 걸려 3초를 못 채우고 사라진다.
 */
let toastTimer = null;

function showToast(message, type) {
    const $toast = $('#toast');
    $toast.text(message)
        .removeClass('success error')
        .addClass(type || 'success')
        .addClass('show');

    clearTimeout(toastTimer);
    toastTimer = setTimeout(function() {
        $toast.removeClass('show');
    }, 3000);
}