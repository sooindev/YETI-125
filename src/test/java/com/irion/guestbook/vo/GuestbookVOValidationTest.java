package com.irion.guestbook.vo;

import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Set;

import static org.junit.Assert.*;

/** GuestbookVO 제약. 검증기는 servlet-context.xml 과 같은 방식(EL 없는 보간기)으로 만든다. */
public class GuestbookVOValidationTest {

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
    public void 제대로_채운_방명록은_통과한다() {
        assertTrue(validator.validate(valid()).isEmpty());
    }

    /** 방명록은 전부 익명이라 닉네임은 사용자가 채우지 않는다 (컨트롤러가 덮어쓴다) */
    @Test
    public void 닉네임이_비어도_통과한다() {
        GuestbookVO vo = valid();
        vo.setNickname(null);

        assertTrue(validator.validate(vo).isEmpty());
    }

    @Test
    public void 메시지가_비면_막는다() {
        GuestbookVO vo = valid();
        vo.setContent("");

        assertEquals("축하 메시지를 입력해 주세요.", firstMessage(vo));
    }

    /** 컬럼이 VARCHAR(30) 이라 여기서 안 막으면 DB 제약에 걸려 500 이 난다 */
    @Test
    public void 닉네임이_30자를_넘으면_막는다() {
        GuestbookVO vo = valid();
        vo.setNickname(repeat('예', 31));

        assertEquals("닉네임은 30자를 넘을 수 없습니다.", firstMessage(vo));
    }

    @Test
    public void 메시지가_500자를_넘으면_막는다() {
        GuestbookVO vo = valid();
        vo.setContent(repeat('축', 501));

        assertEquals("메시지는 500자를 넘을 수 없습니다.", firstMessage(vo));
    }

    @Test
    public void 응원_한마디가_100자를_넘으면_막는다() {
        GuestbookVO vo = valid();
        vo.setCheer(repeat('응', 101));

        assertEquals("응원의 한마디는 100자를 넘을 수 없습니다.", firstMessage(vo));
    }

    /** 응원 한마디는 선택 입력이다 — 비어 있어도 통과해야 한다 */
    @Test
    public void 응원_한마디는_없어도_된다() {
        GuestbookVO vo = valid();
        vo.setCheer(null);

        assertTrue(validator.validate(vo).isEmpty());
    }

    /** 경계값은 통과해야 한다 — 30자에서 막으면 정확히 30자를 쓴 사람이 못 남긴다 */
    @Test
    public void 길이_상한_그_자체는_통과한다() {
        GuestbookVO vo = valid();
        vo.setNickname(repeat('예', 30));
        vo.setContent(repeat('축', 500));
        vo.setCheer(repeat('응', 100));

        assertTrue(validator.validate(vo).isEmpty());
    }

    private static GuestbookVO valid() {
        GuestbookVO vo = new GuestbookVO();
        vo.setNickname("익명");
        vo.setContent("데뷔 3주년 축하해요!");
        vo.setCheer("앞으로도 반짝이길");
        return vo;
    }

    private static String firstMessage(GuestbookVO vo) {
        Set<ConstraintViolation<GuestbookVO>> violations = validator.validate(vo);
        assertFalse("검증에 걸려야 한다", violations.isEmpty());
        return violations.iterator().next().getMessage();
    }

    private static String repeat(char c, int times) {
        StringBuilder sb = new StringBuilder(times);
        for (int i = 0; i < times; i++) {
            sb.append(c);
        }
        return sb.toString();
    }
}
