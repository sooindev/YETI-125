/* ================================================
   Door Opening Intro - Interactive Animation
   ================================================ */

(function() {
    'use strict';

    // 세션 체크 - 같은 세션에서는 한 번만 표시
    const INTRO_SESSION_KEY = 'door_intro_shown';

    // 이미 이번 세션에 표시했으면 숨김
    if (sessionStorage.getItem(INTRO_SESSION_KEY)) {
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
                // 세션에 표시 기록
                sessionStorage.setItem(INTRO_SESSION_KEY, 'true');

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
