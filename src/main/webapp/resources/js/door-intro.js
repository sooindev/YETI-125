/* ================================================
   Door Opening Intro - Interactive Animation
   ================================================ */

(function() {
    'use strict';

    /*
     * 하루에 한 번만 표시한다.
     *
     * sessionStorage 가 아니라 localStorage — 탭을 닫아도 그날 안에는
     * 다시 뜨지 않아야 한다. 날짜는 로컬 기준으로 만든다. toISOString()
     * 을 쓰면 한국에서는 오전 9시에 날짜가 바뀐다.
     */
    const INTRO_DATE_KEY = 'door_intro_date';

    function today() {
        const now = new Date();
        const pad = function (n) { return String(n).padStart(2, '0'); };
        return now.getFullYear() + '-' + pad(now.getMonth() + 1) + '-' + pad(now.getDate());
    }

    /*
     * 사파리 프라이빗 모드 등에서는 localStorage 접근 자체가 예외를 던진다.
     * 감싸두지 않으면 스크립트가 첫 줄에서 죽어 인트로가 영영 안 열린다.
     */
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

    // 오늘 이미 봤으면 숨김
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

        // 문 열기 함수
        function openDoor() {
            // 이미 열리는 중이면 무시
            if (intro.classList.contains('opening')) return;

            // 문 열기 애니메이션 시작
            intro.classList.add('opening');

            // 사운드 효과 (선택사항)
            playDoorSound();

            // 애니메이션 완료 후 fade out
            setTimeout(function() {
                intro.classList.add('fading');
                // 오늘 봤다고 기록
                rememberIntroShown();

                // Fade out 완료 후 완전히 제거
                setTimeout(function() {
                    intro.classList.add('hidden');
                    // DOM에서 완전히 제거
                    setTimeout(function() {
                        if (intro.parentNode) {
                            intro.parentNode.removeChild(intro);
                        }
                    }, 100);
                }, 800); // fade out duration
            }, 2000); // door opening duration
        }

        // Enter 버튼 클릭
        if (enterBtn) {
            enterBtn.addEventListener('click', openDoor);
        }

        // 문 손잡이 클릭
        handles.forEach(function(handle) {
            handle.addEventListener('click', openDoor);
        });

        // 전체 화면 클릭
        intro.addEventListener('click', function(e) {
            // 버튼이나 손잡이가 아닌 배경 클릭도 작동
            if (e.target === intro || e.target.classList.contains('door-container')) {
                openDoor();
            }
        });

        // 키보드 입력 (스페이스바, 엔터)
        document.addEventListener('keydown', function(e) {
            if (intro.classList.contains('hidden')) return;
            if (e.key === ' ' || e.key === 'Enter') {
                e.preventDefault();
                openDoor();
            }
        });
    });

    // 사운드 효과 (선택사항)
    function playDoorSound() {
        // 사운드 파일이 있다면:
        // const audio = new Audio('/resources/sounds/door-open.mp3');
        // audio.volume = 0.3;
        // audio.play().catch(function() {
        //     // 자동재생 실패 시 무시
        // });
    }
})();
