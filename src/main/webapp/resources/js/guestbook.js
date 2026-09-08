/* 3주년 기념 방명록 — 목록 불러오기, 등록, 관리자 삭제 */

const GB_PAGE_SIZE = 12;

let gbOffset = 0;
let gbHasMore = false;
let gbSubmitting = false;
let gbCsrfToken = null;

/** 관리자로 접속했는가. 삭제 버튼을 그릴지만 정한다 — 실제 권한은 서버가 본다 */
function gbIsAdmin() {
    return document.body.getAttribute('data-admin') === 'Y';
}

// /admin/** 요청에 AJAX 표시와 CSRF 토큰을 붙인다.
// 표시가 없으면 인증 실패가 리다이렉트로 와서 jQuery 가 200 + HTML 을 받아 401 분기를 놓친다.
$.ajaxPrefilter(function(options) {
    if (!options.url || options.url.indexOf('/admin/') !== 0) return;

    const headers = {
        'X-Requested-With': 'XMLHttpRequest',
        'Accept': 'application/json'
    };

    const method = (options.type || 'GET').toUpperCase();
    if (gbCsrfToken && method !== 'GET' && method !== 'HEAD') {
        headers['X-CSRF-Token'] = gbCsrfToken;
    }

    options.headers = $.extend(headers, options.headers);
});

$(document).ready(function() {
    loadGuestbook(true);

    $('#gbForm').on('submit', function(e) {
        e.preventDefault();
        submitGuestbook();
    });

    $('#gbMoreBtn').on('click', function() {
        loadGuestbook(false);
    });

    // 남은 글자 수
    $('#gbContent').on('input', function() {
        const len = $(this).val().length;
        $('#gbContentCount').text(len);
        $('.gb-counter').toggleClass('is-full', len >= 500);
    });

    // 카드가 나중에 그려지므로 위임으로 받는다
    $('#gbList').on('click', '.gb-card-delete', function() {
        deleteGuestbook($(this).attr('data-id'));
    });

    // 관리자일 때만 토큰을 받아둔다. 방문자에게는 /admin/* 이 401 이라 부를 이유가 없다
    if (gbIsAdmin()) {
        $.ajax({ url: '/admin/csrf-token', type: 'GET', dataType: 'json' })
            .done(function(res) { gbCsrfToken = res && res.token; });
    }
});

/** 첫 로딩 동안 자리를 잡아 두는 뼈대. 빈 화면이 덜컥 나타났다 바뀌는 것을 막는다 */
function skeletonHtml(count) {
    let html = '';
    for (let i = 0; i < count; i++) {
        html += '<div class="gb-skeleton" aria-hidden="true">' +
                '<div class="gb-skeleton-top">' +
                '<div class="gb-skeleton-line"></div>' +
                '<div class="gb-skeleton-line"></div>' +
                '</div>' +
                '<div class="gb-skeleton-line is-short"></div>' +
                '<div class="gb-skeleton-line"></div>' +
                '<div class="gb-skeleton-line is-mid"></div>' +
                '</div>';
    }
    return html;
}

/** 목록 조회. reset 이면 처음부터 다시 그린다 */
function loadGuestbook(reset) {
    if (reset) {
        gbOffset = 0;
        $('#gbEmpty').hide();
        // 첫 화면에만 뼈대를 깐다. 더 보기로 이어 붙일 때는 이미 카드가 있다
        if ($('#gbList').children().length === 0) {
            $('#gbList').html(skeletonHtml(6));
        }
    }

    $('#gbMoreBtn').prop('disabled', true);

    // 응답이 온 뒤에는 gbOffset 이 이미 다음 값으로 바뀌므로 지금 붙잡아 둔다
    const requestOffset = gbOffset;

    $.ajax({
        url: '/guestbook/list',
        type: 'GET',
        data: { offset: requestOffset, limit: GB_PAGE_SIZE },
        dataType: 'json',
        timeout: 10000
    }).done(function(response) {
        if (!response || !response.success || !response.data) {
            showEmptyIfBlank();
            return;
        }

        const data = response.data;
        const entries = data.entries || [];

        gbHasMore = !!data.hasMore;
        gbOffset = data.nextOffset || (gbOffset + entries.length);

        $('#gbTotal').text(YetiUtil.numberFormat(data.total || 0));

        // 가장 오래된 글이 1번. 최신순 목록이라 맨 위가 가장 큰 번호다
        renderGuestbook(entries, !reset, (data.total || 0) - requestOffset);
        showEmptyIfBlank();
        $('#gbMore').toggle(gbHasMore);
    }).fail(function() {
        showToast('방명록을 불러오지 못했습니다.', 'error');
        $('#gbList').find('.gb-skeleton').remove();
        showEmptyIfBlank();
    }).always(function() {
        $('#gbMoreBtn').prop('disabled', false);
    });
}

function showEmptyIfBlank() {
    $('#gbEmpty').toggle($('#gbList').children().length === 0);
}

/**
 * 카드를 그린다. append 면 뒤에 잇고, 아니면 갈아 끼운다.
 * startOrdinal 은 이 묶음 첫 카드의 번호 — 아래로 내려가며 1씩 줄어든다.
 */
