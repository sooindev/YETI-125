/* ================================================
   Irion Fansite - Info Page (jQuery)
   ================================================ */

$(document).ready(function() {
    // D-Day 계산
    calculateDday();

    // 매일 자정에 D-Day 갱신
    setInterval(calculateDday, 60000);
});

// D-Day 계산
function calculateDday() {
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    // 데뷔일: 2023년 9월 12일
    const debutDate = new Date(2023, 8, 12); // 월은 0부터 시작
    debutDate.setHours(0, 0, 0, 0);

    // 생일: 4월 10일
    const birthday = new Date(today.getFullYear(), 3, 10); // 월은 0부터 시작
    birthday.setHours(0, 0, 0, 0);

    // 생일이 지났으면 내년 생일로 설정
    if (today > birthday) {
        birthday.setFullYear(today.getFullYear() + 1);
    }

    // 데뷔 D-Day 계산 (D+)
    const debutDiff = Math.floor((today - debutDate) / (1000 * 60 * 60 * 24));
    $('#debutDday').text('D+' + numberFormat(debutDiff));

    // 생일 D-Day 계산
    const birthdayDiff = Math.floor((birthday - today) / (1000 * 60 * 60 * 24));

    if (birthdayDiff === 0) {
        $('#birthdayDday').text('🎉 TODAY!');
    } else {
        $('#birthdayDday').text('D-' + numberFormat(birthdayDiff));
    }
}

// 숫자 포맷 (1000 -> 1,000)
function numberFormat(num) {
    return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',');
}