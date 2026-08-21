/* ================================================
   Scroll Animations Script
   Using Intersection Observer API
   ================================================ */

(function() {
    'use strict';

    // 설정
    const config = {
        threshold: 0.15,
        rootMargin: '0px 0px -50px 0px'
    };

    /*
     * Intersection Observer 콜백
     *
     * 한 번 나타난 요소는 관찰을 끊는다. 애니메이션은 되돌리지 않으므로
     * 계속 지켜볼 이유가 없다. 클립/다시보기 더보기로 카드가 수백 개까지
     * 늘어나는 화면이라, 안 끊으면 스크롤할 때마다 그만큼 콜백이 돈다.
     */
    function handleIntersection(entries, observer) {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.classList.add('is-visible');
                observer.unobserve(entry.target);
            }
        });
    }

    // Observer 생성
    const observer = new IntersectionObserver(handleIntersection, {
        threshold: config.threshold,
        rootMargin: config.rootMargin
    });

    // 초기화
    function init() {
        const animateElements = document.querySelectorAll('.scroll-animate');

        animateElements.forEach(element => {
            observer.observe(element);
        });
    }

    // 동적으로 추가된 요소 관찰
    window.observeNewElements = function() {
        const animateElements = document.querySelectorAll('.scroll-animate');
        animateElements.forEach(element => {
            if (!element.classList.contains('is-visible')) {
                observer.observe(element);
            }
        });
    };

    // 페이지 로드 시 실행
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

})();
