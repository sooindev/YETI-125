/* ================================================
   Irion Fansite - Common JavaScript (jQuery)
   ================================================ */

$(document).ready(function() {
    // Mobile Menu Toggle
    $('.mobile-menu-btn').on('click', function() {
        $('.nav').toggleClass('active');
        $(this).toggleClass('active');
    });
});

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