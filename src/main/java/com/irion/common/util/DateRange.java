package com.irion.common.util;

import java.util.Date;

/**
 * 조회 기간 제한.
 *
 * 목록 API 의 start / end 는 화면이 실어 보내는 값이라 그대로 믿을 수 없다.
 * 상한이 없으면 ?start=1900-01-01&amp;end=2999-12-31 요청 하나로 매번 테이블
 * 전체를 훑게 만들 수 있다. 지금은 일정이 몇 건 안 되지만, 쌓인 뒤에는
 * 그 한 줄이 그대로 부하가 된다.
 *
 * 거부하지 않고 끝 날짜를 당긴다. 두 목록 API 는 달력(FullCalendar)이 그대로
 * 먹는 배열을 돌려주기로 되어 있어서, 여기에 오류 응답을 끼워 넣으면 화면이
 * 빈 달력이 아니라 깨진 상태가 된다. 달력이 실제로 요청하는 폭은 아무리
 * 넓어도 1년 남짓이라, 상한에 걸리는 요청은 사람이 만든 것이 아니다.
 */
public final class DateRange {

    /**
     * 한 번에 조회할 수 있는 최대 기간.
     *
     * 달력의 연간 보기는 앞뒤로 몇 주씩 더 붙여 요청하므로 1년(365일)에
     * 딱 맞추면 정상 요청이 잘린다. 여유를 둬서 400일.
     */
    public static final long MAX_SPAN_MILLIS = 400L * 24 * 60 * 60 * 1000L;

    private DateRange() {
    }

    /**
     * 끝 날짜를 상한 안으로 당긴다.
     *
     * end 가 start 보다 앞서면 start 로 맞춘다 — 결과가 비게 되고,
     * 뒤집힌 범위가 SQL 로 그대로 내려가지 않는다.
     */
    public static Date clampEnd(Date start, Date end) {
        if (start == null || end == null) {
            return end;
        }

        if (end.before(start)) {
            return start;
        }

        // 음수면 뺄셈이 넘친 것이다 (수백만 년 폭). 그것도 상한으로 본다.
        long span = end.getTime() - start.getTime();
        if (span < 0 || span > MAX_SPAN_MILLIS) {
            return new Date(start.getTime() + MAX_SPAN_MILLIS);
        }

        return end;
    }
}
