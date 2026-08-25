/* 프로필 화면 — 데뷔일·생일 D-Day */

$(document).ready(function() {
    calculateDday();
    scheduleMidnightRefresh();
});

// 값이 바뀌는 순간은 날짜가 넘어갈 때뿐이라 자정에만 다시 계산한다
function scheduleMidnightRefresh() {
    const now = new Date();
    const nextMidnight = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1);

    // 경계에 딱 걸쳐 어제 날짜로 계산되지 않도록 1초 여유를 둔다
    setTimeout(function() {
        calculateDday();
        scheduleMidnightRefresh();
    }, nextMidnight - now + 1000);
}

function calculateDday() {
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    // 데뷔일: 2023년 9월 12일
    const debutDate = new Date(2023, 8, 12); // 월은 0부터 시작
    debutDate.setHours(0, 0, 0, 0);

    // 생일: 4월 10일
    const birthday = new Date(today.getFullYear(), 3, 10); // 월은 0부터 시작
    birthday.setHours(0, 0, 0, 0);

    if (today > birthday) {
        birthday.setFullYear(today.getFullYear() + 1);
    }

    const debutDiff = Math.floor((today - debutDate) / (1000 * 60 * 60 * 24));
    $('#debutDday').text('D+' + YetiUtil.numberFormat(debutDiff));

    const birthdayDiff = Math.floor((birthday - today) / (1000 * 60 * 60 * 24));

    if (birthdayDiff === 0) {
        $('#birthdayDday').text('🎉 TODAY!');
    } else {
        $('#birthdayDday').text('D-' + YetiUtil.numberFormat(birthdayDiff));
    }
}
