/* ================================================
   Irion Fansite - Common JavaScript (jQuery)
   ================================================ */

$(document).ready(function() {
    // Mobile Menu Toggle
    $('.mobile-menu-btn').on('click', function() {
        $('.nav').toggleClass('active');
        $(this).toggleClass('active');
    });

    initHeaderAutoHide();
});

/**
 * 상단바 자동 접힘 (모바일·데스크톱 공통)
 *
 * 아래로 읽어 내려갈 때는 접고, 위로 올리면 곧바로 다시 꺼낸다.
 * 모바일 메뉴가 열려 있는 동안에는 접지 않는다 — 드로어가 헤더에
 * 붙어 있어 같이 사라지면 안 되기 때문이다.
 */
function initHeaderAutoHide() {
    var header = document.querySelector('.header');
    if (!header) return;

    var lastY = window.scrollY;
    var ticking = false;
    // 이만큼은 움직여야 반응한다 (모바일 주소창 여닫힘 등에 흔들리지 않도록)
    var THRESHOLD = 8;

    function update() {
        ticking = false;

        // 드로어가 열려 있으면 헤더를 고정해 둔다
        if (document.querySelector('.nav.active')) {
            header.classList.remove('is-hidden');
            lastY = window.scrollY;
            return;
        }

        var y = window.scrollY;
        var delta = y - lastY;

        if (Math.abs(delta) < THRESHOLD) return;

        // 최상단 근처에서는 항상 보이게 둔다
        if (y <= header.offsetHeight) {
            header.classList.remove('is-hidden');
        } else if (delta > 0) {
            header.classList.add('is-hidden');
        } else {
            header.classList.remove('is-hidden');
        }
        lastY = y;
    }

    window.addEventListener('scroll', function () {
        if (!ticking) {
            ticking = true;
            window.requestAnimationFrame(update);
        }
    }, { passive: true });

    // 창 크기가 바뀌면(회전·리사이즈) 접힘 상태를 초기화한다
    window.addEventListener('resize', function () {
        header.classList.remove('is-hidden');
        lastY = window.scrollY;
    });
}

// Modal Functions
function openModal(modalId) {
    $('#' + modalId).addClass('active');
    $('body').css('overflow', 'hidden');
}

function closeModal(modalId) {
    if (modalId) {
        $('#' + modalId).removeClass('active');
    } else {
        $('.modal').removeClass('active');
    }
    $('body').css('overflow', '');
}

// Close modal when clicking outside
$(document).on('click', '.modal', function(e) {
    if ($(e.target).hasClass('modal')) {
        $(this).removeClass('active');
        $('body').css('overflow', '');
    }
});

// Close modal with ESC key
$(document).on('keydown', function(e) {
    if (e.key === 'Escape') {
        $('.modal.active').removeClass('active');
        $('body').css('overflow', '');
    }
});

// Date Format Helper
function formatDate(date, format) {
    const d = new Date(date);
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    const hours = String(d.getHours()).padStart(2, '0');
    const minutes = String(d.getMinutes()).padStart(2, '0');

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
}

// Schedule Type to Korean
function getScheduleTypeName(type) {
    const types = {
        'STREAM': '방송',
        'EVENT': '이벤트',
        'OTHER': '기타'
    };
    return types[type] || type;
}

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
                window.location.href = '/admin/login.html';
                return;
            }
            console.error('API Request Error:', xhr);
            if (callback) callback(null);
        }
    };

    if (data && (method === 'POST' || method === 'PUT' || method === 'PATCH')) {
        options.data = JSON.stringify(data);
    }

    $.ajax(options);
}

// Toast Message
function showToast(message, type) {
    const $toast = $('#toast');
    $toast.text(message)
        .removeClass('success error')
        .addClass(type || 'success')
        .addClass('show');

    setTimeout(function() {
        $toast.removeClass('show');
    }, 3000);
}