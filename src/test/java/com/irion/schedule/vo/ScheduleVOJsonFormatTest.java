package com.irion.schedule.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperFactoryBean;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

/**
 * 같은 일정이 주소에 따라 다른 날짜 모양으로 나가지 않는지 본다.
 *
 * 목록 API 는 Map 을 내보내 VO 의 @JsonFormat 을 타지 않고 servlet-context.xml 의
 * 전역 dateFormat 을 쓴다. 둘이 어긋나면 프론트가 두 형식을 다 다뤄야 한다.
 * 한쪽만 고치는 일이 없도록 설정 파일에서 값을 직접 읽어 대조한다.
 */
public class ScheduleVOJsonFormatTest {

    private static String xmlPattern;
    private static String xmlTimeZone;

    @BeforeClass
    public static void servlet_context_에서_설정을_읽는다() throws Exception {
        String xml = readResource("spring/servlet-context.xml");

        // dateFormat 프로퍼티 안의 SimpleDateFormat 선언만 잘라낸다
        int from = xml.indexOf("<property name=\"dateFormat\">");
        assertTrue("dateFormat 설정을 찾지 못했다", from >= 0);
        String block = xml.substring(from, xml.indexOf("</property>", from));

        xmlPattern = firstGroup(block, "<constructor-arg value=\"([^\"]+)\"\\s*/>");
        xmlTimeZone = firstGroup(block, "getTimeZone\"\\s*>\\s*<constructor-arg value=\"([^\"]+)\"");

        assertNotNull("전역 날짜 형식을 읽지 못했다", xmlPattern);
        assertNotNull("전역 날짜 형식에 시간대가 지정돼 있지 않다 — JVM 기본값을 따라가면 "
                + "서버 시간대가 바뀔 때 VO 쪽과 조용히 어긋난다", xmlTimeZone);
    }

    /** 전역 설정과 VO 애너테이션이 같은 패턴이어야 한다 */
    @Test
    public void 전역_설정과_VO_애너테이션의_형식이_같다() throws Exception {
        JsonFormat annotation = ScheduleVO.class
                .getDeclaredField("startDate").getAnnotation(JsonFormat.class);

        assertNotNull("startDate 에 @JsonFormat 이 없다", annotation);
        assertEquals("servlet-context.xml 과 ScheduleVO 의 날짜 형식이 다르다",
                annotation.pattern(), xmlPattern);
        assertEquals("servlet-context.xml 과 ScheduleVO 의 시간대가 다르다",
                annotation.timezone(), xmlTimeZone);
    }

    /** 실제로 찍어보고 같은 글자가 나오는지 확인한다 */
    @Test
    public void 목록과_VO_가_같은_글자로_직렬화된다() throws Exception {
        ObjectMapper mapper = mapperFromXmlConfig();
        Date when = seoul(2026, 5, 20, 8, 0, 0);

        // 목록 API 가 내보내는 모양 — Map 이라 애너테이션을 타지 않는다
        String fromMap = mapper.writeValueAsString(Collections.singletonMap("start", when));

        // VO 를 그대로 내보내는 모양
        ScheduleVO vo = new ScheduleVO();
        vo.setStartDate(when);
        String fromVo = mapper.writeValueAsString(vo);

        assertTrue("목록 쪽 값이 예상과 다르다: " + fromMap,
                fromMap.contains("\"start\":\"2026-05-20T08:00:00\""));
        assertTrue("VO 쪽 값이 목록과 다르다: " + fromVo,
                fromVo.contains("\"startDate\":\"2026-05-20T08:00:00\""));
    }

    /** 공백으로 구분하던 옛 형식이 되살아나면 잡는다 */
    @Test
    public void 공백_구분_형식으로_되돌아가지_않는다() {
        assertFalse("날짜 사이는 'T' 로 구분한다 (옛 형식: yyyy-MM-dd HH:mm:ss): " + xmlPattern,
                xmlPattern.contains("dd HH"));
    }


    /** servlet-context.xml 에 적힌 그대로 ObjectMapper 를 만든다 */
    private static ObjectMapper mapperFromXmlConfig() {
        SimpleDateFormat format = new SimpleDateFormat(xmlPattern);
        format.setTimeZone(TimeZone.getTimeZone(xmlTimeZone));

        Jackson2ObjectMapperFactoryBean factory = new Jackson2ObjectMapperFactoryBean();
        factory.setDateFormat(format);
        factory.afterPropertiesSet();
        return factory.getObject();
    }

    private static Date seoul(int year, int month, int day, int hour, int minute, int second) {
        Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("Asia/Seoul"));
        calendar.clear();
        calendar.set(year, month - 1, day, hour, minute, second);
        return calendar.getTime();
    }

    private static String firstGroup(String text, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static String readResource(String path) {
        try (InputStream in = ScheduleVOJsonFormatTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull("설정 파일을 찾지 못했다: " + path, in);
            try (Scanner scanner = new Scanner(in, StandardCharsets.UTF_8.name())) {
                return scanner.useDelimiter("\\A").next();
            }
        } catch (Exception e) {
            throw new IllegalStateException(path + " 을 읽지 못했다", e);
        }
    }
}
