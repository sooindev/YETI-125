/* 관리자 로그인 */

$(document).ready(function() {
    $('#loginForm').on('submit', function(e) {
        e.preventDefault();
        doLogin();
    });
});

function doLogin() {
    const adminLoginId = $('#adminLoginId').val().trim();
    const password = $('#password').val().trim();

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

    $.ajax({
        url: '/admin/loginProc',
        type: 'POST',
        data: {
            adminLoginId: adminLoginId,
            password: password
        },
        dataType: 'json',
        success: function(response) {
            // 로그인 응답을 콘솔에 찍지 않는다 — 공용 PC 의 개발자 도구에 남는다
            if (response.success) {
                window.location.href = '/admin/schedule';
            } else {
                showError(response.message || '로그인에 실패했습니다.');
            }
        },
        error: function(xhr) {
            // 429(요청 과다)처럼 서버가 이유를 적어 보낸 응답은 그 문구를 그대로 보여준다.
            // 여기서 뭉개면 "몇 초 뒤에 다시" 를 알 방법이 없어 계속 두드리게 된다.
            const message = (xhr.responseJSON && xhr.responseJSON.message)
                    || '로그인 중 오류가 발생했습니다.';
            showError(message);
            $('#password').val('').focus();
        }
    });
}

function showError(message) {
    const $errorMsg = $('#errorMsg');
    $errorMsg.text(message).show();

    setTimeout(function() {
        $errorMsg.fadeOut();
    }, 3000);
}