function renderGuestbook(entries, append, startOrdinal) {
    let html = '';

    entries.forEach(function(entry, i) {
        html += guestbookCard(entry, startOrdinal - i);
    });

    if (append) {
        $('#gbList').append(html);
    } else {
        $('#gbList').html(html);
    }

    // 카드는 JS 가 나중에 그리므로 관찰자에 다시 태워야 떠오른다 (scroll-animations.js)
    if (typeof window.observeNewElements === 'function') {
        window.observeNewElements();
    }
}

/**
 * 카드 한 장.
 * 값은 전부 사용자가 적은 글이다 — 화면에 찍기 직전 반드시 이스케이프한다.
 * 여기를 빠뜨리면 방명록에 <script> 를 적는 것만으로 보는 사람 브라우저에서 실행된다.
 *
 * ordinal 은 "몇 번째 축하인가" — 오래된 것이 1번이다.
 * 목록은 최신순이라 위에서 아래로 번호가 줄어든다.
 */
function guestbookCard(entry, ordinal) {
    const nickname = YetiUtil.escapeHtml(entry.nickname);
    const content = YetiUtil.escapeHtml(entry.content);
    const date = YetiUtil.formatDate(entry.regDate, 'datetime');

    let card = '<article class="gb-card scroll-animate scale-in">';

    if (gbIsAdmin()) {
        card += '<button type="button" class="gb-card-delete" aria-label="이 방명록 삭제"' +
                ' data-id="' + YetiUtil.escapeHtml(entry.id) + '">&times;</button>';
    }

    card += '<div class="gb-card-top">' +
            '<span class="gb-card-idx">' + padOrdinal(ordinal) + '</span>' +
            '<span class="gb-card-date">' + YetiUtil.escapeHtml(date) + '</span>' +
            '</div>' +
            '<p class="gb-card-content">' + content + '</p>';

    if (entry.cheer) {
        card += '<div class="gb-card-cheer">' +
                '<span class="gb-card-cheer-label">Cheer</span>' +
                '<span class="gb-card-cheer-text">' + YetiUtil.escapeHtml(entry.cheer) + '</span>' +
                '</div>';
    }

    // 편지처럼 글쓴이 이름이 맨 끝에 온다
    card += '<p class="gb-card-sign">' + nickname + '</p>';

    return card + '</article>';
}

/** 001 · 023 · 1024 — 세 자리까지는 0 을 채워 자릿수를 맞춘다 */
function padOrdinal(n) {
    const value = n > 0 ? n : 0;
    return value < 1000 ? String(value).padStart(3, '0') : String(value);
}

/** 등록 */
function submitGuestbook() {
    // 두 번 눌러 같은 글이 두 번 들어가는 것을 막는다 (서버에도 30초 간격 제한이 있다)
    if (gbSubmitting) return;

    const content = $('#gbContent').val().trim();
    const cheer = $('#gbCheer').val().trim();

    if (!content) {
        showToast('축하 메시지를 입력해 주세요.', 'error');
        $('#gbContent').focus();
        return;
    }

    gbSubmitting = true;
    $('#gbSubmit').prop('disabled', true);

    $.ajax({
        url: '/guestbook',
        type: 'POST',
        contentType: 'application/json;charset=UTF-8',
        dataType: 'json',
        timeout: 10000,
        // 닉네임은 보내지 않는다 — 서버가 어차피 '익명' 으로 덮어쓴다
        data: JSON.stringify({ content: content, cheer: cheer })
    }).done(function(response) {
        if (!response || !response.success) {
            showToast((response && response.message) || '등록에 실패했습니다.', 'error');
            return;
        }

        showToast(response.message || '등록되었습니다.', 'success');

        $('#gbContent').val('');
        $('#gbCheer').val('');
        $('#gbContentCount').text('0');
        $('.gb-counter').removeClass('is-full');

        // 방금 쓴 글이 맨 위에 오도록 처음부터 다시 읽는다
        loadGuestbook(true);
    }).fail(function() {
        showToast('등록에 실패했습니다. 잠시 후 다시 시도해 주세요.', 'error');
    }).always(function() {
        gbSubmitting = false;
        $('#gbSubmit').prop('disabled', false);
    });
}

/** 삭제 (관리자) */
function deleteGuestbook(guestbookId) {
    if (!guestbookId) return;
    if (!confirm('이 방명록을 삭제할까요?')) return;

    $.ajax({
        url: '/admin/guestbook/' + encodeURIComponent(guestbookId),
        type: 'DELETE',
        dataType: 'json',
        timeout: 10000
    }).done(function(response) {
        if (!response || !response.success) {
            showToast((response && response.message) || '삭제에 실패했습니다.', 'error');
            return;
        }
        showToast(response.message || '삭제했습니다.', 'success');
        loadGuestbook(true);
    }).fail(function(xhr) {
        // 세션이 끊기면 401 이 온다 — 조용히 실패하면 왜 안 지워지는지 알 수 없다
        if (xhr && xhr.status === 401) {
            showToast('로그인이 풀렸습니다. 다시 로그인해 주세요.', 'error');
            return;
        }
        showToast('삭제에 실패했습니다.', 'error');
    });
}
