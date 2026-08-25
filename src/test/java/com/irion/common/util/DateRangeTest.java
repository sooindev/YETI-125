package com.irion.common.util;

import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.Assert.*;

/** 조회 기간 상한. 전에는 요청 한 줄로 100년치를 훑게 만들 수 있었다. */
public class DateRangeTest {

    @Test
    public void 평범한_한_달_요청은_그대로_둔다() {
        Date start = date("2026-08-01");
        Date end = date("2026-08-31");

        assertEquals(end, DateRange.clampEnd(start, end));
    }

    @Test
    public void 달력_연간_보기_폭도_그대로_둔다() {
        // 앞뒤로 몇 주씩 더 붙어도 상한(400일)에 걸리지 않아야 한다
        Date start = date("2026-01-01");
        Date end = date("2026-12-31");

        assertEquals(end, DateRange.clampEnd(start, end));
    }

    @Test
    public void 백년치_요청은_상한까지_당겨진다() {
        Date start = date("1900-01-01");
        Date end = date("2999-12-31");

        Date clamped = DateRange.clampEnd(start, end);

        assertTrue("끝 날짜가 당겨져야 한다", clamped.before(end));
        assertEquals(start.getTime() + DateRange.MAX_SPAN_MILLIS, clamped.getTime());
    }

    @Test
    public void 뒤집힌_범위는_시작으로_맞춘다() {
        Date start = date("2026-08-31");
        Date end = date("2026-08-01");

        assertEquals(start, DateRange.clampEnd(start, end));
    }

    @Test
    public void 널은_그대로_통과시킨다() {
        assertNull(DateRange.clampEnd(date("2026-08-01"), null));
        assertNotNull(DateRange.clampEnd(null, date("2026-08-01")));
    }

    private static Date date(String yyyyMMdd) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd").parse(yyyyMMdd);
        } catch (Exception e) {
            throw new IllegalArgumentException(yyyyMMdd, e);
        }
    }
}
