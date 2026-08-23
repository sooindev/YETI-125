package com.irion.common.util;

import java.util.Date;

/**
 * 조회 기간 제한.
 *
 * 목록 API 의 start / end 는 클라이언트가 정하는 값이라, 상한이 없으면
 * 요청 한 줄로 테이블 전체를 훑게 만들 수 있다.
 *
 * 거부하지 않고 끝 날짜를 당긴다. 두 목록 API 는 FullCalendar 가 그대로 먹는
 * 배열을 돌려주기로 되어 있어서 오류 응답을 끼워 넣으면 달력이 깨진다.
 */
public final class DateRange {

    /** 연간 보기가 앞뒤로 몇 주씩 더 붙여 요청하므로 365일이 아니라 400일 */
    public static final long MAX_SPAN_MILLIS = 400L * 24 * 60 * 60 * 1000L;

    private DateRange() {
    }

    /** 끝 날짜를 상한 안으로 당긴다. 뒤집힌 범위는 start 로 맞춰 빈 결과가 되게 한다 */
    public static Date clampEnd(Date start, Date end) {
        if (start == null || end == null) {
            return end;
        }

        if (end.before(start)) {
            return start;
        }

        // 음수면 뺄셈이 넘친 것이다. 그것도 상한으로 본다
        long span = end.getTime() - start.getTime();
        if (span < 0 || span > MAX_SPAN_MILLIS) {
            return new Date(start.getTime() + MAX_SPAN_MILLIS);
        }

        return end;
    }
}
