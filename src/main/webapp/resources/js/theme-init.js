/**
 * Theme Initialization Script
 * 다크/라이트 테마 결정 및 전환
 *
 * 이 파일은 <head>에서 동기로 로드된다. 첫 페인트 전에
 * data-theme 을 확정해야 새로고침 때 흰 화면이 번쩍이지 않는다.
 * 순서: 저장된 선택 → OS 설정 → 라이트
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
