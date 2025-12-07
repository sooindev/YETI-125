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
  var adminLoginId = $('#adminLoginId').val().trim();
  var password = $('#password').val().trim();

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
    success: function(response) {
      // 로그인 성공 - 일정 관리 페이지로 이동
      window.location.href = '/admin/schedule.html';
    },
    error: function(xhr) {
      if (xhr.status === 401 || xhr.status === 400) {
        showError('아이디 또는 비밀번호가 일치하지 않습니다.');
      } else {
        showError('로그인 중 오류가 발생했습니다.');
      }
      $('#password').val('').focus();
    }
  });
}

// 에러 메시지 표시
function showError(message) {
  var $errorMsg = $('#errorMsg');
  $errorMsg.text(message).show();

  // 3초 후 숨김
  setTimeout(function() {
    $errorMsg.fadeOut();
  }, 3000);
}