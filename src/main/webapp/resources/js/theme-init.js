/**
 * First-paint Initialization Script
 *
 * 이 파일은 <head>에서 동기로 로드된다. 첫 페인트 전에 정해둬야
 * 화면이 번쩍이지 않는 것 두 가지를 여기서 처리한다.
 *
 *   1. 테마   — data-theme 확정 (저장된 선택 → OS 설정 → 라이트)
 *   2. 폰트   — 문 인트로의 제목을 폰트가 준비될 때까지 잠깐 미룸
 *
 * 다른 스크립트는 전부 </body> 앞에서 로드되므로 이 일을 맡길 수 없다.
 * 그때는 이미 첫 페인트가 끝난 뒤다.
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
     * 문 인트로의 제목(.door-title)은 최대 304px 이다. 이 크기에서는
     * 대체 폰트로 먼저 그려졌다가 Anton 으로 바뀌는 것이 그대로 보인다.
     * 글자 폭은 common.css 의 size-adjust 로 맞춰뒀지만, 폭이 같아도
     * 글자 모양까지 같지는 않다.
     *
     * 문이 열리기 전까지는 어차피 가려진 화면이므로, 폰트가 도착할
     * 때까지 인트로 텍스트만 잠깐 미뤄둔다. 문짝(배경)은 곧바로
     * 그려지니 빈 화면이 보이지는 않는다.
     *
     * 기다리는 대상은 Anton 과 JetBrains Mono 뿐이다. 둘은 합쳐서
     * 1.1KB 라 금방 온다. Noto Sans KR(90.8KB)까지 기다리면 문이
     * 늦게 열린다 — 한글은 크기를 맞춘 대체 폰트로 먼저 그려진다.
     */
    var FONTS_TIMEOUT_MS = 1500;
    var fontsSettled = false;

    function fontsReady() {
        if (fontsSettled) return;
        fontsSettled = true;
        root.classList.remove('fonts-pending');
    }

    // JS 가 꺼져 있으면 이 클래스가 붙지 않는다 — 그때는 원래대로 바로 보인다
    root.classList.add('fonts-pending');

    // 폰트 서버가 늦거나 막히면 영영 오지 않는다. 기다리는 데 상한을 둔다.
    setTimeout(fontsReady, FONTS_TIMEOUT_MS);

    // @font-face 는 스타일시트가 파싱된 뒤에야 등록된다. 그전에 물어보면
    // "기다릴 폰트가 없다" 는 답이 돌아와 곧바로 통과해 버린다.
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

    // 사용자가 직접 고르기 전까지는 OS 설정 변화를 따라간다
    if (window.matchMedia) {
        var mq = window.matchMedia('(prefers-color-scheme: dark)');
        var onChange = function (e) {
            if (!stored()) apply(e.matches ? 'dark' : 'light');
        };
        if (mq.addEventListener) mq.addEventListener('change', onChange);
        else if (mq.addListener) mq.addListener(onChange);
    }

    document.addEventListener('DOMContentLoaded', function () {
        // 로드 직후 transition 억제 — 초기 렌더가 흔들리지 않도록
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
