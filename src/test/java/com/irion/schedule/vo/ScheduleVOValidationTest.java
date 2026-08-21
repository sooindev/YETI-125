package com.irion.schedule.vo;

import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Date;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * ScheduleVO 제약.
 *
 * 검증기는 운영과 같은 방식(EL 없는 보간기)으로 만든다.
 * servlet-context.xml 의 validator 빈 설정과 짝을 맞춘 것이다.
 */
public class ScheduleVOValidationTest {

    private static Validator validator;

    @BeforeClass
    public static void setUp() {
        validator = Validation.byProvider(HibernateValidator.class)
                .configure()
                .messageInterpolator(new ParameterMessageInterpolator())
                .buildValidatorFactory()
                .getValidator();
    }

    @Test
    public void 제대로_채운_일정은_통과한다() {
        assertTrue(validator.validate(valid()).isEmpty());
    }

    @Test
    public void 제목이_비면_막는다() {
        ScheduleVO vo = valid();
        vo.setTitle("   ");

        assertEquals("제목을 입력해 주세요.", firstMessage(vo));
    }

    @Test
    public void 제목이_null_이면_막는다() {
        ScheduleVO vo = valid();
        vo.setTitle(null);

        assertEquals("제목을 입력해 주세요.", firstMessage(vo));
    }

    @Test
    public void 제목_200자는_통과하고_201자는_막는다() {
        ScheduleVO ok = valid();
        ok.setTitle(repeat('가', 200));
        assertTrue(validator.validate(ok).isEmpty());

        ScheduleVO tooLong = valid();
        tooLong.setTitle(repeat('가', 201));
        assertEquals("제목은 200자를 넘을 수 없습니다.", firstMessage(tooLong));
    }

    @Test
    public void 시작_일시가_없으면_막는다() {
        ScheduleVO vo = valid();
        vo.setStartDate(null);

        assertEquals("시작 일시를 선택해 주세요.", firstMessage(vo));
    }

    @Test
    public void 알_수_없는_유형을_막는다() {
        ScheduleVO vo = valid();
        vo.setScheduleType("DROP TABLE");

        assertEquals("알 수 없는 일정 유형입니다.", firstMessage(vo));
    }

    @Test
    public void 색상_형식을_막는다() {
        ScheduleVO vo = valid();
        vo.setColor("red; background:url(x)");

        assertFalse(validator.validate(vo).isEmpty());
    }

    /**
     * 드래그로 일정을 옮길 때 FullCalendar 가 색이나 공개 여부를 비워
     * 보낼 수 있다. 값이 없는 것과 잘못된 것은 다르게 다룬다.
     */
    @Test
    public void 선택_항목은_비어_있어도_통과한다() {
        ScheduleVO vo = valid();
        vo.setColor("");
        vo.setDisplayYn("");
        vo.setAllDayYn("");
        vo.setScheduleType("");
        vo.setDescription("");

        assertTrue(String.valueOf(validator.validate(vo)), validator.validate(vo).isEmpty());
    }

    @Test
    public void 선택_항목은_null_이어도_통과한다() {
        ScheduleVO vo = valid();
        vo.setColor(null);
        vo.setDisplayYn(null);
        vo.setAllDayYn(null);
        vo.setScheduleType(null);
        vo.setDescription(null);
        vo.setEndDate(null);

        assertTrue(validator.validate(vo).isEmpty());
    }

    @Test
    public void 설명_길이_상한을_지킨다() {
        ScheduleVO vo = valid();
        vo.setDescription(repeat('가', 5001));

        assertEquals("설명은 5000자를 넘을 수 없습니다.", firstMessage(vo));
    }

    // ── 헬퍼 ─────────────────────────────────────

    private static ScheduleVO valid() {
        ScheduleVO vo = new ScheduleVO();
        vo.setTitle("이리온's 첫 합방");
        vo.setDescription("설명");
        vo.setScheduleType("COLLAB");
        vo.setStartDate(new Date());
        vo.setEndDate(new Date());
        vo.setAllDayYn("N");
        vo.setDisplayYn("Y");
        vo.setColor("#d68fb0");
        return vo;
    }

    private String firstMessage(ScheduleVO vo) {
        Set<ConstraintViolation<ScheduleVO>> violations = validator.validate(vo);
        assertFalse("위반이 하나는 있어야 한다", violations.isEmpty());
        return violations.iterator().next().getMessage();
    }

    private static String repeat(char c, int times) {
        StringBuilder sb = new StringBuilder(times);
        for (int i = 0; i < times; i++) sb.append(c);
        return sb.toString();
    }
}
