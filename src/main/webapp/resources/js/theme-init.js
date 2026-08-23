/**
 * First-paint init — <head>에서 동기로 로드된다.
 *
 *   1. 테마: data-theme 확정 (저장된 선택 → OS 설정 → 라이트)
 *   2. 폰트: 문 인트로 텍스트를 폰트가 준비될 때까지 미룸
 *
 * 다른 스크립트는 </body> 앞이라 첫 페인트 뒤에 실행돼 이 일을 못 맡는다.
 */
(function () {
    'use strict';

    var KEY = 'yeti-theme';
    var root = document.documentElement;

    function stored() {
        try {
            var v = localStorage.getItem(KEY);
            return (v === 'dark' || v === 'light') ? v : null;
        } catch (e) {
            // 사파리 프라이빗 모드 등에서 localStorage 접근이 막힐 수 있다
            return null;
        }
    }

    function systemPrefersDark() {
        return !!(window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches);
    }

    function apply(theme) {
        root.setAttribute('data-theme', theme);
        var btn = document.getElementById('themeToggle');
        if (btn) {
            btn.setAttribute('aria-pressed', theme === 'dark' ? 'true' : 'false');
            btn.setAttribute('title', theme === 'dark' ? '라이트 모드로 전환' : '다크 모드로 전환');
        }
    }

    // 첫 페인트 전에 확정
    apply(stored() || (systemPrefersDark() ? 'dark' : 'light'));

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
        get: function () {
            return root.getAttribute('data-theme') || 'light';
        },
        set: function (theme) {
            if (theme !== 'dark' && theme !== 'light') return;
            apply(theme);
            try { localStorage.setItem(KEY, theme); } catch (e) {}
        },
        toggle: function () {
            this.set(this.get() === 'dark' ? 'light' : 'dark');
        }
    };

    // 직접 고르기 전까지는 OS 설정을 따라간다
    if (window.matchMedia) {
        var mq = window.matchMedia('(prefers-color-scheme: dark)');
        var onChange = function (e) {
            if (!stored()) apply(e.matches ? 'dark' : 'light');
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
            apply(window.YetiTheme.get());
            btn.addEventListener('click', function () {
                window.YetiTheme.toggle();
            });
        }
    });
})();
