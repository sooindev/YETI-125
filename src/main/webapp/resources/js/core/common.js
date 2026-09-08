/* 공용 스크립트 — 상단바, 모달, 토스트, YetiUtil */

$(document).ready(function() {
    // aria-expanded 도 갱신한다 — 버튼 모양만으로는 스크린 리더가 열림 상태를 모른다
    $('.mobile-menu-btn').on('click', function() {
        var opened = $('.nav').toggleClass('active').hasClass('active');
        $(this).toggleClass('active').attr('aria-expanded', opened ? 'true' : 'false');
    });

    initHeaderScrollState();
});

/**
 * 상단바는 항상 고정. 스크롤이 시작되면 흐린 배경이 배어 나온다.
 * 방향에 따라 접는 동작은 iOS 사파리에서 흐림이 남아 쓰지 않는다(CSS 규칙만 남아 있다).
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

/*
 * 공용 유틸. 전에는 전역에 흩어져 있어 뒤에 로드되는 스크립트가 같은 이름으로 덮어썼다.
 * 새 유틸은 여기 추가하고 페이지 스크립트에서 전역 함수를 만들지 말 것.
 */
window.YetiUtil = (function () {
    'use strict';

    /**
     * 날짜 문자열을 Date 로. 못 읽으면 null.
     * "2024-02-29 12:00:00" 은 ES 표준이 아니라 구버전 사파리에서 Invalid Date 가 된다.
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

        /** href/src 에 넣어도 되는 URL 인가. javascript: 스킴이 섞이면 클릭 한 번에 스크립트가 된다. */
        safeUrl: function (url) {
            if (!url) return '';
            var value = String(url).trim();
            return /^(https?:\/\/|\/|\.\/)/i.test(value) ? value : '';
        },

        /** 스킴을 거르고 따옴표를 막은 URL. href/src 를 이어 붙일 때는 반드시 이걸 쓴다. */
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

// 인라인 onclick 대신 data-close-modal — 인라인 핸들러가 없어야 CSP 에서 'unsafe-inline' 을 뺀다
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

// 이전 타이머를 지우고 다시 건다 — 안 그러면 두 번째 토스트가 첫 번째 타이머에 걸린다
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