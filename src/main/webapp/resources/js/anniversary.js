/* 데뷔 3주년 축하 연출 — 접속 폭죽, 축하 팝업, 커서·배경 반짝이 */

(function () {
    'use strict';

    /* ---- 설정 -------------------------------------------------
       연출을 켜 두는 기간. 이 창을 벗어나면 아무것도 하지 않으므로,
       기념 주간이 끝나면 사이트는 저절로 평소 모습으로 돌아간다.
       (아예 걷어내려면 각 JSP 의 anniversary.css / anniversary.js 두 줄을 지운다)
       ----------------------------------------------------------- */
    const DEBUT_TEXT  = '2023.09.12';   // 이리온 데뷔일
    const ANNIV_TEXT  = '2026.09.12';   // 3주년 당일
    const ANNIV_DATE  = '2026-09-12';   // D-day 계산용
    const SHOW_FROM   = '2026-09-05';
    const SHOW_TO     = '2026-09-19';   // 이 날까지 (당일 포함)

    // 축하 인사는 하루에 한 번. door-intro.js 와 같은 방식으로 '본 날짜' 를 남긴다
    const SEEN_KEY = 'anniv_3rd_popup_date';

    const CONFETTI_MS    = 3500;  // 폭죽을 쏘는 시간. 남은 알갱이는 그 뒤 알아서 떨어진다
    const POPUP_DELAY_MS = 900;   // 폭죽이 먼저 터지고, 뒤이어 팝업이 뜬다
    const INTRO_DELAY_MS = 500;   // 문이 걷히기 시작한 뒤 기다리는 시간

    /* ---- 날짜 -------------------------------------------------
       door-intro.js 와 같은 이유로 로컬 기준 문자열을 쓴다 —
       toISOString() 은 한국에서 오전 9시에 날짜가 바뀐다.
       ----------------------------------------------------------- */

    function todayKey() {
        const now = new Date();
        const pad = function (n) { return String(n).padStart(2, '0'); };
        return now.getFullYear() + '-' + pad(now.getMonth() + 1) + '-' + pad(now.getDate());
    }

    function inShowWindow() {
        const t = todayKey();   // YYYY-MM-DD 는 사전순 비교가 곧 날짜순 비교다
        return t >= SHOW_FROM && t <= SHOW_TO;
    }

    // 사파리 프라이빗 모드에서는 접근 자체가 예외라 감싸지 않으면 연출이 통째로 멈춘다
    function popupSeenToday() {
        try {
            return localStorage.getItem(SEEN_KEY) === todayKey();
        } catch (e) {
            return false;
        }
    }

    function rememberPopupShown() {
        try {
            localStorage.setItem(SEEN_KEY, todayKey());
        } catch (e) {}
    }

    /** 3주년까지 남은 날. 당일이면 0, 지났으면 음수 */
    function daysUntilAnniv() {
        const parts = ANNIV_DATE.split('-');
        const target = new Date(+parts[0], +parts[1] - 1, +parts[2]);
        const now = new Date();
        const midnight = new Date(now.getFullYear(), now.getMonth(), now.getDate());
        return Math.round((target - midnight) / 86400000);
    }

    if (!inShowWindow()) return;

    // 오늘 이미 본 사람에게는 팝업만 빠진다 — 폭죽과 반짝이는 그대로다
    const popupShownToday = popupSeenToday();

    /* 폭죽은 홈에서만 터진다. index.jsp 의 <script ... data-confetti="on"> 이 스위치다 —
       주소로 홈을 알아내면 컨텍스트 패스나 경로가 바뀔 때 같이 깨진다.
       currentScript 는 이 스크립트가 실행되는 지금만 가리키므로 여기서 붙잡아 둔다. */
    const confettiEnabled = !!(document.currentScript
            && document.currentScript.getAttribute('data-confetti') === 'on');

    const reduceMotion = !!(window.matchMedia
            && window.matchMedia('(prefers-reduced-motion: reduce)').matches);

    /* ---- 알갱이 캔버스 -----------------------------------------
       폭죽과 반짝이가 캔버스 하나와 루프 하나를 같이 쓴다.
       알갱이가 하나도 없으면 루프를 멈추므로 평소에는 아무것도 돌지 않는다.
       ----------------------------------------------------------- */

    const particles = [];
    let canvas = null;
    let ctx = null;
    let rafId = null;
    let lastTs = 0;
    let vw = 0;
    let vh = 0;

    function initCanvas() {
        canvas = document.createElement('canvas');
        canvas.className = 'anniv-canvas';
        canvas.setAttribute('aria-hidden', 'true');
        document.body.appendChild(canvas);
        ctx = canvas.getContext('2d');
        resizeCanvas();
        // 주소창이 접혔다 펴지는 모바일에서도 좌표가 어긋나지 않게 다시 잡는다
        window.addEventListener('resize', resizeCanvas, { passive: true });
    }

    function resizeCanvas() {
        // 3배 이상은 눈에 띄는 차이 없이 그리는 양만 늘어난다
        const dpr = Math.min(window.devicePixelRatio || 1, 2);
        vw = window.innerWidth;
        vh = window.innerHeight;
        canvas.width = Math.round(vw * dpr);
        canvas.height = Math.round(vh * dpr);
        canvas.style.width = vw + 'px';
        canvas.style.height = vh + 'px';
        ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    }

    function startLoop() {
        if (rafId === null) rafId = window.requestAnimationFrame(tick);
    }

    function tick(ts) {
        rafId = null;

        // 60fps 한 프레임을 1 로 본다. 탭이 잠깐 멈췄다 돌아와도 순간이동하지 않게 3 프레임에서 자른다
        const dt = lastTs ? Math.min((ts - lastTs) / 16.67, 3) : 1;
        lastTs = ts;

        ctx.clearRect(0, 0, vw, vh);

        for (let i = particles.length - 1; i >= 0; i--) {
            const p = particles[i];

            p.life += dt;
            p.vy += p.gravity * dt;
            p.vx *= Math.pow(p.drag, dt);
            p.vy *= Math.pow(p.drag, dt);
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            p.rot += p.vrot * dt;

            if (p.life >= p.maxLife || p.y - p.size > vh + 40) {
                particles.splice(i, 1);
                continue;
            }
            drawParticle(p);
        }

        ctx.globalAlpha = 1;

        if (particles.length) startLoop();
        else lastTs = 0;   // 다음에 다시 시작할 때 큰 dt 가 튀지 않도록
    }

    function drawParticle(p) {
        const left = p.maxLife - p.life;
        const fade = left < p.fade ? Math.max(0, left / p.fade) : 1;

        ctx.globalAlpha = p.alpha * fade;
        ctx.fillStyle = p.color;
        ctx.save();
        ctx.translate(p.x, p.y);
        ctx.rotate(p.rot);

        if (p.shape === 'ring') {
            // 클릭 자리에서 퍼지는 테두리 원. 굵기는 그대로 두고 반지름만 키운다
            ctx.strokeStyle = p.color;
            ctx.lineWidth = p.line;
            ctx.beginPath();
            ctx.arc(0, 0, p.size + p.grow * p.life, 0, Math.PI * 2);
            ctx.stroke();
        } else if (p.shape === 'star') {
            // 오목한 네 꼭짓점 — 사라지면서 작아진다
            const r = p.size * (1 - 0.45 * (p.life / p.maxLife));
            ctx.beginPath();
            ctx.moveTo(0, -r);
            ctx.quadraticCurveTo(0, 0, r, 0);
            ctx.quadraticCurveTo(0, 0, 0, r);
            ctx.quadraticCurveTo(0, 0, -r, 0);
            ctx.quadraticCurveTo(0, 0, 0, -r);
            ctx.fill();
        } else {
            // 가로만 눌렀다 펴서 종이가 팔랑이는 느낌을 낸다
            ctx.scale(Math.cos(p.flutter + p.life * 0.14), 1);
            if (p.shape === 'circle') {
                ctx.beginPath();
                ctx.arc(0, 0, p.size * 0.42, 0, Math.PI * 2);
                ctx.fill();
            } else {
                ctx.fillRect(-p.size / 2, -p.size * 0.34, p.size, p.size * 0.68);
            }
        }

        ctx.restore();
    }

    /* ---- 색 — 테마 토큰에서 그대로 가져온다 (다크에서도 뜬다) ---- */

    function readPalette() {
        const cs = getComputedStyle(document.documentElement);
        function token(name, fallback) {
            return (cs.getPropertyValue(name) || '').trim() || fallback;
        }
        return {
            confetti: [
                token('--frost', '#4ea8d8'),
                token('--frost-deep', '#2f87b8'),
                token('--rose', '#ec7fa9'),
                token('--rose-deep', '#d65f8e'),
                token('--t-song', '#e0a94f'),
                token('--ink-raise', '#ffffff')
            ],
            spark: [
                token('--frost', '#4ea8d8'),
                token('--rose', '#ec7fa9'),
                token('--t-song', '#e0a94f')
            ]
        };
    }

    let colors = null;

    function pick(list) {
        return list[(Math.random() * list.length) | 0];
    }

    /* ---- 폭죽 -------------------------------------------------- */

    /** 한 지점에서 부채꼴로 쏜다. angle/spread 는 라디안, 캔버스 좌표라 위쪽이 음수다 */
    function cannon(x, y, angle, spread, count, power) {
        for (let i = 0; i < count; i++) {
            const a = angle + (Math.random() - 0.5) * spread;
            const v = power * (0.55 + Math.random() * 0.7);
            particles.push({
                shape: Math.random() < 0.22 ? 'circle' : 'rect',
                x: x, y: y,
                vx: Math.cos(a) * v,
                vy: Math.sin(a) * v,
                gravity: 0.24 + Math.random() * 0.1,
                drag: 0.985,
                size: 7 + Math.random() * 6,
                color: pick(colors.confetti),
                rot: Math.random() * Math.PI * 2,
                vrot: (Math.random() - 0.5) * 0.3,
                flutter: Math.random() * Math.PI * 2,
                alpha: 1,
                life: 0,
                maxLife: 260 + Math.random() * 90,
                fade: 60
            });
        }
        startLoop();
    }

    /** 화면 위에서 천천히 내려오는 조각 — 발사 사이를 메운다 */
    function drift(count) {
        for (let i = 0; i < count; i++) {
            particles.push({
                shape: Math.random() < 0.3 ? 'circle' : 'rect',
                x: Math.random() * vw,
                y: -20 - Math.random() * 60,
                vx: (Math.random() - 0.5) * 1.6,
                vy: 1.6 + Math.random() * 1.8,
                gravity: 0.05,
                drag: 0.995,
                size: 5 + Math.random() * 5,
                color: pick(colors.confetti),
                rot: Math.random() * Math.PI * 2,
                vrot: (Math.random() - 0.5) * 0.16,
                flutter: Math.random() * Math.PI * 2,
                alpha: 0.9,
                life: 0,
                maxLife: 420,
                fade: 60
            });
        }
        startLoop();
    }

    function fireConfetti() {
        // 화면 높이에 맞춘 발사력. 중간 세기 알갱이가 화면의 3분의 2쯤까지 올라간다
        const power = Math.max(18, Math.min(vh, 1300) / 28);

        function volley() {
            cannon(vw * 0.06, vh + 10, -Math.PI / 3, 0.9, 26, power);          // 왼쪽 아래 → 오른쪽 위
            cannon(vw * 0.94, vh + 10, -Math.PI * 2 / 3, 0.9, 26, power);      // 오른쪽 아래 → 왼쪽 위
        }

        volley();
        setTimeout(volley, 850);
        setTimeout(volley, 1750);

        const drizzle = setInterval(function () {
            drift(3);
        }, 180);
        setTimeout(function () {
            clearInterval(drizzle);
        }, CONFETTI_MS - 900);

        // 남은 조각을 한꺼번에 거둔다. 이게 없으면 늦게 뜬 조각이 8초 넘게 흩날린다.
        // 반짝이(star)는 계속 살아 있어야 하므로 폭죽 조각만 고른다.
        setTimeout(function () {
            for (let i = 0; i < particles.length; i++) {
                const p = particles[i];
                if (p.shape === 'star') continue;
                p.fade = 50;
                p.maxLife = Math.min(p.maxLife, p.life + 50);
            }
        }, CONFETTI_MS);
    }

    /* ---- 반짝이 — 커서를 따라, 그리고 배경에 드문드문 ------------- */

    function sparkle(x, y, size, alpha) {
        particles.push({
            shape: 'star',
            x: x, y: y,
            vx: (Math.random() - 0.5) * 0.5,
            vy: -0.25 - Math.random() * 0.3,
            gravity: -0.004,        // 살짝 떠오른다
            drag: 0.97,
            size: size,
            color: pick(colors.spark),
            rot: Math.random() * Math.PI / 2,
            vrot: (Math.random() - 0.5) * 0.05,
            flutter: 0,
            alpha: alpha,
            life: 0,
            maxLife: 45 + Math.random() * 25,
            fade: 30
        });
        startLoop();
    }

    /** 클릭한 자리에서 링 하나가 퍼지고 파편이 튄다. 0.5초 안에 끝난다 */
    function clickBurst(x, y) {
        particles.push({
            shape: 'ring',
            x: x, y: y,
            vx: 0, vy: 0, gravity: 0, drag: 1,
            size: 5,
            grow: 0.85,          // 프레임마다 커지는 반지름
            line: 1.6,
            color: colors.spark[0],
            rot: 0, vrot: 0, flutter: 0,
            alpha: 0.75,
            life: 0,
            maxLife: 26,
            fade: 26             // fade 를 수명과 같게 두면 사는 내내 옅어진다
        });

        for (let i = 0; i < 9; i++) {
            const a = (Math.PI * 2 / 9) * i + Math.random() * 0.5;
            const v = 2 + Math.random() * 2.6;
            particles.push({
                shape: Math.random() < 0.5 ? 'star' : 'circle',
                x: x, y: y,
                vx: Math.cos(a) * v,
                vy: Math.sin(a) * v,
                gravity: 0.06,
                drag: 0.9,       // 금세 멎는다 — 튀어 나갔다 그 자리에서 사라진다
                size: 3 + Math.random() * 3,
                color: pick(colors.spark),
                rot: Math.random() * Math.PI,
                vrot: (Math.random() - 0.5) * 0.2,
                flutter: Math.random() * Math.PI * 2,
                alpha: 0.9,
                life: 0,
                maxLife: 26 + Math.random() * 12,
                fade: 20
            });
        }
        startLoop();
    }

    function initSparkles() {
        let lastAt = 0;
        let lastX = 0;
        let lastY = 0;

        // 손가락에는 커서가 없다. 터치 기기에서는 배경 반짝이만 남는다
        const finePointer = !!(window.matchMedia
                && window.matchMedia('(hover: hover) and (pointer: fine)').matches);

        if (finePointer) {
            document.addEventListener('pointermove', function (e) {
                const now = Date.now();
                if (now - lastAt < 55) return;

                const dx = e.clientX - lastX;
                const dy = e.clientY - lastY;
                lastX = e.clientX;
                lastY = e.clientY;

                // 거의 멈춰 있으면 만들지 않는다 — 커서 자리에 반짝이가 쌓이는 걸 막는다
                if (dx * dx + dy * dy < 36) return;

                lastAt = now;
                sparkle(e.clientX + (Math.random() - 0.5) * 10,
                        e.clientY + (Math.random() - 0.5) * 10,
                        4 + Math.random() * 4, 0.85);
            }, { passive: true });
        }

        /* 클릭 이펙트.
           손가락은 스크롤을 시작할 때도 pointerdown 을 내므로, 터치는 탭이 끝난
           뒤(click)에만 터뜨린다 — 안 그러면 넘길 때마다 파편이 튄다. */
        let lastPointerType = 'mouse';

        document.addEventListener('pointerdown', function (e) {
            lastPointerType = e.pointerType || 'mouse';
            if (!e.isPrimary || e.button !== 0) return;   // 오른쪽·가운데 버튼은 뺀다
            if (lastPointerType === 'touch') return;
            clickBurst(e.clientX, e.clientY);
        }, { passive: true });

        document.addEventListener('click', function (e) {
            if (lastPointerType !== 'touch') return;      // 마우스는 위에서 이미 터뜨렸다
            // 키보드(Enter/Space)로 눌린 클릭은 좌표가 0 이라 화면 구석에서 터진다
            if (!e.clientX && !e.clientY) return;
            clickBurst(e.clientX, e.clientY);
        }, { passive: true });

        // 배경 반짝이. 사람이 "어, 방금 뭐 반짝였나" 할 정도로만 띄운다
        setInterval(function () {
            if (document.hidden) return;
            sparkle(Math.random() * vw, Math.random() * vh * 0.92,
                    5 + Math.random() * 5, 0.5);
        }, 1400);
    }

    /* ---- 축하 팝업 ---------------------------------------------- */

    let pop = null;

    function dateLine() {
        const left = daysUntilAnniv();
        let line = DEBUT_TEXT + ' → ' + ANNIV_TEXT;
        if (left > 0) line += ' · D-' + left;
        else if (left === 0) line += ' · D-DAY';
        return line;
    }

    function buildPopup() {
        pop = document.createElement('div');
        pop.className = 'anniv-pop';
        pop.setAttribute('role', 'dialog');
        // 뒤쪽 페이지를 막지 않는 안내창이라 modal 이 아니다
        pop.setAttribute('aria-modal', 'false');
        pop.setAttribute('aria-labelledby', 'annivPopTitle');

        pop.innerHTML =
            '<button type="button" class="anniv-pop-close" aria-label="축하 팝업 닫기">&times;</button>' +
            '<span class="anniv-pop-kicker">Anniversary / 3rd</span>' +
            '<p class="anniv-pop-date"></p>' +
            '<h2 class="anniv-pop-title" id="annivPopTitle">이리온 데뷔 3주년 <em>축하드립니다!</em> 🥳</h2>' +
            '<p class="anniv-pop-desc">눈이 내린 지 세 번째 해입니다.<br>' +
            '함께 걸어온 예티들과 이리온에게 축하를 전합니다.</p>' +
            '<div class="anniv-pop-actions">' +
            '  <button type="button" class="btn btn-primary anniv-pop-ok">고마워요 <span class="btn-arrow">→</span></button>' +
            '</div>';

        pop.querySelector('.anniv-pop-date').textContent = dateLine();

        pop.querySelector('.anniv-pop-close').addEventListener('click', function () {
            closePopup();
        });
        pop.querySelector('.anniv-pop-ok').addEventListener('click', function () {
            closePopup();
        });

        document.body.appendChild(pop);
    }

    function showPopup() {
        pop.classList.add('show');
        // 닫을 때가 아니라 뜨는 순간 기록한다 — 안 닫고 페이지를 옮겨도 그날은 다시 뜨지 않는다
        rememberPopupShown();
    }

    function closePopup() {
        pop.classList.remove('show');
    }

    // 공용 모달의 ESC 처리와 겹치지 않는다 — 서로 자기 것만 닫는다
    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape' && pop && pop.classList.contains('show')) {
            closePopup();
        }
    });

    /* ---- 시작 순서 ---------------------------------------------- */

    function ready(fn) {
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', fn);
        } else {
            fn();
        }
    }

    /** 백그라운드 탭에서는 requestAnimationFrame 이 멈춰 폭죽만 낭비된다. 돌아올 때까지 미룬다 */
    function whenVisible(fn) {
        if (!document.hidden) {
            fn();
            return;
        }
        const onVisible = function () {
            if (document.hidden) return;
            document.removeEventListener('visibilitychange', onVisible);
            fn();
        };
        document.addEventListener('visibilitychange', onVisible);
    }

    /** 홈의 문 인트로가 걷힌 뒤에 터뜨린다 — 문 뒤에서 터지면 아무도 못 본다 */
    function whenIntroGone(fn) {
        const intro = document.querySelector('.door-intro');
        if (!intro || intro.classList.contains('hidden')) {
            fn();
            return;
        }

        const observer = new MutationObserver(function () {
            // fading 이 붙는 순간이 문이 다 열리고 화면이 드러나기 시작하는 때다
            if (!intro.isConnected
                    || intro.classList.contains('fading')
                    || intro.classList.contains('hidden')) {
                observer.disconnect();
                setTimeout(fn, INTRO_DELAY_MS);
            }
        });
        observer.observe(intro, { attributes: true, attributeFilter: ['class'] });
        observer.observe(document.body, { childList: true });
    }

    /**
     * 캔버스와 반짝이만 켠다. 폭죽·팝업을 건너뛰는 날에도 이것만은 남는다.
     * 움직임을 줄이도록 설정한 사용자에게는 아무것도 켜지 않는다(false 를 돌려준다).
     */
    function startSparkles() {
        if (reduceMotion) return false;
        colors = readPalette();
        initCanvas();
        initSparkles();
        return true;
    }

    function celebrate() {
        const moving = startSparkles();

        // 폭죽은 홈에서만, 그리고 움직임을 허용한 사용자에게만
        if (moving && confettiEnabled) fireConfetti();

        // 폭죽이 먼저 터지고 뒤이어 팝업이 뜬다. 폭죽이 없으면 기다릴 이유가 없다
        if (!popupShownToday) {
            setTimeout(showPopup, (moving && confettiEnabled) ? POPUP_DELAY_MS : 300);
        }
    }

    ready(function () {
        // 오늘 이미 본 사람에게는 팝업 자체를 만들지 않는다
        if (!popupShownToday) buildPopup();

        whenVisible(function () {
            whenIntroGone(celebrate);
        });
    });
})();
