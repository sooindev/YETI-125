package com.irion.schedule.controller;

import com.irion.common.util.DateRange;
import com.irion.schedule.service.ScheduleService;
import com.irion.schedule.vo.ScheduleVO;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * 공개 일정 API 와 관리자 일정 API 의 차이를 못 박는다.
 *
 * 둘은 코드가 거의 같아서 한쪽을 고칠 때 다른 쪽을 따라 고치기 쉽다.
 * 공개 쪽이 숨긴 일정을 흘리거나 공개 여부를 응답에 실으면 여기서 걸린다.
 */
public class ScheduleControllerTest {

    /** 서비스가 어떤 메서드로 어떤 기간을 받았는지 붙잡아 둔다 */
    private static final class Captor {
        String calledMethod;
        Date startDate;
        Date endDate;
        List<ScheduleVO> reply = new ArrayList<ScheduleVO>();
    }

    // ── 공개 API ────────────────────────────────────────────────

    /** 공개 목록은 display_yn 으로 거르는 질의를 써야 한다 */
    @Test
    public void 공개_목록은_공개_전용_조회를_쓴다() {
        Captor captor = new Captor();
        ScheduleController controller = publicController(captor);

        controller.getScheduleList(day(2026, 1, 1), day(2026, 2, 1));

        assertEquals("getDisplayScheduleList", captor.calledMethod);
    }

    /** displayYn 이 실려 나가면 어떤 일정이 숨겨져 있는지가 밖에서 보인다 */
    @Test
    public void 공개_응답에는_공개여부가_없다() {
        Captor captor = new Captor();
        captor.reply.add(schedule("정기 방송", "Y"));
        ScheduleController controller = publicController(captor);

        Map<String, Object> event = controller.getScheduleList(day(2026, 1, 1), day(2026, 2, 1)).get(0);

        assertFalse("공개 응답에 displayYn 이 있으면 안 된다: " + event.keySet(),
                event.containsKey("displayYn"));
        assertEquals("정기 방송", event.get("title"));
    }

    // ── 관리자 API ──────────────────────────────────────────────

    @Test
    public void 관리자_목록은_전체_조회를_쓴다() {
        Captor captor = new Captor();
        AdminScheduleController controller = adminController(captor);

        controller.getScheduleList(day(2026, 1, 1), day(2026, 2, 1));

        assertEquals("getScheduleList", captor.calledMethod);
    }

    /** 관리자 화면은 숨긴 일정을 흐리게 그려야 해서 공개 여부가 필요하다 */
    @Test
    public void 관리자_응답에는_공개여부가_있다() {
        Captor captor = new Captor();
        captor.reply.add(schedule("비공개 준비", "N"));
        AdminScheduleController controller = adminController(captor);

        Map<String, Object> event = controller.getScheduleList(day(2026, 1, 1), day(2026, 2, 1)).get(0);

        assertEquals("N", event.get("displayYn"));
    }

    // ── 공통 ────────────────────────────────────────────────────

    /** 상한이 없으면 ?end=9999-12-31 하나로 테이블 전체를 훑게 된다 */
    @Test
    public void 조회_기간이_너무_넓으면_상한까지만_내려간다() {
        Captor captor = new Captor();
        ScheduleController controller = publicController(captor);

        Date start = day(2026, 1, 1);
        controller.getScheduleList(start, day(2099, 12, 31));

        assertEquals("DateRange 상한이 걸려야 한다",
                start.getTime() + DateRange.MAX_SPAN_MILLIS, captor.endDate.getTime());
    }

    @Test
    public void 관리자_조회에도_같은_상한이_걸린다() {
        Captor captor = new Captor();
        AdminScheduleController controller = adminController(captor);

        Date start = day(2026, 1, 1);
        controller.getScheduleList(start, day(2099, 12, 31));

        assertEquals(start.getTime() + DateRange.MAX_SPAN_MILLIS, captor.endDate.getTime());
    }

    /** FullCalendar 는 allDay 를 불리언으로 받는다 — "Y"/"N" 을 그대로 주면 항상 참이 된다 */
    @Test
    public void 종일_여부를_불리언으로_바꾼다() {
        Captor captor = new Captor();
        captor.reply.add(schedule("종일 일정", "Y"));
        captor.reply.get(0).setAllDayYn("Y");
        ScheduleController controller = publicController(captor);

        Map<String, Object> event = controller.getScheduleList(day(2026, 1, 1), day(2026, 2, 1)).get(0);

        assertEquals(Boolean.TRUE, event.get("allDay"));
    }


    private static ScheduleController publicController(Captor captor) {
        ScheduleController controller = new ScheduleController();
        inject(controller, ScheduleController.class, service(captor));
        return controller;
    }

    private static AdminScheduleController adminController(Captor captor) {
        AdminScheduleController controller = new AdminScheduleController();
        inject(controller, AdminScheduleController.class, service(captor));
        return controller;
    }

    /** 스프링 없이 필드에 직접 꽂는다 */
    private static void inject(Object controller, Class<?> type, ScheduleService service) {
        try {
            Field field = type.getDeclaredField("scheduleService");
            field.setAccessible(true);
            field.set(controller, service);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("scheduleService 필드를 찾지 못했다", e);
        }
    }

    private static ScheduleService service(Captor captor) {
        return (ScheduleService) Proxy.newProxyInstance(
                ScheduleService.class.getClassLoader(),
                new Class<?>[] { ScheduleService.class },
                (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.endsWith("ScheduleList")) {
                        captor.calledMethod = name;
                        captor.startDate = (Date) args[0];
                        captor.endDate = (Date) args[1];
                        return captor.reply;
                    }
                    return method.getReturnType() == boolean.class ? Boolean.FALSE : null;
                });
    }

    private static ScheduleVO schedule(String title, String displayYn) {
        ScheduleVO schedule = new ScheduleVO();
        schedule.setScheduleId(1L);
        schedule.setTitle(title);
        schedule.setStartDate(day(2026, 1, 15));
        schedule.setScheduleType("GAME");
        schedule.setAllDayYn("N");
        schedule.setDisplayYn(displayYn);
        schedule.setColor("#8c8fd6");
        return schedule;
    }

    private static Date day(int year, int month, int dayOfMonth) {
        Calendar calendar = new GregorianCalendar();
        calendar.clear();
        calendar.set(year, month - 1, dayOfMonth);
        return calendar.getTime();
    }
}
