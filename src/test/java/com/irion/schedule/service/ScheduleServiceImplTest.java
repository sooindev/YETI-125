package com.irion.schedule.service;

import com.irion.schedule.mapper.ScheduleMapper;
import com.irion.schedule.service.impl.ScheduleServiceImpl;
import com.irion.schedule.vo.ScheduleVO;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Date;

import static org.junit.Assert.*;

/**
 * 저장 직전 기본값 채우기.
 *
 * INSERT/UPDATE 가 컬럼을 직접 지정하면 schema.sql 의 DEFAULT 는 쓰이지 않는다.
 * NOT NULL 컬럼이 null 로 내려가면 그대로 DB 오류다.
 */
public class ScheduleServiceImplTest {

    /** DB 로 내려가기 직전의 VO 를 붙잡아 두는 가짜 매퍼 */
    private static final class Captor {
        ScheduleVO saved;
    }

    @Test
    public void 등록할_때_유형이_비어_있으면_STREAM_으로_채운다() {
        Captor captor = new Captor();
        ScheduleService service = service(captor);

        ScheduleVO schedule = new ScheduleVO();
        schedule.setTitle("합방");
        schedule.setStartDate(new Date());
        // scheduleType 을 넣지 않는다

        service.createSchedule(schedule);

        assertEquals("STREAM", captor.saved.getScheduleType());
    }

    @Test
    public void 등록할_때_나머지_기본값도_함께_채운다() {
        Captor captor = new Captor();
        ScheduleService service = service(captor);

        ScheduleVO schedule = new ScheduleVO();
        schedule.setTitle("합방");
        schedule.setStartDate(new Date());

        service.createSchedule(schedule);

        assertEquals("N", captor.saved.getAllDayYn());
        assertEquals("Y", captor.saved.getDisplayYn());
        assertEquals("#6366F1", captor.saved.getColor());
    }

    @Test
    public void 보낸_값이_있으면_기본값으로_덮어쓰지_않는다() {
        Captor captor = new Captor();
        ScheduleService service = service(captor);

        ScheduleVO schedule = new ScheduleVO();
        schedule.setTitle("비공개 준비");
        schedule.setStartDate(new Date());
        schedule.setScheduleType("KARAOKE");
        schedule.setAllDayYn("Y");
        schedule.setDisplayYn("N");
        schedule.setColor("#FF0000");

        service.createSchedule(schedule);

        assertEquals("KARAOKE", captor.saved.getScheduleType());
        assertEquals("Y", captor.saved.getAllDayYn());
        assertEquals("N", captor.saved.getDisplayYn());
        assertEquals("#FF0000", captor.saved.getColor());
    }

    /** @Pattern 이 빈 문자열을 허용해서, '' 가 NOT NULL 을 통과해 눌러앉는다 */
    @Test
    public void 빈_문자열도_기본값으로_바꾼다() {
        Captor captor = new Captor();
        ScheduleService service = service(captor);

        ScheduleVO schedule = new ScheduleVO();
        schedule.setTitle("합방");
        schedule.setStartDate(new Date());
        schedule.setScheduleType("");
        schedule.setAllDayYn("");
        schedule.setDisplayYn("");

        service.createSchedule(schedule);

        assertEquals("STREAM", captor.saved.getScheduleType());
        assertEquals("N", captor.saved.getAllDayYn());
        assertEquals("Y", captor.saved.getDisplayYn());
    }

    /** 수정 경로에도 같은 처리가 필요하다 — UPDATE 문이 NOT NULL 컬럼을 그대로 덮어쓴다 */
    @Test
    public void 수정할_때도_기본값을_채운다() {
        Captor captor = new Captor();
        ScheduleService service = service(captor);

        ScheduleVO schedule = new ScheduleVO();
        schedule.setScheduleId(7L);
        schedule.setTitle("제목만 고침");
        schedule.setStartDate(new Date());

        service.updateSchedule(schedule);

        assertEquals("STREAM", captor.saved.getScheduleType());
        assertEquals("N", captor.saved.getAllDayYn());
    }

    /**
     * 'Y' 로 채우면 숨겨둔 일정이 공개되고 'N' 이면 공개하던 일정이 사라진다.
     * 비운 채로 넘겨 SQL 이 그 컬럼을 건드리지 않게 한다.
     */
    @Test
    public void 수정할_때_공개여부가_없으면_비운_채로_넘긴다() {
        Captor captor = new Captor();
        ScheduleService service = service(captor);

        ScheduleVO schedule = new ScheduleVO();
        schedule.setScheduleId(7L);
        schedule.setTitle("제목만 고침");
        schedule.setStartDate(new Date());
        // displayYn 을 넣지 않는다

        service.updateSchedule(schedule);

        assertNull("기본값을 채우면 DB 의 기존 값을 덮어쓰게 된다",
                captor.saved.getDisplayYn());
    }

    @Test
    public void 수정할_때_공개여부를_보내면_그대로_쓴다() {
        Captor captor = new Captor();
        ScheduleService service = service(captor);

        ScheduleVO schedule = new ScheduleVO();
        schedule.setScheduleId(7L);
        schedule.setTitle("숨기기");
        schedule.setStartDate(new Date());
        schedule.setDisplayYn("N");

        service.updateSchedule(schedule);

        assertEquals("N", captor.saved.getDisplayYn());
    }

    // ========================================

    /** 가짜 매퍼를 꽂은 서비스. 스프링 없이 필드에 직접 넣는다. */
    private static ScheduleService service(Captor captor) {
        ScheduleMapper mapper = (ScheduleMapper) Proxy.newProxyInstance(
                ScheduleMapper.class.getClassLoader(),
                new Class<?>[] { ScheduleMapper.class },
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("insertSchedule".equals(name) || "updateSchedule".equals(name)) {
                        ScheduleVO vo = (ScheduleVO) args[0];
                        captor.saved = vo;
                        vo.setScheduleId(vo.getScheduleId() == null ? 1L : vo.getScheduleId());
                        return 1;
                    }
                    return method.getReturnType() == int.class ? 0 : null;
                });

        ScheduleServiceImpl service = new ScheduleServiceImpl();
        try {
            Field field = ScheduleServiceImpl.class.getDeclaredField("scheduleMapper");
            field.setAccessible(true);
            field.set(service, mapper);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("scheduleMapper 필드를 찾지 못했다", e);
        }
        return service;
    }
}
