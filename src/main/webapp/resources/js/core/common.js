/* 공용 스크립트 — 상단바, 모달, 토스트, YetiUtil */

$(document).ready(function() {
    // aria-expanded 도 갱신 — 버튼 모양만으로는 스크린 리더가 못 읽는다
    $('.mobile-menu-btn').on('click', function() {
        var opened = $('.nav').toggleClass('active').hasClass('active');
        $(this).toggleClass('active').attr('aria-expanded', opened ? 'true' : 'false');
    });

    initHeaderScrollState();
});

/**
 * 상단바 고정. 스크롤 시작 시 흐린 배경.
 * 방향에 따라 접는 동작은 iOS 사파리에서 흐림이 남아 쓰지 않는다(CSS 만 남음)
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
 * 공용 유틸. 전역에 흩어져 있어 나중 스크립트가 같은 이름으로 덮어쓰던 것을 모았다.
 * 새 유틸은 여기 추가 — 페이지 스크립트에서 전역 함수를 만들지 말 것
 */
window.YetiUtil = (function () {
    'use strict';

    /**
     * 날짜 문자열 → Date. 실패 시 null.
     * "2024-02-29 12:00:00" 은 ES 표준이 아니라 구버전 사파리에서 Invalid Date
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

        /** HTML 이스케이프 — 찍기 직전 한 번만 */
        escapeHtml: function (text) {
            if (text === null || text === undefined || text === '') return '';
            return String(text)
                .replace(/&/g, '&amp;')
                .replace(/</g, '&lt;')
                .replace(/>/g, '&gt;')
                .replace(/"/g, '&quot;')
                .replace(/'/g, '&#039;');
        },

        /** href/src 에 넣어도 되는 URL 인지. javascript: 스킴은 클릭 한 번에 스크립트가 된다 */
        safeUrl: function (url) {
            if (!url) return '';
            var value = String(url).trim();
            return /^(https?:\/\/|\/|\.\/)/i.test(value) ? value : '';
        },

        /** 스킴 검사 + 따옴표 차단. href/src 를 이어 붙일 때는 반드시 이것 */
        attrUrl: function (url) {
            return this.escapeHtml(this.safeUrl(url));
        },

        /** style 속성용 색 — #rgb / #rrggbb 만 */
        safeColor: function (color) {
            return /^#[0-9a-fA-F]{3}([0-9a-fA-F]{3})?$/.test(color || '') ? color : '#6366F1';
        },

        /** 1000 → 1,000 */
        numberFormat: function (num) {
            if (num === null || num === undefined) return '0';
            return String(num).replace(/\B(?=(\d{3})+(?!\d))/g, ',');
        },

        /** 날짜 포맷 — date / datetime / datetime-local / display */
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

        /** "3월 5일" — 카드용 짧은 형식 */
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

// 인라인 onclick 대신 data-close-modal — 인라인 핸들러가 없어야 CSP 에서 'unsafe-inline' 제거 가능
$(document).on('click', '[data-close-modal]', function() {
    YetiUtil.closeModal($(this).attr('data-close-modal'));
});

// 모달 바깥 클릭으로 닫기
$(document).on('click', '.modal', function(e) {
    if ($(e.target).hasClass('modal')) {
        YetiUtil.closeModal();
    }
});

// ESC 로 닫기
$(document).on('keydown', function(e) {
    if (e.key === 'Escape') {
        YetiUtil.closeModal();
    }
});

// 이전 타이머 해제 후 재설정 — 안 하면 두 번째 토스트가 첫 타이머에 걸린다
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