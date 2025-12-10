/* ================================================
   Irion Fansite - Admin Login (jQuery)
   ================================================ */

$(document).ready(function() {
    // 로그인 폼 제출
    $('#loginForm').on('submit', function(e) {
        e.preventDefault();
        doLogin();
    });
});

// 로그인 처리
function doLogin() {
    const adminLoginId = $('#adminLoginId').val().trim();
    const password = $('#password').val().trim();

    // 유효성 검사
    if (!adminLoginId) {
        showError('아이디를 입력해주세요.');
        $('#adminLoginId').focus();
        return;
    }

    if (!password) {
        showError('비밀번호를 입력해주세요.');
        $('#password').focus();
        return;
    }

    // 로그인 요청
    $.ajax({
        url: '/admin/loginProc',
        type: 'POST',
        data: {
            adminLoginId: adminLoginId,
            password: password
        },
        dataType: 'json',
        success: function(response) {
            console.log('=== Login Response ===');
            console.log('success:', response.success);
            console.log('message:', response.message);
            console.log('full response:', response);

            if (response.success) {
                console.log('Redirecting to /admin/admin-schedule.html...');
                window.location.href = '/admin/admin-schedule.html';
            } else {
                showError(response.message || '로그인에 실패했습니다.');
            }
        },
        error: function(xhr, status, error) {
            console.log('Error:', xhr, status, error);  // 디버깅용
            showError('로그인 중 오류가 발생했습니다.');
            $('#password').val('').focus();
        }
    });
}

// 에러 메시지 표시
function showError(message) {
    const $errorMsg = $('#errorMsg');
    $errorMsg.text(message).show();

    // 3초 후 숨김
    setTimeout(function() {
        $errorMsg.fadeOut();
    }, 3000);
}