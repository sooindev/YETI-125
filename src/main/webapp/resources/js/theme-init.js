/**
 * Theme Initialization Script
 * 페이지 로드 전 다크모드 설정 및 transition 제어
 */

// 다크모드 적용
document.documentElement.setAttribute('data-theme', 'dark');

// 페이지 로드 시 transition 비활성화
document.addEventListener('DOMContentLoaded', function() {
    document.body.classList.add('preload');
    setTimeout(function() {
        document.body.classList.remove('preload');
    }, 100);
});
