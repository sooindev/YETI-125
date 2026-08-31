package com.irion.schedule.mapper;

import com.irion.schedule.vo.ScheduleVO;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.InputStream;
import java.util.Date;

import static org.junit.Assert.*;

/** 매퍼 XML 이 만드는 SQL 문장. 서비스 테스트는 매퍼가 가짜라 못 잡아 DB 없이 문장만 본다. */
public class ScheduleSqlTest {

    private static final String NS = "com.irion.schedule.mapper.ScheduleMapper.";

    private static Configuration config;

    @BeforeClass
    public static void 매퍼_XML_을_읽는다() throws Exception {
        config = new Configuration();
        // 형식상 필요할 뿐 연결은 열지 않는다
        config.setEnvironment(new Environment(
                "test", new JdbcTransactionFactory(), new UnpooledDataSource()));
        config.getTypeAliasRegistry().registerAlias("ScheduleVO", ScheduleVO.class);

        String resource = "sql/schedule/Schedule_SQL.xml";
        try (InputStream in = ScheduleSqlTest.class.getClassLoader().getResourceAsStream(resource)) {
            assertNotNull("매퍼 XML 을 찾지 못했다: " + resource, in);
            new XMLMapperBuilder(in, config, resource, config.getSqlFragments()).parse();
        }
    }

    @Test
    public void 공개여부를_보내면_문장에_들어간다() {
        String sql = updateSql(schedule("N"));

        assertTrue("display_yn 이 SET 절에 있어야 한다: " + sql,
                sql.contains("display_yn"));
    }

    /** 조건 없이 덮어쓰면 숨겨둔 일정이 조용히 공개된다 */
    @Test
    public void 공개여부가_없으면_문장에서_빠진다() {
        String sql = updateSql(schedule(null));

        assertFalse("display_yn 을 건드리면 안 된다: " + sql,
                sql.contains("display_yn"));
    }

    @Test
    public void 빈_문자열도_없는_것으로_본다() {
        String sql = updateSql(schedule(""));

        assertFalse("빈 문자열로 덮어쓰면 CHAR(1) 에 '' 가 남는다: " + sql,
                sql.contains("display_yn"));
    }

    /** 조건이 빠져도 SET 절의 쉼표가 어긋나지 않아야 한다 */
    @Test
    public void 공개여부가_빠져도_문장이_망가지지_않는다() {
        String sql = normalize(updateSql(schedule(null)));

        assertTrue("SET 절이 있어야 한다: " + sql, sql.contains("SET "));
        assertFalse("SET 바로 뒤에 쉼표가 오면 안 된다: " + sql, sql.contains("SET ,"));
        assertFalse("WHERE 앞에 쉼표가 남으면 안 된다: " + sql, sql.contains(", WHERE"));
        assertFalse("쉼표가 겹치면 안 된다: " + sql, sql.contains(",,"));

        // 나머지 컬럼은 그대로 있어야 한다
        for (String column : new String[] {
                "title", "description", "schedule_type", "start_date",
                "end_date", "all_day_yn", "color" }) {
            assertTrue(column + " 이 빠졌다: " + sql, sql.contains(column));
        }
        assertTrue("WHERE 조건이 있어야 한다: " + sql, sql.contains("schedule_id ="));
        assertTrue("삭제된 행은 제외해야 한다: " + sql, sql.contains("del_yn = 'N'"));
    }

    /** 공개 목록 조회는 공개 여부로 걸러야 한다 */
    @Test
    public void 공개_목록은_display_yn_으로_거른다() {
        String sql = normalize(sqlOf("selectDisplayScheduleList", new ScheduleVO()));

        assertTrue("공개 일정만 나와야 한다: " + sql, sql.contains("display_yn = 'Y'"));
    }

    /**
     * 기간 조건은 상한·하한이 둘 다 살아 있어야 한다.
     *
     * `OR end_date IS NULL` 로 단발 일정을 살리면 그 일정에는 하한이 사라진다 —
     * "종료일이 없으면 무조건 통과"라 시작일을 아예 안 보게 되어, 이틀치를 물어도
     * 테이블 전체가 돌아왔다. DateRange 의 400일 상한도 같이 무의미해진다.
     */
    @Test
    public void 기간_조건에_하한이_살아_있다() {
        for (String statement : new String[] { "selectScheduleList", "selectDisplayScheduleList" }) {
            String sql = normalize(sqlOf(statement, new ScheduleVO()));

            assertTrue(statement + " 에 상한이 없다: " + sql,
                    sql.contains("start_date <="));
            assertTrue(statement + " 에 하한이 없다: " + sql,
                    sql.contains("COALESCE(end_date, start_date) >="));
            assertFalse(statement + " 의 하한이 end_date IS NULL 로 무력화된다: " + sql,
                    sql.contains("end_date IS NULL"));
        }
    }

    /** 삭제는 행을 지우지 않고 표시만 한다 */
    @Test
    public void 삭제는_소프트_삭제다() {
        String sql = normalize(sqlOf("deleteSchedule", 1L));

        assertTrue("UPDATE 여야 한다: " + sql, sql.startsWith("UPDATE"));
        assertFalse("DELETE 면 안 된다: " + sql, sql.contains("DELETE"));
        assertTrue(sql.contains("del_yn = 'Y'"));
    }


    private static String updateSql(ScheduleVO schedule) {
        return normalize(sqlOf("updateSchedule", schedule));
    }

    private static String sqlOf(String statementId, Object param) {
        MappedStatement ms = config.getMappedStatement(NS + statementId);
        BoundSql boundSql = ms.getBoundSql(param);
        return boundSql.getSql();
    }

    /** 문장 모양만 보면 되므로 공백을 한 칸으로 */
    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }

    private static ScheduleVO schedule(String displayYn) {
        ScheduleVO schedule = new ScheduleVO();
        schedule.setScheduleId(7L);
        schedule.setTitle("제목");
        schedule.setStartDate(new Date());
        schedule.setScheduleType("STREAM");
        schedule.setAllDayYn("N");
        schedule.setColor("#6366F1");
        schedule.setDisplayYn(displayYn);
        return schedule;
    }
}
