/* 스크롤 등장 애니메이션 (Intersection Observer) */

(function() {
    'use strict';

    const config = {
        threshold: 0.15,
        rootMargin: '0px 0px -50px 0px'
    };

    // 한 번 나타난 요소는 관찰을 끊는다 — 카드가 수백 개로 늘면 스크롤마다 그만큼 콜백이 돈다
    function handleIntersection(entries, observer) {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.classList.add('is-visible');
                observer.unobserve(entry.target);
            }
        });
    }

    const observer = new IntersectionObserver(handleIntersection, {
        threshold: config.threshold,
        rootMargin: config.rootMargin
    });

    function init() {
        const animateElements = document.querySelectorAll('.scroll-animate');

        animateElements.forEach(element => {
            observer.observe(element);
        });
    }

    window.observeNewElements = function() {
        const animateElements = document.querySelectorAll('.scroll-animate');
        animateElements.forEach(element => {
            if (!element.classList.contains('is-visible')) {
                observer.observe(element);
            }
        });
    };

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

})();
