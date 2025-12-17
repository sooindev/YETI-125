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

    // Intersection Observer 콜백
    function handleIntersection(entries, observer) {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.classList.add('is-visible');
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

        console.log(`Scroll animations initialized for ${animateElements.length} elements`);
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
