/* 프로필 — 데뷔일·생일 D-Day */

$(document).ready(function() {
    calculateDday();
    scheduleMidnightRefresh();
});

// 날짜가 넘어갈 때만 바뀌므로 자정에만 재계산
function scheduleMidnightRefresh() {
    const now = new Date();
    const nextMidnight = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1);

    // 경계에서 어제로 계산되지 않도록 1초 여유
    setTimeout(function() {
        calculateDday();
        scheduleMidnightRefresh();
    }, nextMidnight - now + 1000);
}

function calculateDday() {
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    // 데뷔일 2023-09-12
    const debutDate = new Date(2023, 8, 12); // 월은 0부터 시작
    debutDate.setHours(0, 0, 0, 0);

    // 생일 4-10
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
