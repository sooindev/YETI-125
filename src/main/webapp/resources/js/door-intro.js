/* 문 열기 인트로 */

(function() {
    'use strict';

    // 하루에 한 번만. 탭을 닫아도 유지돼야 해서 localStorage 를 쓰고,
    // 날짜는 로컬 기준으로 만든다 — toISOString() 은 한국에서 오전 9시에 날짜가 바뀐다.
    const INTRO_DATE_KEY = 'door_intro_date';

    function today() {
        const now = new Date();
        const pad = function (n) { return String(n).padStart(2, '0'); };
        return now.getFullYear() + '-' + pad(now.getMonth() + 1) + '-' + pad(now.getDate());
    }

    // 사파리 프라이빗 모드에서는 접근 자체가 예외라, 감싸지 않으면 인트로가 영영 안 열린다
    function introShownToday() {
        try {
            return localStorage.getItem(INTRO_DATE_KEY) === today();
        } catch (e) {
            return false;
        }
    }

    function rememberIntroShown() {
        try {
            localStorage.setItem(INTRO_DATE_KEY, today());
        } catch (e) {}
    }

    if (introShownToday()) {
        const intro = document.querySelector('.door-intro');
        if (intro) {
            intro.classList.add('hidden');
        }
        return;
    }

    document.addEventListener('DOMContentLoaded', function() {
        const intro = document.querySelector('.door-intro');
        const enterBtn = document.querySelector('.door-enter');
        const handles = document.querySelectorAll('.door-handle');

        if (!intro) return;

        function openDoor() {
            if (intro.classList.contains('opening')) return;

            intro.classList.add('opening');

            playDoorSound();

            setTimeout(function() {
                intro.classList.add('fading');
                rememberIntroShown();

                setTimeout(function() {
                    intro.classList.add('hidden');
                    setTimeout(function() {
                        if (intro.parentNode) {
                            intro.parentNode.removeChild(intro);
                        }
                    }, 100);
                }, 800); // fade out duration
            }, 2000); // door opening duration
        }

        if (enterBtn) {
            enterBtn.addEventListener('click', openDoor);
        }

        handles.forEach(function(handle) {
            handle.addEventListener('click', openDoor);
        });

        intro.addEventListener('click', function(e) {
            if (e.target === intro || e.target.classList.contains('door-container')) {
                openDoor();
            }
        });

        document.addEventListener('keydown', function(e) {
            if (intro.classList.contains('hidden')) return;
            if (e.key === ' ' || e.key === 'Enter') {
                e.preventDefault();
                openDoor();
            }
        });
    });

    function playDoorSound() {
    }
})();
