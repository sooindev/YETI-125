/**
 * First-paint init — <head>에서 동기로 로드된다.
 *
 *   1. 테마: data-theme 확정 (고른 모드 → 시스템 설정 → 라이트)
 *   2. 폰트: 문 인트로 텍스트를 폰트가 준비될 때까지 미룸
 *
 * 다른 스크립트는 </body> 앞이라 첫 페인트 뒤에 실행돼 이 일을 못 맡는다.
 */
(function () {
    'use strict';

    var KEY = 'yeti-theme';
    var root = document.documentElement;

    /*
     * 모드와 테마는 다른 값이다.
     *
     *   모드  = 사용자가 고른 것    — system | light | dark
     *   테마  = 지금 화면에 칠한 색 — light | dark
     *
     * system 모드에서는 둘이 갈린다. OS 가 다크면 테마는 dark 지만
     * 모드는 여전히 system 이다. 버튼 아이콘은 모드를, <html> 은
     * 테마를 따라간다.
     */
    var MODES = ['system', 'light', 'dark'];

    // 저장값이 없으면 시스템을 따른다. light/dark 만 저장하므로
    // 예전에 테마를 골라둔 방문자의 값도 그대로 유효하다.
    function storedMode() {
        try {
            var v = localStorage.getItem(KEY);
            return (v === 'dark' || v === 'light') ? v : 'system';
        } catch (e) {
            // 사파리 프라이빗 모드 등에서 localStorage 접근이 막힐 수 있다
            return 'system';
        }
    }

    function systemPrefersDark() {
        return !!(window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches);
    }

    function resolve(mode) {
        return mode === 'system' ? (systemPrefersDark() ? 'dark' : 'light') : mode;
    }

    // 버튼 title 은 "다음에 무엇이 되는지" 를 알려준다
    var NEXT_HINT = {
        system: '시스템 설정 따르기',
        light:  '라이트 모드로 전환',
        dark:   '다크 모드로 전환'
    };
    var MODE_LABEL = {
        system: '시스템 설정',
        light:  '라이트 모드',
        dark:   '다크 모드'
    };

    function nextMode(mode) {
        return MODES[(MODES.indexOf(mode) + 1) % MODES.length];
    }

    function apply(mode) {
        root.setAttribute('data-theme', resolve(mode));

        // <head> 에서 도는 첫 호출에는 버튼이 아직 없다. DOMContentLoaded 에서 다시 부른다.
        var btn = document.getElementById('themeToggle');
        if (btn) {
            btn.setAttribute('data-mode', mode);
            btn.setAttribute('aria-label', '테마: ' + MODE_LABEL[mode]);
            btn.setAttribute('title', NEXT_HINT[nextMode(mode)]);
        }
    }

    // 첫 페인트 전에 확정
    apply(storedMode());

    /*
     * 폰트 준비 표시.
     *
     * .door-title 은 최대 304px 이라 폰트가 바뀌는 것이 그대로 보인다.
     * 폭은 common.css 의 size-adjust 로 맞춰뒀지만 글자 모양까지 같지는 않다.
     * 문이 닫혀 있는 동안이므로 텍스트만 미룬다 — 문짝은 곧바로 그려진다.
     *
     * 기다리는 대상은 Anton + JetBrains Mono(합쳐서 1.1KB)뿐이다.
     * Noto Sans KR(90.8KB)까지 기다리면 문이 늦게 열린다.
     */
    var FONTS_TIMEOUT_MS = 1500;
    var fontsSettled = false;

    function fontsReady() {
        if (fontsSettled) return;
        fontsSettled = true;
        root.classList.remove('fonts-pending');
    }

    // JS 가 꺼져 있으면 클래스가 안 붙어 원래대로 바로 보인다
    root.classList.add('fonts-pending');

    // 폰트 서버가 막히면 영영 오지 않는다
    setTimeout(fontsReady, FONTS_TIMEOUT_MS);

    // @font-face 는 스타일시트 파싱 뒤에 등록된다. 그전에 물어보면
    // "기다릴 폰트가 없다" 는 답이 돌아온다
    document.addEventListener('DOMContentLoaded', function () {
        if (!document.fonts || !document.fonts.load) {
            fontsReady();
            return;
        }
        Promise.all([
            document.fonts.load('400 100px Anton'),
            document.fonts.load('400 16px "JetBrains Mono"')
        ]).then(fontsReady, fontsReady);
    });

    window.YetiTheme = {
        // 지금 칠해진 색
        get: function () {
            return root.getAttribute('data-theme') || 'light';
        },
        // 사용자가 고른 모드
        getMode: function () {
            return storedMode();
        },
        set: function (mode) {
            if (MODES.indexOf(mode) === -1) return;
            try {
                // system 은 "저장값 없음" 으로 표현한다 — 지워야 OS 를 다시 따라간다
                if (mode === 'system') localStorage.removeItem(KEY);
                else localStorage.setItem(KEY, mode);
            } catch (e) {}
            apply(mode);
        },
        // 시스템 → 라이트 → 다크 → 시스템
        toggle: function () {
            this.set(nextMode(storedMode()));
        }
    };

    // system 모드인 동안에는 OS 설정 변경을 그대로 따라간다
    if (window.matchMedia) {
        var mq = window.matchMedia('(prefers-color-scheme: dark)');
        var onChange = function () {
            if (storedMode() === 'system') apply('system');
        };
        if (mq.addEventListener) mq.addEventListener('change', onChange);
        else if (mq.addListener) mq.addListener(onChange);
    }

    document.addEventListener('DOMContentLoaded', function () {
        // 초기 렌더가 흔들리지 않도록 transition 억제
        document.body.classList.add('preload');
        setTimeout(function () {
            document.body.classList.remove('preload');
        }, 100);

        var btn = document.getElementById('themeToggle');
        if (btn) {
            apply(storedMode());
            btn.addEventListener('click', function () {
                window.YetiTheme.toggle();
            });
        }
    });
})();
